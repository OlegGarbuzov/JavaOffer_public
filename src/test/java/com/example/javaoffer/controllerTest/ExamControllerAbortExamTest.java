package com.example.javaoffer.controllerTest;

import com.example.javaoffer.TestTempExamProgress;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.dto.ExamAbortRequestDTO;
import com.example.javaoffer.exam.dto.ExamAbortResponseDTO;
import com.example.javaoffer.exam.entity.UserScoreHistory;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.service.UserScoreHistoryService;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.enums.UserRole;
import com.example.javaoffer.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.UUID;

import static com.example.javaoffer.common.constants.UrlConstant.URL_ABORT_EXAM;
import static com.example.javaoffer.common.constants.UrlConstant.URL_EXAM_ROOT;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_EXAM_RESULT_BLOCK;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тестовый класс для контроллера прерывания экзамена.
 * <p>
 * Проверяет функциональность досрочного завершения экзамена в различных режимах:
 * <ul>
 *   <li>Свободный режим (FREE) - простое прерывание без сохранения результатов</li>
 *   <li>Рейтинговый режим (RATING) - прерывание с сохранением результатов в истории</li>
 * </ul>
 * </p>
 * <p>
 * Тестирует:
 * <ul>
 *   <li>Корректное формирование ответа при прерывании</li>
 *   <li>Очистку кэша после прерывания экзамена</li>
 *   <li>Сохранение результатов в рейтинговом режиме</li>
 *   <li>Обработку отсутствующих сессий экзамена</li>
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
public class ExamControllerAbortExamTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Cache<UUID, TemporaryExamProgress> cache;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private UserService userService;

	@Autowired
	private UserScoreHistoryService userScoreHistoryService;

	/**
	 * Тестирует успешное прерывание экзамена в свободном режиме.
	 * <p>
	 * Проверяет корректность обработки запроса на прерывание экзамена
	 * в свободном режиме, включая формирование ответа и очистку кэша.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	void abortExamTest_success() throws Exception {
		log.info("Тестирование прерывания экзамена в свободном режиме");

		UUID testExamId = UUID.randomUUID();
		TemporaryExamProgress initialProgress = TestTempExamProgress.testData7(ExamMode.FREE);
		
		log.debug("Создание тестовой сессии экзамена с ID: {}", testExamId);
		cache.put(testExamId, initialProgress);

		ObjectMapper mapper = new ObjectMapper();
		ExamAbortRequestDTO examRequest = new ExamAbortRequestDTO();
		examRequest.setExamId(testExamId);
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса на прерывание экзамена");
		MvcResult mvcResult = mockMvc.perform(post(URL_EXAM_ROOT + URL_ABORT_EXAM)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(model().attribute("examId", Matchers.notNullValue()))
				.andExpect(model().attribute("result", Matchers.notNullValue()))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_EXAM_RESULT_BLOCK))
				.andReturn();

		ExamAbortResponseDTO result = (ExamAbortResponseDTO) mvcResult.getModelAndView().getModel().get("result");

		log.trace("Проверка корректности ответа");
		assertNotNull(result, "Результат прерывания экзамена должен присутствовать");

		log.debug("Проверка счетчиков успешных и неуспешных ответов");
		assertEquals(initialProgress.getSuccessAnswersCountAbsolute(), result.getSuccessAnswersCountAbsolute(),
			"Количество успешных ответов должно совпадать");
		assertEquals(initialProgress.getFailAnswersCountAbsolute(), result.getFailAnswersCountAbsolute(),
			"Количество неуспешных ответов должно совпадать");

		log.debug("Проверка очистки кэша после прерывания");
		assertNull(cache.getIfPresent(testExamId), "Сессия экзамена должна быть удалена из кэша");
		
		log.info("Тестирование прерывания экзамена в свободном режиме завершено успешно");
	}

	/**
	 * Тестирует успешное прерывание экзамена в рейтинговом режиме.
	 * <p>
	 * Проверяет корректность обработки прерывания экзамена в рейтинговом режиме,
	 * включая сохранение результатов в историю пользователя и расчет финальных баллов.
	 * В рейтинговом режиме прерывание должно сохранить результаты для подсчета рейтинга.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	void abortExamTestRatingMode_success() throws Exception {
		log.info("Тестирование прерывания экзамена в рейтинговом режиме");
		
		UUID testExamId = UUID.randomUUID();
		log.debug("Создание тестового пользователя и истории результатов для экзамена: {}", testExamId);

		User testUser = getTestUser();
		userService.save(testUser);
		userScoreHistoryService.save(getTestUserScoreHistory(testUser, testExamId));

		TemporaryExamProgress initialProgress = TestTempExamProgress.testData7(ExamMode.RATING);
		log.debug("Создание тестовой сессии экзамена в рейтинговом режиме");
		cache.put(testExamId, initialProgress);

		ObjectMapper mapper = new ObjectMapper();
		ExamAbortRequestDTO examRequest = new ExamAbortRequestDTO();
		examRequest.setExamId(testExamId);
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса на прерывание экзамена с аутентификацией пользователя");
		MvcResult mvcResult = mockMvc.perform(post(URL_EXAM_ROOT + URL_ABORT_EXAM)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf())
						.with(authentication(new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()))))
				.andExpect(model().attribute("examId", Matchers.notNullValue()))
				.andExpect(model().attribute("result", Matchers.notNullValue()))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_EXAM_RESULT_BLOCK))
				.andReturn();

		ExamAbortResponseDTO result = (ExamAbortResponseDTO) mvcResult.getModelAndView().getModel().get("result");

		log.trace("Проверка корректности ответа");
		assertNotNull(result, "Результат прерывания экзамена должен присутствовать");

		log.debug("Проверка общих счетчиков успешных и неуспешных ответов");
		assertEquals(initialProgress.getSuccessAnswersCountAbsolute(), result.getSuccessAnswersCountAbsolute(),
			"Количество успешных ответов должно совпадать");
		assertEquals(initialProgress.getFailAnswersCountAbsolute(), result.getFailAnswersCountAbsolute(),
			"Количество неуспешных ответов должно совпадать");

		log.debug("Проверка специфичных для рейтингового режима метрик");
		assertEquals(initialProgress.getCurrentBasePoint(), result.getTotalBasePoints(),
			"Общие базовые баллы должны совпадать");
		assertNotEquals(0, result.getScore(), "Итоговый счет должен быть ненулевым");
		assertNotNull(result.getBonusByTime(), "Бонус за время должен присутствовать");
		assertNotNull(result.getTimeTakenToComplete(), "Время прохождения должно присутствовать");

		log.debug("Проверка сохранения результатов в историю пользователя");
		assertEquals(testExamId, userScoreHistoryService.findByUserId(testUser.getId()).getFirst().getExamID(),
			"История пользователя должна содержать результаты экзамена");

		log.debug("Проверка очистки кэша после прерывания");
		assertNull(cache.getIfPresent(testExamId), "Сессия экзамена должна быть удалена из кэша");
		
		log.info("Тестирование прерывания экзамена в рейтинговом режиме завершено успешно");
	}

	/**
	 * Тестирует прерывание экзамена при отсутствии прогресса в кэше.
	 * <p>
	 * Проверяет корректность обработки ситуации, когда запрашивается прерывание
	 * несуществующей сессии экзамена. Система должна вернуть статус 410 (Gone),
	 * указывающий на то, что запрашиваемый ресурс больше недоступен.
	 * </p>
	 *
	 * @throws Exception при ошибке в выполнении HTTP запроса
	 */
	@Test
	void abortExamTest_sessionNotFound() throws Exception {
		log.info("Тестирование прерывания несуществующей сессии экзамена");
		
		log.debug("Очистка всех сессий из кэша");
		cache.invalidateAll();

		UUID nonExistentExamId = UUID.randomUUID();
		log.debug("Создание запроса на прерывание несуществующего экзамена: {}", nonExistentExamId);
		
		ExamAbortRequestDTO examRequest = new ExamAbortRequestDTO();
		examRequest.setExamId(nonExistentExamId);

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(examRequest);

		log.debug("Отправка запроса на прерывание несуществующего экзамена");
		mockMvc.perform(post(URL_EXAM_ROOT + URL_ABORT_EXAM)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()))
				.andExpect(status().isGone());
		
		log.info("Тестирование прерывания несуществующей сессии экзамена завершено успешно");
	}

	/**
	 * Очищает кэш перед выполнением каждого теста.
	 * <p>
	 * Обеспечивает изоляцию тестов путем очистки кэша задач по сложности.
	 * Необходимо для предотвращения влияния результатов одного теста на другой.
	 * </p>
	 */
	@BeforeEach
	void clearCache() {
		log.debug("Очистка кэша задач по сложности перед выполнением теста");
		org.springframework.cache.Cache cache = cacheManager.getCache("allTasksByDifficulty");
		cache.clear();
	}

	/**
	 * Создает тестового пользователя для использования в тестах.
	 * <p>
	 * Генерирует пользователя с базовыми характеристиками для тестирования
	 * функциональности прерывания экзамена в рейтинговом режиме.
	 * </p>
	 *
	 * @return сконфигурированный тестовый пользователь
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
	 * Создает историю результатов пользователя для тестирования.
	 * <p>
	 * Генерирует начальную запись истории результатов с нулевыми значениями
	 * для тестирования сохранения результатов прерванного экзамена.
	 * </p>
	 *
	 * @param user пользователь, для которого создается история
	 * @param examId идентификатор экзамена
	 * @return сконфигурированная история результатов пользователя
	 */
	private UserScoreHistory getTestUserScoreHistory(User user, UUID examId) {
		log.trace("Создание истории результатов для пользователя: {} и экзамена: {}", user.getUsername(), examId);
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
