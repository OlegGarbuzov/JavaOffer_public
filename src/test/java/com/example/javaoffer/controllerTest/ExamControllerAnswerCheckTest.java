package com.example.javaoffer.controllerTest;

import com.example.javaoffer.TestTempExamProgress;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.dto.AnswerDTO;
import com.example.javaoffer.exam.dto.ExamCheckAnswerRequestDTO;
import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.dto.ValidateAnswerResponseDTO;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.exception.InvalidRequestIdException;
import com.example.javaoffer.exam.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.h2.engine.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.example.javaoffer.common.constants.UrlConstant.URL_ANSWER_CHECK;
import static com.example.javaoffer.common.constants.UrlConstant.URL_EXAM_ROOT;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для функциональности проверки ответов на вопросы экзамена.
 * <p>
 * Тестирует следующие аспекты проверки ответов:
 * <ul>
 *   <li>Проверку правильных и неправильных ответов</li>
 *   <li>Подсчет статистики ответов для разных режимов экзамена</li>
 *   <li>Валидацию идентификаторов запросов</li>
 *   <li>Обработку дубликатов запросов</li>
 *   <li>Heartbeat мониторинг в рейтинговом режиме</li>
 * </ul>
 * </p>
 * <p>
 * Проверяет корректность:
 * <ul>
 *   <li>HTTP статусов ответов и содержимого JSON</li>
 *   <li>Обновления счетчиков успешных/неуспешных ответов</li>
 *   <li>Начисления/списания баллов в рейтинговом режиме</li>
 *   <li>Управления состоянием кэша экзамена</li>
 *   <li>Валидации временных меток и идентификаторов</li>
 * </ul>
 * </p>
 * <p>
 * Использует параметризованные тесты для проверки различных сценариев
 * в свободном и рейтинговом режимах экзамена.
 * </p>
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@ExtendWith(MockitoExtension.class)
class ExamControllerAnswerCheckTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Cache<UUID, TemporaryExamProgress> cache;

	@Autowired
	private TaskService taskService;

	@Autowired
	private CacheManager cacheManager;

	/**
	 * Очищает кэш перед каждым тестом для обеспечения изоляции тестов.
	 */
	@BeforeEach
	void clearCache() {
		log.debug("Очистка кэша перед выполнением теста");
		org.springframework.cache.Cache cache = cacheManager.getCache("allTasksByDifficulty");
		cache.clear();
		log.trace("Кэш 'allTasksByDifficulty' очищен");
	}


	/**
	 * Поставщик аргументов для параметризованных тестов проверки ответов.
	 * <p>
	 * Предоставляет тестовые данные для различных сценариев:
	 * <ul>
	 *   <li>Правильные и неправильные ответы в свободном режиме</li>
	 *   <li>Правильные и неправильные ответы в рейтинговом режиме</li>
	 *   <li>Ожидаемые изменения счетчиков после каждого ответа</li>
	 * </ul>
	 * </p>
	 *
	 * @return поток аргументов для тестирования проверки ответов
	 */
	static Stream<Arguments> answerCheckArgumentsProvider() {
		return Stream.of(
				// Тест #1 для СВОБОДНОГО режима - неверный ответ
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData1(ExamMode.FREE),
						false,
						0,
						TestTempExamProgress.testData1(ExamMode.FREE).getFailAnswersCount() + 1,
						TestTempExamProgress.testData1(ExamMode.FREE).getSuccessAnswersCountAbsolute(),
						TestTempExamProgress.testData1(ExamMode.FREE).getFailAnswersCountAbsolute() + 1),
				// Тест #2 для СВОБОДНОГО режима - верный ответ
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData1(ExamMode.FREE),
						true,
						TestTempExamProgress.testData1(ExamMode.FREE).getSuccessAnswersCount() + 1,
						0,
						TestTempExamProgress.testData1(ExamMode.FREE).getSuccessAnswersCountAbsolute() + 1,
						TestTempExamProgress.testData1(ExamMode.FREE).getFailAnswersCountAbsolute()),
				// Тест #1 для РЕЙТИНГОВОГО режима - неверный ответ
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData8(ExamMode.RATING),
						false,
						0,
						TestTempExamProgress.testData8(ExamMode.RATING).getFailAnswersCount() + 1,
						TestTempExamProgress.testData8(ExamMode.RATING).getSuccessAnswersCountAbsolute(),
						TestTempExamProgress.testData8(ExamMode.RATING).getFailAnswersCountAbsolute() + 1),
				// Тест #2 для РЕЙТИНГОВОГО режима - верный ответ
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData9(ExamMode.RATING),
						true,
						TestTempExamProgress.testData9(ExamMode.RATING).getSuccessAnswersCount() + 1,
						0,
						TestTempExamProgress.testData9(ExamMode.RATING).getSuccessAnswersCountAbsolute() + 1,
						TestTempExamProgress.testData9(ExamMode.RATING).getFailAnswersCountAbsolute()));
	}

	/**
	 * Параметризованный тест проверки ответов на вопросы экзамена.
	 * <p>
	 * Тестирует полный цикл проверки ответа:
	 * <ul>
	 *   <li>Создание тестового прогресса в кэше</li>
	 *   <li>Отправку запроса на проверку ответа</li>
	 *   <li>Валидацию ответа сервера</li>
	 *   <li>Проверку обновления счетчиков и статистики</li>
	 *   <li>Корректность начисления/списания баллов в рейтинговом режиме</li>
	 * </ul>
	 * </p>
	 *
	 * @param url                                 конечная точка для проверки ответа
	 * @param cacheProgress                       начальный прогресс экзамена для кэша
	 * @param expectedCorrectlyAnswer             ожидается ли правильный ответ
	 * @param expectedSuccessAnswersCount         ожидаемое количество успешных ответов
	 * @param expectedFailAnswersCount            ожидаемое количество неуспешных ответов
	 * @param expectedSuccessAnswersCountAbsolute ожидаемое абсолютное количество успешных ответов
	 * @param expectedFailAnswersCountAbsolute    ожидаемое абсолютное количество неуспешных ответов
	 * @throws Exception при ошибках выполнения HTTP запроса или обработки данных
	 */
	@ParameterizedTest
	@MethodSource("answerCheckArgumentsProvider")
	void answerCheckTest(
			String url,
			TemporaryExamProgress cacheProgress,
			boolean expectedCorrectlyAnswer,
			int expectedSuccessAnswersCount,
			int expectedFailAnswersCount,
			int expectedSuccessAnswersCountAbsolute,
			int expectedFailAnswersCountAbsolute) throws Exception {

		log.info("Начало параметризованного теста проверки ответа. Ожидается правильный ответ: {}", expectedCorrectlyAnswer);

		log.debug("Получение случайного тестового вопроса");
		Optional<TaskDTO> testRandomTask = taskService.getTestRandomTasksByDifficulty(TaskDifficulty.Easy2);
		TaskDTO randomTask = testRandomTask.orElseThrow(() -> {
			log.error("Не удалось получить тестовый вопрос");
			return new RuntimeException("Тестовый вопрос не найден");
		});

		int baseAnswerCost = randomTask.getDifficulty().getLevel() * 10;
		int expectedCurrentBasePoint = expectedCorrectlyAnswer ? cacheProgress.getCurrentBasePoint() + baseAnswerCost
				: cacheProgress.getCurrentBasePoint() - baseAnswerCost;

		log.trace("Базовая стоимость ответа: {}, ожидаемые баллы: {}", baseAnswerCost, expectedCurrentBasePoint);

		long expectedAnswerId = getExpectedCorrectlyAnswer(randomTask, expectedCorrectlyAnswer);
		log.debug("Выбран ответ с ID: {} (ожидается {}: {})", expectedAnswerId,
				expectedCorrectlyAnswer ? "правильный" : "неправильный", expectedCorrectlyAnswer);

		UUID testExamId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		log.trace("Созданы идентификаторы: examId={}, requestId={}", testExamId, requestId);

		log.debug("Настройка прогресса в кэше");
		cacheProgress.setLastTaskId(randomTask.getId());
		cacheProgress.setNextAnswerCheckRequestId(requestId);
		cache.put(testExamId, cacheProgress);

		ExamCheckAnswerRequestDTO examRequest = buildAndGetExamCheckAnswerRequestDTO(testExamId, requestId, expectedAnswerId);

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(examRequest);
		log.trace("Создан JSON запрос: {}", json);

		log.debug("Отправка POST запроса на проверку ответа");
		MvcResult mvcResult = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(status().isOk())
				.andReturn();

		String responseJson = mvcResult.getResponse().getContentAsString();
		ValidateAnswerResponseDTO validateAnswerResponseDTO = mapper.readValue(responseJson, ValidateAnswerResponseDTO.class);

		log.trace("Получен ответ от сервера: корректность={}", validateAnswerResponseDTO.getUserChooseIsCorrect());

		log.debug("Проверка полноты данных в ответе");
		assertNotNull(validateAnswerResponseDTO.getUserChooseIsCorrect(), "Корректность ответа должна быть указана");
		assertNotNull(validateAnswerResponseDTO.getContent(), "Содержимое ответа должно присутствовать");
		assertNotNull(validateAnswerResponseDTO.getExplanation(), "Объяснение должно быть предоставлено");
		assertNotNull(validateAnswerResponseDTO.getRequestId(), "ID запроса должен быть установлен");
		assertNotNull(validateAnswerResponseDTO.getId(), "ID должен присутствовать");

		log.debug("Проверка обновления прогресса в кэше");
		TemporaryExamProgress existingProgress = cache.getIfPresent(examRequest.getExamId());
		assertNotNull(existingProgress, "Прогресс должен существовать в кэше");

		log.trace("Проверка счетчиков ответов");
		assertEquals(expectedSuccessAnswersCount, existingProgress.getSuccessAnswersCount(),
				"Счетчик успешных ответов не соответствует ожидаемому");
		assertEquals(expectedFailAnswersCount, existingProgress.getFailAnswersCount(),
				"Счетчик неуспешных ответов не соответствует ожидаемому");
		assertEquals(expectedSuccessAnswersCountAbsolute, existingProgress.getSuccessAnswersCountAbsolute(),
				"Абсолютный счетчик успешных ответов не соответствует ожидаемому");
		assertEquals(expectedFailAnswersCountAbsolute, existingProgress.getFailAnswersCountAbsolute(),
				"Абсолютный счетчик неуспешных ответов не соответствует ожидаемому");

		if (cacheProgress.getExamMode() == ExamMode.RATING) {
			log.trace("Проверка баллов для рейтингового режима");
			assertEquals(expectedCurrentBasePoint, existingProgress.getCurrentBasePoint(),
					"Количество баллов в рейтинговом режиме не соответствует ожидаемому");
		}

		log.trace("Проверка идентификаторов запросов");
		assertEquals(existingProgress.getNextQuestionRequestId(), validateAnswerResponseDTO.getRequestId(),
				"ID следующего вопроса должен совпадать с ID в ответе");
		assertNull(existingProgress.getNextAnswerCheckRequestId(),
				"ID следующей проверки ответа должен быть null после обработки");
		assertEquals(existingProgress.getLastAnswerCheckRequestId(), requestId,
				"ID последней проверки ответа должен соответствовать текущему запросу");

		log.info("Параметризованный тест проверки ответа завершен успешно");
	}

	/**
	 * Поставщик аргументов для тестирования валидации идентификаторов запросов.
	 * <p>
	 * Предоставляет тестовые данные для проверки обработки некорректных
	 * идентификаторов запросов в разных режимах экзамена.
	 * </p>
	 *
	 * @return поток аргументов для тестирования валидации запросов
	 */
	static Stream<Arguments> requestAnswerCheckWithNotValidRequestIdTestProvider() {
		return Stream.of(
				// Тест для свободного режима с невалидным requestId
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.FREE)),
				// Тест для рейтингового режима с невалидным requestId
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.RATING))
		);
	}

	/**
	 * Параметризованный тест валидации идентификаторов запросов.
	 * <p>
	 * Проверяет, что система корректно обрабатывает запросы с невалидными
	 * идентификаторами и возвращает соответствующие ошибки.
	 * </p>
	 *
	 * @param url             конечная точка для проверки ответа
	 * @param initialProgress начальный прогресс экзамена
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@ParameterizedTest
	@MethodSource("requestAnswerCheckWithNotValidRequestIdTestProvider")
	void requestAnswerCheckWithNotValidRequestIdTest(String url, TemporaryExamProgress initialProgress) throws Exception {
		log.info("Тестирование валидации идентификатора запроса");

		log.debug("Поиск тестового вопроса для инициализации кэша");
		List<TaskDTO> tasksByDifficulty = taskService.getTasksByDifficulty(TaskDifficulty.Easy2);
		Long initialLastTaskId = tasksByDifficulty.getLast().getId();
		log.trace("Найден тестовый вопрос с ID: {}", initialLastTaskId);

		UUID testExamId = UUID.randomUUID();
		log.debug("Настройка прогресса в кэше с examId: {}", testExamId);
		initialProgress.setLastTaskId(initialLastTaskId);
		cache.put(testExamId, initialProgress);

		log.debug("Создание запроса с невалидным requestId");
		ObjectMapper mapper = new ObjectMapper();
		ExamCheckAnswerRequestDTO examRequest = ExamCheckAnswerRequestDTO.builder()
				.examId(testExamId)
				.selectedAnswer(0L)
				.requestId(UUID.randomUUID()) // Используем случайный requestId (не тот, что ожидается)
				.build();

		String json = mapper.writeValueAsString(examRequest);
		log.trace("Отправка запроса с невалидным requestId");

		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()).with(user("testUser").roles(String.valueOf(Role.USER))))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					log.trace("Проверка типа исключения для невалидного requestId");
					assertInstanceOf(InvalidRequestIdException.class, result.getResolvedException(),
							"Должно быть выброшено InvalidRequestIdException для невалидного requestId");
				});

		log.info("Тестирование валидации идентификатора запроса завершено успешно");
	}

	/**
	 * Поставщик аргументов для тестирования обработки дублированных запросов.
	 * <p>
	 * Предоставляет тестовые данные для проверки корректной обработки
	 * повторных запросов с одинаковыми идентификаторами в разных режимах экзамена.
	 * </p>
	 *
	 * @return поток аргументов для тестирования дублированных запросов
	 */
	static Stream<Arguments> duplicateCheckAnswerWithSameRequestIdTestProvider() {
		return Stream.of(
				// Тест для свободного режима с установленным lastAnswerCheckRequestId
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.FREE)),
				// Тест для свободного режима с null lastAnswerCheckRequestId
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								null,
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.FREE)),
				// Тест для рейтингового режима с установленным lastAnswerCheckRequestId
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.RATING)),
				// Тест для рейтингового режима с null lastAnswerCheckRequestId
				Arguments.of(
						URL_EXAM_ROOT + URL_ANSWER_CHECK,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								null,
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.RATING))
		);
	}

	/**
	 * Параметризованный тест обработки дублированных запросов с одинаковыми идентификаторами.
	 * <p>
	 * Проверяет, что система корректно обрабатывает повторные запросы и не изменяет
	 * состояние экзамена при дублированных запросах:
	 * <ul>
	 *   <li>Не изменяет счетчики ответов</li>
	 *   <li>Не изменяет идентификаторы запросов в кэше</li>
	 *   <li>Возвращает корректный ответ без дублирования обработки</li>
	 * </ul>
	 * </p>
	 *
	 * @param url             конечная точка для проверки ответа
	 * @param initialProgress начальный прогресс экзамена
	 * @throws Exception при ошибках выполнения HTTP запроса или обработки данных
	 */
	@ParameterizedTest
	@MethodSource("duplicateCheckAnswerWithSameRequestIdTestProvider")
	void duplicateCheckAnswerWithSameRequestIdTest(String url, TemporaryExamProgress initialProgress) throws Exception {
		log.info("Тестирование обработки дублированного запроса");

		log.debug("Получение тестового вопроса для дублированного запроса");
		Optional<TaskDTO> testRandomTask = taskService.getTestRandomTasksByDifficulty(TaskDifficulty.Easy2);
		TaskDTO randomTask = testRandomTask.orElseThrow(() -> {
			log.error("Не удалось получить тестовый вопрос для дублированного запроса");
			return new RuntimeException("Тестовый вопрос не найден");
		});

		long expectedAnswerId = getExpectedCorrectlyAnswer(randomTask, true);
		log.debug("Выбран правильный ответ с ID: {} для задачи ID: {}", expectedAnswerId, randomTask.getId());

		UUID testExamId = UUID.randomUUID();
		log.debug("Настройка начального прогресса в кэше с examId: {}", testExamId);
		initialProgress.setLastTaskId(randomTask.getId());
		cache.put(testExamId, initialProgress);

		log.debug("Создание дублированного запроса с lastAnswerCheckRequestId: {}",
				initialProgress.getLastAnswerCheckRequestId());
		ObjectMapper mapper = new ObjectMapper();
		ExamCheckAnswerRequestDTO examRequest = ExamCheckAnswerRequestDTO.builder()
				.examId(testExamId)
				.requestId(initialProgress.getLastAnswerCheckRequestId())
				.selectedAnswer(expectedAnswerId)
				.build();
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка дублированного запроса");
		MvcResult mvcResult = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andReturn();

		String responseJson = mvcResult.getResponse().getContentAsString();
		ObjectMapper objectMapper = new ObjectMapper();
		ValidateAnswerResponseDTO result = objectMapper.readValue(responseJson, ValidateAnswerResponseDTO.class);
		UUID responseRequestId = result.getRequestId();

		log.trace("Получен ответ с requestId: {}", responseRequestId);
		assertNotNull(responseRequestId, "ID запроса в ответе должен присутствовать");

		TemporaryExamProgress existingProgress = cache.getIfPresent(testExamId);
		assertNotNull(existingProgress, "Прогресс должен существовать в кэше");

		UUID expectedResponseId = existingProgress.getNextQuestionRequestId() == null ?
				existingProgress.getLastQuestionRequestId() : existingProgress.getNextQuestionRequestId();

		log.trace("Проверка соответствия идентификаторов запросов");
		assertEquals(expectedResponseId, responseRequestId,
				"ID в ответе должен соответствовать ожидаемому ID следующего запроса");

		log.debug("Проверка неизменности идентификаторов в кэше");
		assertEquals(existingProgress.getNextQuestionRequestId(), initialProgress.getNextQuestionRequestId(),
				"ID следующего вопроса не должен изменяться при дублированном запросе");
		assertEquals(existingProgress.getLastQuestionRequestId(), initialProgress.getLastQuestionRequestId(),
				"ID последнего вопроса не должен изменяться при дублированном запросе");
		assertEquals(existingProgress.getLastAnswerCheckRequestId(), initialProgress.getLastAnswerCheckRequestId(),
				"ID последней проверки ответа не должен изменяться при дублированном запросе");
		assertEquals(existingProgress.getNextAnswerCheckRequestId(), initialProgress.getNextAnswerCheckRequestId(),
				"ID следующей проверки ответа не должен изменяться при дублированном запросе");

		log.debug("Проверка неизменности счетчиков при дублированном запросе");
		assertEquals(existingProgress.getSuccessAnswersCount(), initialProgress.getSuccessAnswersCount(),
				"Счетчик успешных ответов не должен изменяться");
		assertEquals(existingProgress.getFailAnswersCount(), initialProgress.getFailAnswersCount(),
				"Счетчик неуспешных ответов не должен изменяться");
		assertEquals(existingProgress.getSuccessAnswersCountAbsolute(), initialProgress.getSuccessAnswersCountAbsolute(),
				"Абсолютный счетчик успешных ответов не должен изменяться");
		assertEquals(existingProgress.getFailAnswersCountAbsolute(), initialProgress.getFailAnswersCountAbsolute(),
				"Абсолютный счетчик неуспешных ответов не должен изменяться");
		assertEquals(existingProgress.getTimeOfLastQuestion(), initialProgress.getTimeOfLastQuestion(),
				"Время последнего вопроса не должно изменяться");
		assertEquals(existingProgress.getCurrentBasePoint(), initialProgress.getCurrentBasePoint(),
				"Количество баллов не должно изменяться");

		log.trace("Проверка неизменности ID последней задачи");
		assertEquals(initialProgress.getLastTaskId(), existingProgress.getLastTaskId(),
				"ID последней задачи должен остаться неизменным");

		log.info("Тестирование обработки дублированного запроса завершено успешно");
	}

	/**
	 * Создает DTO запроса для проверки ответа на вопрос.
	 *
	 * @param examId           идентификатор экзамена
	 * @param requestId        идентификатор запроса
	 * @param selectedAnswerId идентификатор выбранного ответа
	 * @return DTO запроса проверки ответа
	 */
	private ExamCheckAnswerRequestDTO buildAndGetExamCheckAnswerRequestDTO(UUID examId, UUID requestId, Long selectedAnswerId) {
		log.trace("Создание DTO запроса проверки ответа: examId={}, requestId={}, answerId={}",
				examId, requestId, selectedAnswerId);

		return ExamCheckAnswerRequestDTO.builder()
				.examId(examId)
				.requestId(requestId)
				.selectedAnswer(selectedAnswerId)
				.build();
	}

	/**
	 * Получает ID правильного или неправильного ответа для тестирования.
	 *
	 * @param randomTask        задача с вариантами ответов
	 * @param expectedCorrectly требуется ли правильный ответ
	 * @return ID соответствующего ответа
	 */
	private long getExpectedCorrectlyAnswer(TaskDTO randomTask, boolean expectedCorrectly) {
		log.trace("Поиск {} ответа для задачи ID: {}",
				expectedCorrectly ? "правильного" : "неправильного", randomTask.getId());

		List<AnswerDTO> answers = randomTask.getAnswers();

		if (expectedCorrectly) {
			return answers.stream()
					.filter(AnswerDTO::getIsCorrect)
					.findFirst()
					.map(AnswerDTO::getId)
					.orElseThrow(() -> {
						log.error("Не найден правильный ответ для задачи ID: {}", randomTask.getId());
						return new RuntimeException("Правильный ответ не найден");
					});
		} else {
			return answers.stream()
					.filter(answer -> !answer.getIsCorrect())
					.findFirst()
					.map(AnswerDTO::getId)
					.orElseThrow(() -> {
						log.error("Не найден неправильный ответ для задачи ID: {}", randomTask.getId());
						return new RuntimeException("Неправильный ответ не найден");
					});
		}
	}

	/**
	 * Тестирует увеличение счетчика пропущенных heartbeat при долгом отсутствии в рейтинговом режиме.
	 * <p>
	 * Проверяет, что система корректно отслеживает пропуски heartbeat в соревновательном режиме
	 * и увеличивает соответствующий счетчик при превышении временных лимитов.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Должен увеличивать счетчик пропущенных heartbeat при долгом отсутствии в соревновательном режиме")
	void shouldIncrementHeartbeatMissedCountWhenLongAbsenceInRatingMode() throws Exception {
		log.info("Тестирование увеличения счетчика пропущенных heartbeat в рейтинговом режиме");

		UUID examId = UUID.randomUUID();
		log.debug("Создание прогресса в рейтинговом режиме с устаревшим heartbeat для examId: {}", examId);
		TemporaryExamProgress progress = TestTempExamProgress.testData8(ExamMode.RATING);
		progress.setLastHeartbeatTime(Instant.now().minus(Duration.ofMinutes(2))); // 2 минуты назад
		progress.setNextExpectedHeartbeatTime(Instant.now().minus(Duration.ofMinutes(1))); // 1 минута назад
		progress.setHeartbeatMissedCount(0);

		Long taskId = 1L;
		UUID requestId = UUID.randomUUID();
		log.trace("Настройка прогресса: taskId={}, requestId={}, начальный heartbeatMissedCount=0", taskId, requestId);
		progress.setLastTaskId(taskId);
		progress.setNextAnswerCheckRequestId(requestId);
		cache.put(examId, progress);

		ExamCheckAnswerRequestDTO examRequest = buildAndGetExamCheckAnswerRequestDTO(examId, requestId, 1L);

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса на проверку ответа для тестирования heartbeat");
		mockMvc.perform(post(URL_EXAM_ROOT + URL_ANSWER_CHECK)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(status().isOk())
				.andReturn();

		log.debug("Проверка увеличения счетчика пропущенных heartbeat");
		TemporaryExamProgress updatedProgress = cache.getIfPresent(examId);
		assertNotNull(updatedProgress, "Обновленный прогресс должен существовать в кэше");
		assertEquals(1, updatedProgress.getHeartbeatMissedCount(),
				"Счетчик пропущенных heartbeat должен увеличиться на 1");

		log.info("Тестирование увеличения счетчика пропущенных heartbeat завершено успешно");
	}

	/**
	 * Тестирует отсутствие увеличения счетчика пропущенных heartbeat в свободном режиме.
	 * <p>
	 * Проверяет, что система не отслеживает пропуски heartbeat в свободном режиме
	 * и не увеличивает счетчик при долгом отсутствии активности пользователя.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Не должен увеличивать счетчик пропущенных heartbeat при долгом отсутствии в свободном режиме")
	void shouldNotIncrementHeartbeatMissedCountInFreeMode() throws Exception {
		log.info("Тестирование отсутствия увеличения счетчика пропущенных heartbeat в свободном режиме");

		UUID examId = UUID.randomUUID();
		log.debug("Создание прогресса в свободном режиме с устаревшим heartbeat для examId: {}", examId);
		TemporaryExamProgress progress = TestTempExamProgress.testData1(ExamMode.FREE);
		progress.setLastHeartbeatTime(Instant.now().minus(Duration.ofMinutes(2))); // 2 минуты назад
		progress.setNextExpectedHeartbeatTime(Instant.now().minus(Duration.ofMinutes(1))); // 1 минута назад
		int initialMissedCount = progress.getHeartbeatMissedCount();
		log.trace("Начальный счетчик пропущенных heartbeat: {}", initialMissedCount);

		Long taskId = 1L;
		UUID requestId = UUID.randomUUID();
		log.trace("Настройка прогресса: taskId={}, requestId={}", taskId, requestId);
		progress.setLastTaskId(taskId);
		progress.setNextAnswerCheckRequestId(requestId);
		cache.put(examId, progress);

		ExamCheckAnswerRequestDTO examRequest = buildAndGetExamCheckAnswerRequestDTO(examId, requestId, 1L);

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса на проверку ответа в свободном режиме");
		mockMvc.perform(post(URL_EXAM_ROOT + URL_ANSWER_CHECK)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(status().isOk())
				.andReturn();

		log.debug("Проверка неизменности счетчика пропущенных heartbeat");
		TemporaryExamProgress updatedProgress = cache.getIfPresent(examId);
		assertNotNull(updatedProgress, "Обновленный прогресс должен существовать в кэше");
		assertEquals(initialMissedCount, updatedProgress.getHeartbeatMissedCount(),
				"Счетчик пропущенных heartbeat не должен изменяться в свободном режиме");

		log.info("Тестирование отсутствия увеличения счетчика пропущенных heartbeat завершено успешно");
	}

}
