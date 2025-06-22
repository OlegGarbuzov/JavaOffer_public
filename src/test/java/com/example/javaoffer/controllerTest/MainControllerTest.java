package com.example.javaoffer.controllerTest;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static TestConstants.UserConstant.USER_NAME;
import static TestConstants.UserConstant.USER_ROLE_STR;
import static com.example.javaoffer.common.constants.SecurityConstant.COOKIE_XSRF_TOKEN;
import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для MainController.
 * <p>
 * Тестирует основные страницы приложения:
 * <ul>
 *   <li>Главную страницу приложения</li>
 *   <li>Страницу контактов</li>
 *   <li>Страницу выбора режима экзамена</li>
 * </ul>
 * </p>
 * <p>
 * Проверяет корректность:
 * <ul>
 *   <li>HTTP статусов ответов</li>
 *   <li>Возвращаемых представлений (view)</li>
 *   <li>Наличия необходимых атрибутов модели</li>
 *   <li>Установки CSRF токенов</li>
 *   <li>Поведения для аутентифицированных и неаутентифицированных пользователей</li>
 * </ul>
 * </p>
 *
 * @author Garbuzov Oleg
 * @since 1.0
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MainControllerTest {

	@Autowired
	private MockMvc mockMvc;

	/**
	 * Тестирует отображение главной страницы приложения.
	 * <p>
	 * Проверяет корректность загрузки главной страницы для:
	 * <ul>
	 *   <li>Неаутентифицированных пользователей</li>
	 *   <li>Аутентифицированных пользователей</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Верифицирует:
	 * <ul>
	 *   <li>HTTP статус 200 OK</li>
	 *   <li>Корректное имя представления</li>
	 *   <li>Наличие обязательных атрибутов модели</li>
	 *   <li>Установку CSRF токена</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запросов
	 */
	@Test
	void showMainPage() throws Exception {
		log.info("Начало тестирования главной страницы");
		
		log.debug("Тестирование главной страницы для неаутентифицированного пользователя");
		mockMvc.perform(get(URL_ROOT))
				.andExpect(cookie().exists(COOKIE_XSRF_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_XSRF_TOKEN, false))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_INDEX))
				.andExpect(model().attributeExists("request"));

		log.debug("Тестирование главной страницы для аутентифицированного пользователя");
		mockMvc.perform(get(URL_ROOT)
						.with(user(USER_NAME).roles(USER_ROLE_STR)))
				.andExpect(cookie().exists(COOKIE_XSRF_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_XSRF_TOKEN, false))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_INDEX))
				.andExpect(model().attributeExists("request"));
		
		log.info("Тестирование главной страницы завершено успешно");
	}

	/**
	 * Тестирует отображение страницы контактов.
	 * <p>
	 * Проверяет корректность загрузки страницы контактов для:
	 * <ul>
	 *   <li>Неаутентифицированных пользователей</li>
	 *   <li>Аутентифицированных пользователей</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Верифицирует:
	 * <ul>
	 *   <li>HTTP статус 200 OK</li>
	 *   <li>Корректное имя представления</li>
	 *   <li>Наличие обязательных атрибутов модели</li>
	 *   <li>Установку CSRF токена</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запросов
	 */
	@Test
	void showContactPage() throws Exception {
		log.info("Начало тестирования страницы контактов");
		
		log.debug("Тестирование страницы контактов для неаутентифицированного пользователя");
		mockMvc.perform(get(URL_CONTACT))
				.andExpect(cookie().exists(COOKIE_XSRF_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_XSRF_TOKEN, false))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_CONTACT))
				.andExpect(model().attributeExists("request"));

		log.debug("Тестирование страницы контактов для аутентифицированного пользователя");
		mockMvc.perform(get(URL_CONTACT)
						.with(user(USER_NAME).roles(USER_ROLE_STR)))
				.andExpect(cookie().exists(COOKIE_XSRF_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_XSRF_TOKEN, false))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_CONTACT))
				.andExpect(model().attributeExists("request"));
		
		log.info("Тестирование страницы контактов завершено успешно");
	}

	/**
	 * Тестирует отображение страницы выбора режима экзамена.
	 * <p>
	 * Проверяет корректность загрузки страницы выбора режима для:
	 * <ul>
	 *   <li>Неаутентифицированных пользователей</li>
	 *   <li>Аутентифицированных пользователей</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Верифицирует:
	 * <ul>
	 *   <li>HTTP статус 200 OK</li>
	 *   <li>Корректное имя представления</li>
	 *   <li>Наличие обязательных атрибутов модели</li>
	 *   <li>Установку CSRF токена</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запросов
	 */
	@Test
	void showModeSelectPage() throws Exception {
		log.info("Начало тестирования страницы выбора режима");
		
		log.debug("Тестирование страницы выбора режима для неаутентифицированного пользователя");
		mockMvc.perform(get(URL_MODE_SELECT))
				.andExpect(cookie().exists(COOKIE_XSRF_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_XSRF_TOKEN, false))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_MODE_SELECT))
				.andExpect(model().attributeExists("request"));

		log.debug("Тестирование страницы выбора режима для аутентифицированного пользователя");
		mockMvc.perform(get(URL_MODE_SELECT)
						.with(user(USER_NAME).roles(USER_ROLE_STR)))
				.andExpect(cookie().exists(COOKIE_XSRF_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_XSRF_TOKEN, false))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_MODE_SELECT))
				.andExpect(model().attributeExists("request"));
		
		log.info("Тестирование страницы выбора режима завершено успешно");
	}
}
