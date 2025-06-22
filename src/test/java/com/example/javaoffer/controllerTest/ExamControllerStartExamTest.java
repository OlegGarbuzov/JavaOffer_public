package com.example.javaoffer.controllerTest;

import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.dto.TemporaryExamProgressDTO;
import com.example.javaoffer.exam.dto.ExamResumeResponseDTO;
import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.exception.AuthenticatedValidationException;
import com.example.javaoffer.exam.property.FreeModeProperties;
import com.example.javaoffer.exam.property.RatingModeProperties;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.example.javaoffer.common.constants.UrlConstant.URL_EXAM_ROOT;
import static com.example.javaoffer.common.constants.UrlConstant.URL_START_EXAM;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_EXAM;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для функционала запуска экзамена в ExamController.
 * <p>
 * Тестирует следующие сценарии:
 * <ul>
 *   <li>Успешный запуск экзамена в свободном режиме</li>
 *   <li>Проверку аутентификации при запуске рейтингового экзамена</li>
 *   <li>Корректность создания и кэширования прогресса экзамена</li>
 *   <li>Валидацию начальных параметров экзамена</li>
 * </ul>
 * </p>
 * <p>
 * Проверяет корректность:
 * <ul>
 *   <li>HTTP статусов ответов</li>
 *   <li>Содержимого модели представления</li>
 *   <li>Создания записей в кэше</li>
 *   <li>Начальных значений прогресса экзамена</li>
 *   <li>Параметров первого вопроса</li>
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
class ExamControllerStartExamTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FreeModeProperties freeModeProperties;

	@Autowired
	private RatingModeProperties ratingModeProperties;

	@Autowired
	private Cache<UUID, TemporaryExamProgress> cache;

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
	 * Тестирует успешный запуск экзамена в свободном режиме.
	 * <p>
	 * Проверяет весь цикл запуска экзамена:
	 * <ul>
	 *   <li>Отправку POST запроса с параметрами экзамена</li>
	 *   <li>Корректность возвращаемого представления и модели</li>
	 *   <li>Создание записи в кэше с правильными начальными значениями</li>
	 *   <li>Получение первого вопроса с корректными параметрами</li>
	 *   <li>Инициализацию счетчиков успешных/неуспешных ответов</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса или проверки результатов
	 */
	@Test
	void startExamTest() throws Exception {
		log.info("Начало тестирования запуска экзамена в свободном режиме");

		log.debug("Отправка POST запроса для запуска экзамена");
		MvcResult mvcResult = mockMvc.perform(post(URL_EXAM_ROOT + URL_START_EXAM)
						.param("examMode", freeModeProperties.getMode().toString())
						.with(csrf()))
				.andExpect(model().attribute("request", Matchers.notNullValue()))
				.andExpect(model().attribute("examId", Matchers.notNullValue()))
				.andExpect(model().attribute("result", Matchers.notNullValue()))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_EXAM))
				.andReturn();

		log.debug("Извлечение данных из результата запроса");
		ExamResumeResponseDTO result = (ExamResumeResponseDTO) mvcResult.getModelAndView().getModel().get("result");
		TaskDTO task = result.getTaskDto();
		TemporaryExamProgressDTO progress = result.getStats();

		log.trace("Проверка наличия обязательных данных в ответе");
		assertNotNull(task, "Задача должна присутствовать в ответе");
		assertNotNull(progress, "Прогресс экзамена должен присутствовать в ответе");

		UUID examId = (UUID) mvcResult.getModelAndView().getModel().get("examId");
		log.debug("Проверка создания записи в кэше для examId: {}", examId);
		TemporaryExamProgress existing = cache.getIfPresent(examId);
		assertNotNull(existing, "Прогресс экзамена должен быть сохранен в кэше");

		log.trace("Проверка начальных значений прогресса экзамена");
		assertEquals(TaskDifficulty.Easy1, progress.getCurrentDifficulty(),
				"Начальный уровень сложности должен быть Easy1");
		assertEquals(existing.getNextAnswerCheckRequestId(), progress.getRequestId(),
				"ID запроса должен соответствовать ID проверки ответа");
		assertNotNull(existing.getLastQuestionRequestId(),
				"ID последнего запроса вопроса должен быть установлен");
		assertNull(existing.getNextQuestionRequestId(),
				"ID следующего запроса вопроса должен быть null при запуске");

		log.trace("Проверка счетчиков ответов");
		assertTrue(existing.getCorrectlyAnsweredQuestionsId().isEmpty(),
				"Список правильных ответов должен быть пустым");
		assertEquals(0, existing.getSuccessAnswersCount(), "Счетчик успешных ответов должен быть 0");
		assertEquals(0, existing.getFailAnswersCount(), "Счетчик неуспешных ответов должен быть 0");
		assertEquals(0, existing.getSuccessAnswersCountAbsolute(), "Абсолютный счетчик успешных ответов должен быть 0");
		assertEquals(0, existing.getFailAnswersCountAbsolute(), "Абсолютный счетчик неуспешных ответов должен быть 0");
		assertEquals(0, progress.getSuccessAnswersCountAbsolute(), "Успешные ответы в прогрессе должны быть 0");
		assertEquals(0, progress.getFailAnswersCountAbsolute(), "Неуспешные ответы в прогрессе должны быть 0");

		log.trace("Проверка параметров первого вопроса");
		assertEquals(TaskDifficulty.Easy1, task.getDifficulty(), "Сложность первого вопроса должна быть Easy1");
		assertNotNull(task.getId(), "ID задачи должен быть установлен");
		assertFalse(task.getAnswers().isEmpty(), "Список ответов не должен быть пустым");
		assertEquals(TaskGrade.JUNIOR, task.getGrade(), "Грейд задачи должен быть JUNIOR");
		assertNotNull(task.getQuestion(), "Текст вопроса должен присутствовать");
		assertNotNull(task.getTopic(), "Тема вопроса должна быть установлена");

		log.info("Тестирование запуска экзамена завершено успешно");
	}

	/**
	 * Тестирует исключение при попытке неаутентифицированного пользователя запустить рейтинговый экзамен.
	 * <p>
	 * Проверяет систему безопасности:
	 * <ul>
	 *   <li>Блокировку доступа для анонимных пользователей к защищенным эндпоинтам</li>
	 *   <li>Возврат статуса 401 Unauthorized</li>
	 *   <li>Выброс соответствующего исключения AuthenticatedValidationException</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithAnonymousUser
	void shouldThrowAuthenticatedValidationExceptionWhenAnonymousUserAccessesProtectedEndpoint() throws Exception {
		log.info("Начало тестирования блокировки доступа для анонимных пользователей");

		log.debug("Отправка запроса от анонимного пользователя для запуска рейтингового экзамена");
		mockMvc.perform(post(URL_EXAM_ROOT + URL_START_EXAM)
						.param("examMode", ratingModeProperties.getMode().toString())
						.with(csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(result -> {
					log.trace("Проверка типа исключения: {}",
							result.getResolvedException() != null ? result.getResolvedException().getClass().getSimpleName() : "null");
					assertInstanceOf(AuthenticatedValidationException.class,
							result.getResolvedException(),
							"Должно быть выброшено исключение AuthenticatedValidationException");
				});

		log.info("Тестирование блокировки доступа завершено успешно");
	}
}
