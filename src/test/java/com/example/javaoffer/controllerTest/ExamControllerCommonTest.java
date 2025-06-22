package com.example.javaoffer.controllerTest;

import com.example.javaoffer.exam.dto.ExamAbortRequestDTO;
import com.example.javaoffer.exam.dto.ExamCheckAnswerRequestDTO;
import com.example.javaoffer.exam.dto.ExamNextQuestionRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.h2.engine.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static com.example.javaoffer.common.constants.UrlConstant.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для общей валидации запросов в ExamController.
 * <p>
 * Тестирует валидацию входящих данных для различных эндпоинтов:
 * <ul>
 *   <li>Запроса следующего вопроса экзамена</li>
 *   <li>Проверки ответа на вопрос</li>
 *   <li>Прерывания экзамена</li>
 * </ul>
 * </p>
 * <p>
 * Проверяет корректность:
 * <ul>
 *   <li>Валидации @NotNull аннотаций</li>
 *   <li>Возврата статуса 400 Bad Request при невалидных данных</li>
 *   <li>Выброса MethodArgumentNotValidException</li>
 *   <li>Обработки JSON запросов с некорректными полями</li>
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
class ExamControllerCommonTest {

	@Autowired
	private MockMvc mockMvc;

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
	 * Тестирует валидацию полей запросов для различных эндпоинтов экзамена.
	 * <p>
	 * Проверяет обработку невалидных данных в следующих сценариях:
	 * <ul>
	 *   <li>ExamNextQuestionRequestDTO с null значениями examId и requestId</li>
	 *   <li>ExamCheckAnswerRequestDTO с null значениями examId, requestId и selectedAnswer</li>
	 *   <li>ExamAbortRequestDTO с null значением examId</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Каждый тест должен возвращать:
	 * <ul>
	 *   <li>HTTP статус 400 Bad Request</li>
	 *   <li>Исключение MethodArgumentNotValidException</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запросов или сериализации JSON
	 */
	@Test
	void shouldReturnBadRequestWhenRequestFieldsIsInvalid() throws Exception {
		log.info("Начало тестирования валидации полей запросов экзамена");

		ObjectMapper objectMapper = new ObjectMapper();

		log.debug("Тестирование ExamNextQuestionRequestDTO с null examId");
		ExamNextQuestionRequestDTO invalidNextQuestionExamIdNull = ExamNextQuestionRequestDTO.builder()
				.examId(null)
				.requestId(UUID.randomUUID())
				.build();
		String json = objectMapper.writeValueAsString(invalidNextQuestionExamIdNull);

		mockMvc.perform(post(URL_EXAM_ROOT + URL_NEXT_QUESTION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()).with(user("testUser").roles(String.valueOf(Role.USER))))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					log.trace("Проверка исключения для null examId в ExamNextQuestionRequestDTO");
					assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException(),
							"Должно быть выброшено MethodArgumentNotValidException для null examId");
				});

		log.debug("Тестирование ExamNextQuestionRequestDTO с null requestId");
		ExamNextQuestionRequestDTO invalidNextQuestionRequestIdNull = ExamNextQuestionRequestDTO.builder()
				.examId(UUID.randomUUID())
				.requestId(null)
				.build();
		json = objectMapper.writeValueAsString(invalidNextQuestionRequestIdNull);

		mockMvc.perform(post(URL_EXAM_ROOT + URL_NEXT_QUESTION)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()).with(user("testUser").roles(String.valueOf(Role.USER))))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					log.trace("Проверка исключения для null requestId в ExamNextQuestionRequestDTO");
					assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException(),
							"Должно быть выброшено MethodArgumentNotValidException для null requestId");
				});

		log.debug("Тестирование ExamCheckAnswerRequestDTO с null examId");
		ExamCheckAnswerRequestDTO invalidCheckAnswerExamIdNull = ExamCheckAnswerRequestDTO.builder()
				.examId(null)
				.requestId(UUID.randomUUID())
				.selectedAnswer(0L)
				.build();
		json = objectMapper.writeValueAsString(invalidCheckAnswerExamIdNull);

		mockMvc.perform(post(URL_EXAM_ROOT + URL_ANSWER_CHECK)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()).with(user("testUser").roles(String.valueOf(Role.USER))))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					log.trace("Проверка исключения для null examId в ExamCheckAnswerRequestDTO");
					assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException(),
							"Должно быть выброшено MethodArgumentNotValidException для null examId");
				});

		log.debug("Тестирование ExamCheckAnswerRequestDTO с null requestId");
		ExamCheckAnswerRequestDTO invalidCheckAnswerRequestIdNull = ExamCheckAnswerRequestDTO.builder()
				.examId(UUID.randomUUID())
				.requestId(null)
				.selectedAnswer(0L)
				.build();
		json = objectMapper.writeValueAsString(invalidCheckAnswerRequestIdNull);

		mockMvc.perform(post(URL_EXAM_ROOT + URL_ANSWER_CHECK)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()).with(user("testUser").roles(String.valueOf(Role.USER))))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					log.trace("Проверка исключения для null requestId в ExamCheckAnswerRequestDTO");
					assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException(),
							"Должно быть выброшено MethodArgumentNotValidException для null requestId");
				});

		log.debug("Тестирование ExamCheckAnswerRequestDTO с null selectedAnswer");
		ExamCheckAnswerRequestDTO invalidCheckAnswerSelectedAnswerNull = ExamCheckAnswerRequestDTO.builder()
				.examId(UUID.randomUUID())
				.requestId(UUID.randomUUID())
				.selectedAnswer(null)
				.build();
		json = objectMapper.writeValueAsString(invalidCheckAnswerSelectedAnswerNull);

		mockMvc.perform(post(URL_EXAM_ROOT + URL_ANSWER_CHECK)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()).with(user("testUser").roles(String.valueOf(Role.USER))))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					log.trace("Проверка исключения для null selectedAnswer в ExamCheckAnswerRequestDTO");
					assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException(),
							"Должно быть выброшено MethodArgumentNotValidException для null selectedAnswer");
				});

		log.debug("Тестирование ExamAbortRequestDTO с null examId");
		ExamAbortRequestDTO invalidAbortRequestExamIdNull = ExamAbortRequestDTO.builder()
				.examId(null)
				.build();
		json = objectMapper.writeValueAsString(invalidAbortRequestExamIdNull);

		mockMvc.perform(post(URL_EXAM_ROOT + URL_ABORT_EXAM)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json)
						.with(csrf()).with(user("testUser").roles(String.valueOf(Role.USER))))
				.andExpect(status().isBadRequest())
				.andExpect(result -> {
					log.trace("Проверка исключения для null examId в ExamAbortRequestDTO");
					assertInstanceOf(MethodArgumentNotValidException.class, result.getResolvedException(),
							"Должно быть выброшено MethodArgumentNotValidException для null examId");
				});

		log.info("Тестирование валидации полей запросов завершено успешно");
	}
}
