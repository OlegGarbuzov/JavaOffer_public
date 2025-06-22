package com.example.javaoffer.controllerTest;

import com.example.javaoffer.TestTempExamProgress;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.dto.TemporaryExamProgressDTO;
import com.example.javaoffer.exam.dto.ExamNextQuestionRequestDTO;
import com.example.javaoffer.exam.dto.ExamResumeResponseDTO;
import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.entity.UserScoreHistory;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.enums.TaskTopic;
import com.example.javaoffer.exam.exception.InvalidRequestIdException;
import com.example.javaoffer.exam.service.TaskService;
import com.example.javaoffer.exam.service.UserScoreHistoryService;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.enums.UserRole;
import com.example.javaoffer.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.h2.engine.Role;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.javaoffer.common.constants.UrlConstant.URL_EXAM_ROOT;
import static com.example.javaoffer.common.constants.UrlConstant.URL_NEXT_QUESTION;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_QUESTION_BLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тестовый класс для контроллера получения следующих вопросов экзамена.
 * <p>
 * Проверяет функциональность получения следующих вопросов в экзамене в различных режимах:
 * <ul>
 *   <li>Свободный режим (FREE) - без ограничений по времени и количеству ошибок</li>
 *   <li>Рейтинговый режим (RATING) - с контролем времени и лимитом ошибок</li>
 * </ul>
 * </p>
 * <p>
 * Тестирует:
 * <ul>
 *   <li>Логику адаптивной сложности вопросов</li>
 *   <li>Валидацию идентификаторов запросов</li>
 *   <li>Обработку дублированных запросов</li>
 *   <li>Прерывание экзамена по различным причинам</li>
 *   <li>Исключение ранее отвеченных вопросов</li>
 * </ul>
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
public class ExamControllerNextQuestionTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Cache<UUID, TemporaryExamProgress> cache;

	@Autowired
	private TaskService taskService;

	@Autowired
	private UserScoreHistoryService userScoreHistoryService;

	@Autowired
	private UserService userService;

	@Autowired
	private CacheManager cacheManager;

	/**
	 * Очищает кэш задач перед каждым тестом.
	 * <p>
	 * Необходимо для обеспечения изолированности тестов и предотвращения
	 * влияния данных из предыдущих тестов на текущий.
	 * </p>
	 */
	@BeforeEach
	void clearCache() {
		log.debug("Очистка кэша задач перед выполнением теста");
		org.springframework.cache.Cache cache = cacheManager.getCache("allTasksByDifficulty");
		if (cache != null) {
			cache.clear();
			log.trace("Кэш 'allTasksByDifficulty' очищен");
		} else {
			log.warn("Кэш 'allTasksByDifficulty' не найден");
		}
	}

	/**
	 * Создает и возвращает DTO запроса для получения следующего вопроса экзамена.
	 * <p>
	 * Формирует стандартный объект запроса с указанными идентификаторами
	 * экзамена и запроса для последующего использования в тестах.
	 * </p>
	 *
	 * @param examId    идентификатор экзамена
	 * @param requestId идентификатор запроса
	 * @return DTO запроса для получения следующего вопроса
	 */
	private ExamNextQuestionRequestDTO buildAndGetExamNextQuestionRequestDTO(UUID examId, UUID requestId) {
		log.trace("Создание DTO запроса следующего вопроса: examId={}, requestId={}", examId, requestId);

		ExamNextQuestionRequestDTO examRequest = new ExamNextQuestionRequestDTO();
		examRequest.setExamId(examId);
		examRequest.setRequestId(requestId);
		return examRequest;
	}

	/**
	 * Поставщик аргументов для тестирования логики получения следующего вопроса.
	 * <p>
	 * Предоставляет тестовые данные для проверки адаптивной сложности вопросов
	 * в различных режимах экзамена и сценариях прогресса пользователя.
	 * </p>
	 * <p>
	 * Тестовые сценарии:
	 * <ul>
	 *   <li>Увеличение сложности при достижении порога правильных ответов</li>
	 *   <li>Уменьшение сложности при достижении порога неправильных ответов</li>
	 *   <li>Сохранение минимальной сложности при превышении порога ошибок</li>
	 *   <li>Сохранение текущей сложности при нормальных показателях</li>
	 * </ul>
	 * </p>
	 *
	 * @return поток аргументов для тестирования логики следующего вопроса
	 */
	static Stream<Arguments> nextQuestionArgumentsProvider() {
		return Stream.of(
				// Свободный режим: увеличение сложности при достижении порога правильных ответов
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData1(ExamMode.FREE),
						TaskDifficulty.Easy3),
				// Свободный режим: уменьшение сложности при достижении порога неправильных ответов
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData2(ExamMode.FREE),
						TaskDifficulty.Easy2),
				// Свободный режим: сохранение минимальной сложности при превышении порога ошибок
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData3(ExamMode.FREE),
						TaskDifficulty.Easy1),
				// Свободный режим: сохранение текущей сложности при нормальных показателях
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData4(ExamMode.FREE),
						TaskDifficulty.Easy2),
				// Рейтинговый режим: увеличение сложности при достижении порога правильных ответов
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData8(ExamMode.RATING),
						TaskDifficulty.Easy3),
				// Рейтинговый режим: уменьшение сложности при достижении порога неправильных ответов
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData9(ExamMode.RATING),
						TaskDifficulty.Easy2),
				// Рейтинговый режим: сохранение минимальной сложности при превышении порога ошибок
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData10(ExamMode.RATING),
						TaskDifficulty.Easy1),
				// Рейтинговый режим: сохранение текущей сложности при нормальных показателях
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData4(ExamMode.RATING),
						TaskDifficulty.Easy2)
		);
	}

	/**
	 * Параметризованный тест логики получения следующего вопроса экзамена.
	 * <p>
	 * Проверяет корректность адаптивной логики изменения сложности вопросов
	 * в зависимости от прогресса пользователя в различных режимах экзамена.
	 * </p>
	 *
	 * @param url                    конечная точка для получения следующего вопроса
	 * @param cacheProgress          начальный прогресс экзамена в кэше
	 * @param expectedTaskDifficulty ожидаемая сложность следующего вопроса
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@ParameterizedTest
	@MethodSource("nextQuestionArgumentsProvider")
	void nextQuestionTest(
			String url,
			TemporaryExamProgress cacheProgress,
			TaskDifficulty expectedTaskDifficulty) throws Exception {
		log.info("Тестирование логики получения следующего вопроса с ожидаемой сложностью: {}", expectedTaskDifficulty);

		UUID testExamId = UUID.randomUUID();
		log.debug("Сохранение прогресса в кэше с examId: {}", testExamId);
		cache.put(testExamId, cacheProgress);

		log.debug("Создание запроса следующего вопроса с requestId: {}", cacheProgress.getNextQuestionRequestId());
		ExamNextQuestionRequestDTO examRequest = buildAndGetExamNextQuestionRequestDTO(
				testExamId, cacheProgress.getNextQuestionRequestId());
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса получения следующего вопроса");
		MvcResult mvcResult = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(model().attribute("examId", Matchers.notNullValue()))
				.andExpect(model().attribute("result", Matchers.notNullValue()))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_QUESTION_BLOCK))
				.andReturn();

		ExamResumeResponseDTO result = (ExamResumeResponseDTO) mvcResult.getModelAndView().getModel().get("result");
		TaskDTO task = result.getTaskDto();
		TemporaryExamProgressDTO progressResponseDTO = result.getStats();

		log.trace("Проверка полученных данных из ответа");
		assertNotNull(task, "Задача должна присутствовать в ответе");
		assertNotNull(progressResponseDTO, "Прогресс должен присутствовать в ответе");

		UUID examId = (UUID) mvcResult.getModelAndView().getModel().get("examId");
		TemporaryExamProgress existingProgress = cache.getIfPresent(examId);

		log.debug("Проверка синхронизации прогресса между кэшем и ответом");
		assertNotNull(existingProgress, "Прогресс должен существовать в кэше");
		assertEquals(existingProgress.getSuccessAnswersCountAbsolute(), progressResponseDTO.getSuccessAnswersCountAbsolute(),
				"Счетчик успешных ответов должен совпадать");
		assertEquals(existingProgress.getFailAnswersCountAbsolute(), progressResponseDTO.getFailAnswersCountAbsolute(),
				"Счетчик неуспешных ответов должен совпадать");
		assertTrue(existingProgress.getCorrectlyAnsweredQuestionsId().isEmpty(),
				"Список правильно отвеченных вопросов должен быть пуст");

		log.debug("Проверка идентификаторов запросов в кэше и ответе");
		assertEquals(existingProgress.getNextAnswerCheckRequestId(), progressResponseDTO.getRequestId(),
				"ID следующей проверки ответа должен совпадать с ID в ответе");
		assertNull(existingProgress.getNextQuestionRequestId(),
				"ID следующего вопроса должен быть null после обработки");
		assertEquals(existingProgress.getLastQuestionRequestId(), cacheProgress.getNextQuestionRequestId(),
				"ID последнего вопроса должен соответствовать обработанному запросу");

		log.debug("Проверка характеристик полученной задачи");
		assertEquals(expectedTaskDifficulty, task.getDifficulty(),
				"Сложность задачи должна соответствовать ожидаемой");
		assertNotNull(task.getId(), "ID задачи должен присутствовать");
		assertFalse(task.getAnswers().isEmpty(), "Список ответов не должен быть пустым");
		assertEquals(TaskGrade.JUNIOR, task.getGrade(), "Уровень задачи должен быть JUNIOR");
		assertNotNull(task.getQuestion(), "Текст вопроса должен присутствовать");
		assertNotNull(task.getTopic(), "Тема задачи должна присутствовать");

		log.info("Тестирование логики получения следующего вопроса завершено успешно");
	}

	/**
	 * Повторяемый тест проверки исключения ранее отвеченных вопросов.
	 * <p>
	 * Тест выполняется 10 раз для проверки стабильности алгоритма выбора вопросов.
	 * Проверяет, что система не предлагает вопросы, на которые пользователь
	 * уже правильно ответил, а также не повторяет последний заданный вопрос.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@RepeatedTest(10)
	void nextQuestionIsNotOneOfTheCorrectAnswersTest(RepetitionInfo repetitionInfo) throws Exception {
		log.info("Тестирование исключения ранее отвеченных вопросов (попытка #{}/10)", repetitionInfo.getCurrentRepetition());

		log.debug("Очистка кэша задач");
		cacheManager.getCache("allTasksByDifficulty").clear();

		List<TaskDTO> prepareCorrectlyAnsweredQuestions = taskService.getTasksByDifficulty(TaskDifficulty.Easy2).stream()
				.map(TaskDTO::copy)
				.collect(Collectors.toList());
		prepareCorrectlyAnsweredQuestions.removeFirst();
		TaskDTO lastTask = prepareCorrectlyAnsweredQuestions.removeLast();
		List<Long> prepareCorrectlyAnsweredQuestionsId = prepareCorrectlyAnsweredQuestions.stream()
				.map(TaskDTO::getId)
				.toList();

		log.debug("Подготовлен список из {} ранее отвеченных вопросов, последний вопрос ID: {}",
				prepareCorrectlyAnsweredQuestionsId.size(), lastTask.getId());

		UUID testExamId = UUID.randomUUID();
		UUID nextQuestionRequestID = UUID.randomUUID();

		TemporaryExamProgress temporaryExamProgress =
				TestTempExamProgress.testData6(prepareCorrectlyAnsweredQuestionsId, lastTask.getId(), ExamMode.FREE);
		temporaryExamProgress.setNextQuestionRequestId(nextQuestionRequestID);
		cache.put(testExamId, temporaryExamProgress);

		log.debug("Создание запроса следующего вопроса");
		ExamNextQuestionRequestDTO examRequest = buildAndGetExamNextQuestionRequestDTO(testExamId, nextQuestionRequestID);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса следующего вопроса");
		MvcResult mvcResult = mockMvc.perform(post(URL_EXAM_ROOT + URL_NEXT_QUESTION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(model().attribute("examId", Matchers.notNullValue()))
				.andExpect(model().attribute("result", Matchers.notNullValue()))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_QUESTION_BLOCK))
				.andReturn();

		ExamResumeResponseDTO result = (ExamResumeResponseDTO) mvcResult.getModelAndView().getModel().get("result");
		TaskDTO nextTask = result.getTaskDto();

		log.debug("Получен следующий вопрос с ID: {}, сложность: {}", nextTask.getId(), nextTask.getDifficulty());

		log.trace("Проверка исключения ранее отвеченных вопросов");
		assertEquals(TaskDifficulty.Easy2, nextTask.getDifficulty(),
				"Сложность следующего вопроса должна соответствовать ожидаемой");
		assertThat(prepareCorrectlyAnsweredQuestionsId).doesNotContain(nextTask.getId());
		assertNotEquals(lastTask.getId(), nextTask.getId(),
				"Следующий вопрос не должен совпадать с последним заданным");

		log.info("Тестирование исключения ранее отвеченных вопросов завершено успешно");
	}

	/**
	 * Поставщик аргументов для тестирования дублированных запросов следующего вопроса.
	 * <p>
	 * Предоставляет различные конфигурации тестовых данных для проверки обработки
	 * повторных запросов с одним и тем же requestId в разных режимах экзамена.
	 * Включает тестовые случаи для свободного и рейтингового режимов с различными
	 * комбинациями nextAnswerCheckRequestId (null и не null).
	 * </p>
	 *
	 * @return поток аргументов для параметризованного теста
	 */
	static Stream<Arguments> duplicateNextQuestionRequestWithSameRequestIdTestProvider() {
		return Stream.of(
				// Свободный режим с nextAnswerCheckRequestId
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.FREE)),

				// Свободный режим без nextAnswerCheckRequestId
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								null,
								0L,
								ExamMode.FREE)),

				// Рейтинговый режим с nextAnswerCheckRequestId
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.RATING)),

				// Рейтинговый режим без nextAnswerCheckRequestId
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								null,
								0L,
								ExamMode.RATING))
		);
	}

	/**
	 * Тестирует обработку дублированных запросов следующего вопроса с одинаковым requestId.
	 * <p>
	 * Проверяет, что при повторном запросе с уже обработанным requestId система:
	 * <ul>
	 *   <li>Возвращает тот же вопрос, что был возвращен ранее</li>
	 *   <li>Не изменяет идентификаторы запросов в кэше</li>
	 *   <li>Корректно формирует ответ с существующими данными</li>
	 * </ul>
	 * Это обеспечивает идемпотентность операции получения следующего вопроса.
	 * </p>
	 *
	 * @param url             эндпоинт для запроса следующего вопроса
	 * @param initialProgress начальный прогресс экзамена
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@ParameterizedTest
	@MethodSource("duplicateNextQuestionRequestWithSameRequestIdTestProvider")
	void duplicateNextQuestionRequestWithSameRequestIdTest(String url, TemporaryExamProgress initialProgress) throws Exception {
		log.info("Тестирование дублированных запросов следующего вопроса с режимом: {}", initialProgress.getExamMode());

		List<TaskDTO> tasksByDifficulty = taskService.getTasksByDifficulty(TaskDifficulty.Easy2);
		Long initialLastTaskId = tasksByDifficulty.getLast().getId();
		log.debug("Выбран тестовый вопрос с ID: {}", initialLastTaskId);

		UUID testExamId = UUID.randomUUID();
		initialProgress.setLastTaskId(initialLastTaskId);
		cache.put(testExamId, initialProgress);
		log.debug("Создана тестовая сессия экзамена с ID: {}", testExamId);

		ObjectMapper mapper = new ObjectMapper();
		ExamNextQuestionRequestDTO examRequest = new ExamNextQuestionRequestDTO();
		examRequest.setExamId(testExamId);
		examRequest.setRequestId(initialProgress.getLastQuestionRequestId());
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка дублированного запроса с requestId: {}", initialProgress.getLastQuestionRequestId());
		MvcResult mvcResult = mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(model().attribute("examId", Matchers.notNullValue()))
				.andExpect(model().attribute("result", Matchers.notNullValue()))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_QUESTION_BLOCK))
				.andReturn();

		ExamResumeResponseDTO result = (ExamResumeResponseDTO) mvcResult.getModelAndView().getModel().get("result");
		TemporaryExamProgressDTO responseProgress = result.getStats();
		TaskDTO responseTask = result.getTaskDto();

		TemporaryExamProgress existingProgress = cache.getIfPresent(testExamId);

		log.trace("Проверка корректности ответа на дублированный запрос");
		assertNotNull(responseProgress, "Прогресс должен присутствовать в ответе");

		UUID expectedResponseId = existingProgress.getNextAnswerCheckRequestId() == null ?
				existingProgress.getLastAnswerCheckRequestId() : existingProgress.getNextAnswerCheckRequestId();

		assertEquals(responseProgress.getRequestId(), expectedResponseId,
				"Клиенту должен вернуться корректный ID для проверки ответа");

		log.debug("Проверка неизменности идентификаторов запросов в кэше");
		assertEquals(existingProgress.getNextQuestionRequestId(), initialProgress.getNextQuestionRequestId(),
				"ID следующего вопроса не должен измениться");
		assertEquals(existingProgress.getLastQuestionRequestId(), initialProgress.getLastQuestionRequestId(),
				"ID последнего вопроса не должен измениться");
		assertEquals(existingProgress.getLastAnswerCheckRequestId(), initialProgress.getLastAnswerCheckRequestId(),
				"ID последней проверки ответа не должен измениться");
		assertEquals(existingProgress.getNextAnswerCheckRequestId(), initialProgress.getNextAnswerCheckRequestId(),
				"ID следующей проверки ответа не должен измениться");

		assertEquals(initialLastTaskId, responseTask.getId(),
				"Должен быть возвращен тот же вопрос, что и в прошлый раз");

		log.info("Тестирование дублированных запросов следующего вопроса завершено успешно");
	}

	/**
	 * Поставщик аргументов для тестирования запросов с невалидным requestId.
	 * <p>
	 * Предоставляет конфигурации для проверки обработки запросов следующего вопроса
	 * с невалидным (случайным) requestId в разных режимах экзамена.
	 * Такие запросы должны отклоняться с ошибкой InvalidRequestIdException.
	 * </p>
	 *
	 * @return поток аргументов для параметризованного теста
	 */
	static Stream<Arguments> requestNextQuestionWithNotValidRequestIdTestProvider() {
		return Stream.of(
				// Свободный режим с невалидным requestId
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
						TestTempExamProgress.testData5(
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								UUID.randomUUID(),
								0L,
								ExamMode.FREE)),
				// Рейтинговый режим с невалидным requestId
				Arguments.of(
						URL_EXAM_ROOT + URL_NEXT_QUESTION,
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
	 * Тестирует валидацию requestId при запросе следующего вопроса.
	 * <p>
	 * Проверяет, что система корректно отклоняет запросы с невалидным requestId,
	 * который не соответствует ожидаемому системой идентификатору.
	 * Такие запросы должны возвращать статус 400 Bad Request с исключением
	 * InvalidRequestIdException, что защищает от несанкционированных запросов.
	 * </p>
	 *
	 * @param url             эндпоинт для запроса следующего вопроса
	 * @param initialProgress начальный прогресс экзамена
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@ParameterizedTest
	@MethodSource("requestNextQuestionWithNotValidRequestIdTestProvider")
	void requestNextQuestionWithNotValidRequestIdTest(String url, TemporaryExamProgress initialProgress) throws Exception {
		log.info("Тестирование валидации requestId для режима: {}", initialProgress.getExamMode());

		List<TaskDTO> tasksByDifficulty = taskService.getTasksByDifficulty(TaskDifficulty.Easy2);
		Long initialLastTaskId = tasksByDifficulty.getLast().getId();
		log.debug("Выбран тестовый вопрос с ID: {}", initialLastTaskId);

		UUID testExamId = UUID.randomUUID();
		initialProgress.setLastTaskId(initialLastTaskId);
		cache.put(testExamId, initialProgress);
		log.debug("Создана тестовая сессия экзамена с ID: {}", testExamId);

		ObjectMapper mapper = new ObjectMapper();
		ExamNextQuestionRequestDTO examRequest = new ExamNextQuestionRequestDTO();
		examRequest.setExamId(testExamId);
		UUID invalidRequestId = UUID.randomUUID();
		examRequest.setRequestId(invalidRequestId);
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса с невалидным requestId: {}", invalidRequestId);
		mockMvc.perform(post(url)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()).with(user("testUser").roles(String.valueOf(Role.USER))))
				.andExpect(status().isBadRequest())
				.andExpect(result -> assertInstanceOf(InvalidRequestIdException.class, result.getResolvedException()));

		log.info("Тестирование валидации requestId завершено успешно");
	}

	/**
	 * Тестирует специальный ответ при прерывании экзамена из-за превышения лимита неверных ответов.
	 * <p>
	 * Проверяет корректность обработки ситуации, когда экзамен в рейтинговом режиме
	 * был прерван из-за превышения допустимого количества неверных ответов.
	 * Система должна возвращать специальный ответ с установленным флагом
	 * isExamTerminatedByFailAnswerCount для уведомления клиента о причине завершения.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Должен возвращать специальный ответ при прерывании экзамена из-за превышения лимита неверных ответов в рейтинговом режиме")
	void shouldReturnCorrectResponseWhenExamTerminatedByFailCountInRatingMode() throws Exception {
		log.info("Тестирование ответа при прерывании экзамена из-за превышения лимита ошибок");

		UUID examId = UUID.randomUUID();
		log.debug("Создание тестового экзамена с ID: {}", examId);

		User testUser = getTestUser();
		userService.save(testUser);
		userScoreHistoryService.save(getTestUserScoreHistory(testUser, examId));

		UUID requestId = UUID.randomUUID();

		TemporaryExamProgress progress = TestTempExamProgress.testData4(ExamMode.RATING);
		progress.setTerminatedByFailAnswerCount(true);
		progress.setNextQuestionRequestId(requestId);
		cache.put(examId, progress);
		log.debug("Создан прогресс экзамена с флагом прерывания по лимиту ошибок");

		ExamNextQuestionRequestDTO examRequest = new ExamNextQuestionRequestDTO();
		examRequest.setExamId(examId);
		examRequest.setRequestId(requestId);

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса следующего вопроса для прерванного экзамена");
		MvcResult result = mockMvc.perform(post(URL_EXAM_ROOT + URL_NEXT_QUESTION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf())
						.with(authentication(new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()))))
				.andExpect(status().isOk())
				.andReturn();

		ExamResumeResponseDTO response = (ExamResumeResponseDTO) result.getModelAndView().getModel().get("result");

		log.trace("Проверка флага прерывания экзамена по лимиту ошибок");
		assertTrue(response.isExamTerminatedByFailAnswerCount(),
				"Должен быть установлен флаг завершения экзамена по превышению лимита ошибок");

		log.info("Тестирование ответа при прерывании экзамена из-за превышения лимита ошибок завершено успешно");
	}

	/**
	 * Тестирует специальный ответ при прерывании экзамена из-за нарушений антимошенничества.
	 * <p>
	 * Проверяет корректность обработки ситуации, когда экзамен был прерван
	 * из-за выявленных нарушений системы антимошенничества.
	 * Система должна возвращать специальный ответ с:
	 * <ul>
	 *   <li>Установленным флагом isExamTerminatedByViolation</li>
	 *   <li>Пустой задачей с ID=0 и темой OTHER</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Должен возвращать специальный ответ при прерывании экзамена из-за нарушений")
	void shouldReturnTerminatedResponseWhenExamTerminatedByViolations() throws Exception {
		log.info("Тестирование ответа при прерывании экзамена из-за нарушений антимошенничества");

		UUID examId = UUID.randomUUID();
		log.debug("Создание тестового экзамена с ID: {}", examId);

		User testUser = getTestUser();
		userService.save(testUser);
		userScoreHistoryService.save(getTestUserScoreHistory(testUser, examId));

		UUID requestId = UUID.randomUUID();

		TemporaryExamProgress progress = TestTempExamProgress.testData1(ExamMode.RATING);
		progress.setTerminatedByViolations(true);
		progress.setNextQuestionRequestId(requestId);
		cache.put(examId, progress);
		log.debug("Создан прогресс экзамена с флагом прерывания из-за нарушений");

		ExamNextQuestionRequestDTO examRequest = buildAndGetExamNextQuestionRequestDTO(examId, requestId);

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса следующего вопроса для экзамена, прерванного из-за нарушений");
		MvcResult result = mockMvc.perform(post(URL_EXAM_ROOT + URL_NEXT_QUESTION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf())
						.with(authentication(new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()))))
				.andExpect(status().isOk())
				.andReturn();

		ExamResumeResponseDTO response = (ExamResumeResponseDTO) result.getModelAndView().getModel().get("result");

		log.trace("Проверка флага прерывания экзамена из-за нарушений");
		assertTrue(response.isExamTerminatedByViolation(),
				"Должен быть установлен флаг прерывания экзамена из-за нарушений");

		log.trace("Проверка возврата пустой задачи");
		assertEquals(0L, response.getTaskDto().getId(),
				"Должна возвращаться пустая задача с ID=0");
		assertEquals(TaskTopic.OTHER, response.getTaskDto().getTopic(),
				"Тема задачи должна быть OTHER для прерванного экзамена");

		log.info("Тестирование ответа при прерывании экзамена из-за нарушений завершено успешно");
	}

	/**
	 * Создает тестового пользователя для использования в тестах.
	 * <p>
	 * Создает пользователя с базовыми настройками:
	 * пароль "pass", имя "name", роль USER.
	 * </p>
	 *
	 * @return тестовый пользователь
	 */
	private User getTestUser() {
		log.trace("Создание тестового пользователя");
		return User.builder()
				.password("pass")
				.username("name")
				.userScoreHistories(new ArrayList<>())
				.role(UserRole.ROLE_USER)
				.build();
	}

	/**
	 * Создает историю результатов тестового пользователя для экзамена.
	 * <p>
	 * Создает запись в истории с нулевыми значениями всех счетчиков
	 * и баллов для указанного пользователя и экзамена.
	 * </p>
	 *
	 * @param user   пользователь, для которого создается история
	 * @param examId идентификатор экзамена
	 * @return история результатов пользователя
	 */
	private UserScoreHistory getTestUserScoreHistory(User user, UUID examId) {
		log.trace("Создание истории результатов для пользователя {} и экзамена {}", user.getUsername(), examId);
		return UserScoreHistory.builder()
				.user(user)
				.totalBasePoints(0)
				.bonusByTime(0.0)
				.successAnswersCountAbsolute(0)
				.failAnswersCountAbsolute(0)
				.score(0L)
				.examID(examId)
				.build();
	}

}
