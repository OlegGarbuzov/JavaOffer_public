package com.example.javaoffer.controllerTest;

import com.example.javaoffer.exam.anticheat.config.AntiCheatProperties;
import com.example.javaoffer.exam.anticheat.dto.SessionIntegrityResponseDTO;
import com.example.javaoffer.exam.anticheat.dto.SessionStatusResponseDTO;
import com.example.javaoffer.exam.anticheat.enums.EventType;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.example.javaoffer.exam.enums.ExamMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static TestConstants.TestConstant.URL_ANTICHEAT_STATUS;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тестовый класс для контроллера системы защиты от мошенничества.
 * <p>
 * Проверяет функциональность системы антимошенничества, включая:
 * <ul>
 *   <li>Обработку различных типов нарушений (переключение вкладок, копирование текста, вмешательство в DOM и т.д.)</li>
 *   <li>Различие в обработке нарушений для рейтингового и свободного режимов</li>
 *   <li>Подсчет нарушений и автоматическое прерывание экзамена при превышении лимитов</li>
 *   <li>Обработку heartbeat сигналов для контроля активности пользователя</li>
 *   <li>Валидацию входных данных и обработку ошибочных запросов</li>
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
public class AntiCheatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ExamSessionCacheService examSessionCacheService;

	@Autowired
	private AntiCheatProperties antiCheatProperties;

	private UUID examId;
	private final Long questionId = 1L;

	/**
	 * Настраивает тестовую среду перед каждым тестом.
	 * <p>
	 * Создает новую сессию экзамена в рейтинговом режиме с начальными
	 * значениями всех счетчиков нарушений и сохраняет её в кэше.
	 * </p>
	 */
	@BeforeEach
	void setUp() {
		examId = UUID.randomUUID();
		log.debug("Создание тестовой сессии экзамена с ID: {}", examId);

		TemporaryExamProgress progress = TemporaryExamProgress.builder()
				.examMode(ExamMode.RATING)
				.lastTaskId(questionId)
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.devToolsViolationCount(0)
				.domTamperingViolationCount(0)
				.functionTamperingViolationCount(0)
				.antiOcrTamperingViolationCount(0)
				.moduleTamperingViolationCount(0)
				.pageCloseViolationCount(0)
				.externalContentViolationCount(0)
				.terminatedByViolations(false)
				.lastHeartbeatTime(Instant.now())
				.nextExpectedHeartbeatTime(Instant.now().plusMillis(5000))
				.progressCreateAt(LocalDateTime.now())
				.correctlyAnsweredQuestionsId(new CopyOnWriteArrayList<>())
				.userAnswers(new CopyOnWriteArrayList<>())
				.build();

		examSessionCacheService.save(examId, progress);
		log.trace("Тестовая сессия экзамена сохранена в кэше");
	}

	/**
	 * Создает JSON представление события нарушения с использованием ID текущего экзамена.
	 * <p>
	 * Упрощенная версия метода для создания JSON с предустановленным questionId.
	 * </p>
	 *
	 * @param examId идентификатор экзамена
	 * @param eventTypeJsonName JSON имя типа события
	 * @return JSON строка с данными события
	 */
	private String createViolationEventJson(UUID examId, String eventTypeJsonName) {
		return createViolationEventJson(examId, 1L, eventTypeJsonName);
	}

	/**
	 * Создает JSON представление события нарушения с полными параметрами.
	 * <p>
	 * Формирует JSON объект с данными события для отправки в контроллер антимошенничества.
	 * </p>
	 *
	 * @param examId идентификатор экзамена
	 * @param questionId идентификатор вопроса
	 * @param eventTypeJsonName JSON имя типа события
	 * @return JSON строка с данными события
	 */
	private String createViolationEventJson(UUID examId, Long questionId, String eventTypeJsonName) {
		log.trace("Создание JSON события нарушения: examId={}, questionId={}, eventType={}", 
			examId, questionId, eventTypeJsonName);
		return "{\n" +
				"    \"examId\": \"" + (examId != null ? examId : "") + "\",\n" +
				"    \"questionId\": " + questionId + ",\n" +
				"    \"eventType\": \"" + eventTypeJsonName + "\"\n" +
				"}";
	}

	@Nested
	@DisplayName("Тесты унифицированного API для отправки событий античита")
	class UnifiedAntiCheatTests {

		/**
		 * Тестирует игнорирование нарушений в не рейтинговом режиме.
		 * <p>
		 * В свободном режиме экзамена система антимошенничества должна игнорировать
		 * все нарушения и не изменять соответствующие счетчики. Это позволяет
		 * пользователям свободно использовать браузер без ограничений.
		 * </p>
		 *
		 * @throws Exception при ошибках выполнения HTTP запроса
		 */
		@Test
		@DisplayName("Должен игнорировать нарушения в не рейтинговом режиме")
		@WithMockUser(username = "testUser")
		void shouldIgnoreViolationsInNonRatingMode() throws Exception {
			log.info("Тестирование игнорирования нарушений в свободном режиме");
			
			UUID nonRatingExamId = UUID.randomUUID();
			log.debug("Создание экзамена в свободном режиме с ID: {}", nonRatingExamId);
			
			TemporaryExamProgress nonRatingProgress = TemporaryExamProgress.builder()
					.examMode(ExamMode.FREE)
					.lastTaskId(questionId)
					.tabSwitchViolationCount(0)
					.progressCreateAt(LocalDateTime.now())
					.correctlyAnsweredQuestionsId(new CopyOnWriteArrayList<>())
					.userAnswers(new CopyOnWriteArrayList<>())
					.build();
			examSessionCacheService.save(nonRatingExamId, nonRatingProgress);

			String jsonContent = createViolationEventJson(nonRatingExamId, getJsonNameForEventType(EventType.TAB_SWITCH));
			log.debug("Отправка события переключения вкладки для свободного режима");

			MvcResult result = mockMvc.perform(post(URL_ANTICHEAT_STATUS)
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf()))
					.andExpect(status().isOk())
					.andReturn();

			SessionIntegrityResponseDTO response = objectMapper.readValue(
					result.getResponse().getContentAsString(), SessionIntegrityResponseDTO.class);

			log.trace("Проверка игнорирования нарушения в свободном режиме");
			assertFalse(response.isExamTerminatedByViolation(), 
				"Экзамен не должен быть прерван в свободном режиме");

			Optional<TemporaryExamProgress> updatedProgress = examSessionCacheService.get(nonRatingExamId);
			assertTrue(updatedProgress.isPresent(), "Прогресс экзамена должен существовать");
			assertEquals(0, updatedProgress.get().getTabSwitchViolationCount(),
				"Счетчик переключений вкладок не должен изменяться в свободном режиме");
			
			log.info("Тестирование игнорирования нарушений в свободном режиме завершено успешно");
		}

		/**
		 * Тестирует накопление нарушений вмешательства в работу системы.
		 * <p>
		 * Проверяет, что различные типы нарушений вмешательства (DevTools, DOM tampering)
		 * корректно накапливаются и при достижении лимита приводят к прерыванию экзамена.
		 * </p>
		 *
		 * @throws Exception при ошибках выполнения HTTP запроса
		 */
		@Test
		@DisplayName("Должен суммировать нарушения вмешательства в работу системы")
		@WithMockUser(username = "testUser")
		void shouldCumulateTamperingViolations() throws Exception {
			log.info("Тестирование накопления нарушений вмешательства в систему");
			
			String devToolsJson = createViolationEventJson(examId, getJsonNameForEventType(EventType.DEVTOOLS));
			String domTamperingJson = createViolationEventJson(examId, getJsonNameForEventType(EventType.DOM_TAMPERING));

			log.debug("Отправка нарушений: открытие DevTools и вмешательство в DOM");
			mockMvc.perform(post(URL_ANTICHEAT_STATUS)
							.contentType(MediaType.APPLICATION_JSON)
							.content(devToolsJson)
							.with(csrf())
							.with(user("testUser").roles("USER")))
					.andExpect(status().isOk());

			mockMvc.perform(post(URL_ANTICHEAT_STATUS)
							.contentType(MediaType.APPLICATION_JSON)
							.content(domTamperingJson)
							.with(csrf())
							.with(user("testUser").roles("USER")))
					.andExpect(status().isOk());

			log.trace("Проверка накопления нарушений вмешательства");
			Optional<TemporaryExamProgress> updatedProgress = examSessionCacheService.get(examId);
			assertTrue(updatedProgress.isPresent(), "Прогресс экзамена должен существовать");
			assertEquals(1, updatedProgress.get().getDevToolsViolationCount(),
				"Счетчик нарушений DevTools должен быть равен 1");
			assertEquals(1, updatedProgress.get().getDomTamperingViolationCount(),
				"Счетчик нарушений DOM должен быть равен 1");

			int totalTamperingViolations =
					updatedProgress.get().getDevToolsViolationCount() +
							updatedProgress.get().getDomTamperingViolationCount() +
							updatedProgress.get().getFunctionTamperingViolationCount() +
							updatedProgress.get().getModuleTamperingViolationCount() +
							updatedProgress.get().getPageCloseViolationCount() +
							updatedProgress.get().getExternalContentViolationCount();

			log.debug("Общее количество нарушений вмешательства: {}, лимит: {}", 
				totalTamperingViolations, antiCheatProperties.getMaxTamperingViolations());
			assertEquals(2, totalTamperingViolations, "Общее количество нарушений должно быть 2");

			if (totalTamperingViolations >= antiCheatProperties.getMaxTamperingViolations()) {
				assertTrue(updatedProgress.get().isTerminatedByViolations(),
					"Экзамен должен быть прерван при превышении лимита вмешательств");
			} else {
				assertFalse(updatedProgress.get().isTerminatedByViolations(),
					"Экзамен не должен быть прерван до достижения лимита вмешательств");
			}
			
			log.info("Тестирование накопления нарушений вмешательства в систему завершено успешно");
		}

		/**
		 * Тестирует обработку всех типов нарушений системы антимошенничества.
		 * <p>
		 * Перебирает все доступные типы событий нарушений и проверяет,
		 * что каждый тип корректно обрабатывается системой и увеличивает
		 * соответствующий счетчик нарушений. Исключает HEART_BEAT, так как
		 * для него используется другой формат запроса и ответа.
		 * </p>
		 *
		 * @throws Exception при ошибках выполнения HTTP запроса
		 */
		@Test
		@DisplayName("Должен обрабатывать все типы нарушений")
		@WithMockUser(username = "testUser")
		void shouldHandleAllViolationTypes() throws Exception {
			log.info("Тестирование обработки всех типов нарушений");
			
			for (EventType eventType : EventType.values()) {
				if (eventType == EventType.HEART_BEAT) {
					log.debug("Пропуск HEART_BEAT события (отдельный формат запроса)");
					continue;
				}

				log.debug("Тестирование обработки события типа: {}", eventType);
				setUp();

				String jsonContent = createViolationEventJson(examId, getJsonNameForEventType(eventType));

				MvcResult result = mockMvc.perform(post(URL_ANTICHEAT_STATUS)
								.contentType(MediaType.APPLICATION_JSON)
								.content(jsonContent)
								.with(csrf())
								.with(user("testUser").roles("USER")))
						.andExpect(status().isOk())
						.andReturn();

				SessionIntegrityResponseDTO response = objectMapper.readValue(
						result.getResponse().getContentAsString(), SessionIntegrityResponseDTO.class);

				log.trace("Проверка ответа для события типа: {}", eventType);
				assertFalse(response.isExamTerminatedByViolation(),
					"Экзамен не должен быть прерван единичным нарушением типа " + eventType);

				Optional<TemporaryExamProgress> updatedProgress = examSessionCacheService.get(examId);
				assertTrue(updatedProgress.isPresent(), "Прогресс экзамена должен существовать");

				log.trace("Проверка увеличения счетчика для события типа: {}", eventType);
				switch (eventType) {
					case TAB_SWITCH -> assertEquals(1, updatedProgress.get().getTabSwitchViolationCount(),
						"Счетчик переключения вкладок должен увеличиться");
					case TEXT_COPY -> assertEquals(1, updatedProgress.get().getTextCopyViolationCount(),
						"Счетчик копирования текста должен увеличиться");
					case DEVTOOLS -> assertEquals(1, updatedProgress.get().getDevToolsViolationCount(),
						"Счетчик открытия DevTools должен увеличиться");
					case DOM_TAMPERING -> assertEquals(1, updatedProgress.get().getDomTamperingViolationCount(),
						"Счетчик вмешательства в DOM должен увеличиться");
					case FUNCTION_TAMPERING -> assertEquals(1, updatedProgress.get().getFunctionTamperingViolationCount(),
						"Счетчик вмешательства в функции должен увеличиться");
					case MODULE_TAMPERING -> assertEquals(1, updatedProgress.get().getModuleTamperingViolationCount(),
						"Счетчик вмешательства в модули должен увеличиться");
					case PAGE_CLOSE -> assertEquals(1, updatedProgress.get().getPageCloseViolationCount(),
						"Счетчик закрытия страницы должен увеличиться");
					case EXTERNAL_CONTENT -> assertEquals(1, updatedProgress.get().getExternalContentViolationCount(),
						"Счетчик внешнего контента должен увеличиться");
					case ANTI_OCR_TAMP -> assertEquals(1, updatedProgress.get().getAntiOcrTamperingViolationCount(),
						"Счетчик вмешательства в анти-OCR должен увеличиться");
				}
			}
			
			log.info("Тестирование обработки всех типов нарушений завершено успешно");
		}

		/**
		 * Тестирует валидацию входных данных и возврат 400 Bad Request.
		 * <p>
		 * Проверяет, что система корректно отклоняет запросы с невалидными данными,
		 * включая null значения для обязательных полей examId и eventType.
		 * Система должна вернуть статус 400 Bad Request при нарушении требований валидации.
		 * </p>
		 *
		 * @throws Exception при ошибках выполнения HTTP запроса
		 */
		@Test
		@DisplayName("Должен вернуть 400 Bad Request при невалидных данных")
		@WithMockUser(username = "testUser")
		void shouldReturn400WhenInvalidData() throws Exception {
			log.info("Тестирование валидации входных данных");
			
			log.debug("Тестирование валидации с null examId");
			String invalidJson = "{\n" +
					"    \"examId\": null,\n" +
					"    \"eventType\": \"" + getJsonNameForEventType(EventType.TAB_SWITCH) + "\"\n" +
					"}";

			mockMvc.perform(post(URL_ANTICHEAT_STATUS)
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidJson)
							.with(csrf()))
					.andExpect(status().isBadRequest());
			
			log.debug("Валидация null examId прошла успешно");

			log.debug("Тестирование валидации с null eventType");
			invalidJson = "{\n" +
					"    \"examId\": \"" + examId + "\",\n" +
					"    \"eventType\": null\n" +
					"}";

			mockMvc.perform(post(URL_ANTICHEAT_STATUS)
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidJson)
							.with(csrf()))
					.andExpect(status().isBadRequest());
			
			log.debug("Валидация null eventType прошла успешно");
			log.info("Тестирование валидации входных данных завершено успешно");
		}

		/**
		 * Тестирует обработку запроса для несуществующего экзамена.
		 * <p>
		 * Проверяет, что система корректно обрабатывает ситуацию, когда поступает
		 * запрос на обработку нарушения для несуществующей сессии экзамена.
		 * Система должна вернуть статус 410 Gone, указывающий на то, что
		 * запрашиваемый ресурс больше недоступен.
		 * </p>
		 *
		 * @throws Exception при ошибках выполнения HTTP запроса
		 */
		@Test
		@DisplayName("Должен вернуть 410 Gone при обработке нарушения для несуществующего экзамена")
		@WithMockUser(username = "testUser")
		void shouldReturn410WhenExamNotExists() throws Exception {
			log.info("Тестирование обработки нарушения для несуществующего экзамена");
			
			UUID nonExistentExamId = UUID.randomUUID();
			log.debug("Создание запроса для несуществующего экзамена: {}", nonExistentExamId);
			String jsonContent = createViolationEventJson(nonExistentExamId, getJsonNameForEventType(EventType.TAB_SWITCH));

			log.debug("Отправка запроса для несуществующего экзамена");
			MvcResult result = mockMvc.perform(post(URL_ANTICHEAT_STATUS)
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf()))
					.andExpect(status().isGone())
					.andReturn();

			log.trace("Проверка содержимого ответа");
			assertTrue(result.getResponse().getContentAsString().contains("не найдена"),
				"Ответ должен содержать сообщение о том, что сессия не найдена");
			
			log.info("Тестирование обработки нарушения для несуществующего экзамена завершено успешно");
		}

		/**
		 * Тестирует обработку heartbeat сигналов и увеличение счетчика при повторных запросах.
		 * <p>
		 * Проверяет корректность обработки heartbeat запросов с инициализационными токенами.
		 * При первом запросе система должна обновить токен сессии, при повторном запросе
		 * с тем же init токеном должен увеличиться счетчик пропущенных heartbeat.
		 * Это помогает отслеживать потенциальные попытки обхода системы мониторинга.
		 * </p>
		 *
		 * @throws Exception при ошибках выполнения HTTP запроса
		 */
		@Test
		@DisplayName("Должен корректно обрабатывать начальный heartbeat и увеличить счетчик при повторе")
		@WithMockUser(username = "testUser")
		void shouldHandleHeartbeatAndIncrementCounterOnRepeatedInitToken() throws Exception {
			log.info("Тестирование обработки heartbeat сигналов с инициализационными токенами");
			
			String initToken = "init_" + UUID.randomUUID();
			log.debug("Создание инициализационного токена: {}", initToken);

			String jsonContent = "{\n" +
					"    \"examId\": \"" + examId + "\",\n" +
					"    \"questionId\": " + questionId + ",\n" +
					"    \"token\": \"" + initToken + "\",\n" +
					"    \"timezoneOffset\": 0,\n" +
					"    \"timezoneString\": \"UTC\",\n" +
					"    \"clientTime\": " + System.currentTimeMillis() + ",\n" +
					"    \"eventType\": \"" + getJsonNameForEventType(EventType.HEART_BEAT) + "\"\n" +
					"}";

			log.debug("Отправка первичного heartbeat запроса");
			MvcResult firstResult = mockMvc.perform(post(URL_ANTICHEAT_STATUS)
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf()))
					.andExpect(status().isOk())
					.andReturn();

			SessionStatusResponseDTO firstResponse = objectMapper.readValue(
					firstResult.getResponse().getContentAsString(), SessionStatusResponseDTO.class);

			log.trace("Проверка первичного ответа heartbeat");
			assertNotNull(firstResponse.getNextToken(), "Новый токен должен быть сгенерирован");
			assertFalse(firstResponse.isExamTerminatedByViolation(), 
				"Экзамен не должен быть прерван heartbeat запросом");
			assertNotNull(firstResponse.getChallenge(), "Challenge должен присутствовать в ответе");

			Optional<TemporaryExamProgress> updatedProgress = examSessionCacheService.get(examId);
			assertTrue(updatedProgress.isPresent(), "Прогресс экзамена должен существовать");
			assertEquals(firstResponse.getNextToken(), updatedProgress.get().getLastSessionToken(),
				"Токен сессии должен обновиться в кэше");

			int initialMissedCount = updatedProgress.get().getHeartbeatMissedCount();
			log.debug("Начальное количество пропущенных heartbeat: {}", initialMissedCount);

			log.debug("Отправка повторного heartbeat запроса с тем же init токеном");
			MvcResult repeatedResult = mockMvc.perform(post(URL_ANTICHEAT_STATUS)
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf()))
					.andExpect(status().isOk())
					.andReturn();

			SessionStatusResponseDTO repeatedResponse = objectMapper.readValue(
					repeatedResult.getResponse().getContentAsString(), SessionStatusResponseDTO.class);

			log.trace("Проверка повторного ответа heartbeat");
			assertNotNull(repeatedResponse.getNextToken(), "Новый токен должен быть сгенерирован");
			assertFalse(repeatedResponse.isExamTerminatedByViolation(),
				"Экзамен не должен быть прерван повторным heartbeat запросом");
			assertNotNull(repeatedResponse.getChallenge(), "Challenge должен присутствовать в ответе");

			Optional<TemporaryExamProgress> finalProgress = examSessionCacheService.get(examId);
			assertTrue(finalProgress.isPresent(), "Прогресс экзамена должен существовать");
			assertEquals(initialMissedCount + 1, finalProgress.get().getHeartbeatMissedCount(),
				"Счетчик пропущенных heartbeat должен увеличиться на 1 при повторном init токене");
			
			log.info("Тестирование обработки heartbeat сигналов завершено успешно");
		}
	}

	/**
	 * Возвращает JSON-имя для типа события антимошенничества.
	 * <p>
	 * Преобразует тип события в строковое представление, используемое
	 * в JSON запросах к API системы антимошенничества.
	 * </p>
	 *
	 * @param eventType тип события антимошенничества
	 * @return JSON имя события
	 */
	private String getJsonNameForEventType(EventType eventType) {
		log.trace("Получение JSON имени для типа события: {}", eventType);
		return eventType.getJsonName();
	}
}