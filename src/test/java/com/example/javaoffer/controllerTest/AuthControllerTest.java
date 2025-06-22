package com.example.javaoffer.controllerTest;

import com.example.javaoffer.common.exception.dto.ErrorResponse;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.enums.UserRole;
import com.example.javaoffer.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static TestConstants.UserConstant.*;
import static com.example.javaoffer.common.constants.SecurityConstant.*;
import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_LOGIN;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_REGISTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для контроллера аутентификации и авторизации.
 * <p>
 * Тестирует полный цикл аутентификации пользователей:
 * <ul>
 *   <li>Отображение страниц входа и регистрации</li>
 *   <li>Процесс регистрации новых пользователей</li>
 *   <li>Аутентификацию существующих пользователей</li>
 *   <li>Процесс выхода из системы</li>
 *   <li>Валидацию входных данных</li>
 * </ul>
 * </p>
 * <p>
 * Проверяет корректность:
 * <ul>
 *   <li>HTTP статусов ответов</li>
 *   <li>Редиректов для аутентифицированных пользователей</li>
 *   <li>Установки и удаления JWT токенов в cookies</li>
 *   <li>Валидации пользовательских данных</li>
 *   <li>Обработки ошибок аутентификации</li>
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
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserService userService;

	/**
	 * Тестирует отображение страницы входа в систему.
	 * <p>
	 * Проверяет два сценария:
	 * <ul>
	 *   <li>Доступ неаутентифицированного пользователя - показ страницы входа</li>
	 *   <li>Доступ аутентифицированного пользователя - редирект на главную страницу</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Верифицирует установку CSRF токена и наличие необходимых атрибутов модели.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запросов
	 */
	@Test
	void showLoginPageTest() throws Exception {
		log.info("Начало тестирования страницы входа");
		
		log.debug("Тестирование доступа неаутентифицированного пользователя к странице входа");
		mockMvc.perform(get(URL_LOGIN))
				.andExpect(status().isOk())
				.andExpect(cookie().exists(COOKIE_XSRF_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_XSRF_TOKEN, false))
				.andExpect(view().name(VIEW_TEMPLATE_LOGIN))
				.andExpect(model().attributeExists("request"));

		log.debug("Тестирование редиректа аутентифицированного пользователя");
		mockMvc.perform(get(URL_LOGIN)
						.with(user(USER_NAME).roles(USER_ROLE_STR)))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl(URL_ROOT));
		
		log.info("Тестирование страницы входа завершено успешно");
	}

	/**
	 * Тестирует отображение страницы регистрации.
	 * <p>
	 * Проверяет два сценария:
	 * <ul>
	 *   <li>Доступ неаутентифицированного пользователя - показ страницы регистрации</li>
	 *   <li>Доступ аутентифицированного пользователя - редирект на главную страницу</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Верифицирует установку CSRF токена и наличие необходимых атрибутов модели.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запросов
	 */
	@Test
	void showRegistrationPageTest() throws Exception {
		log.info("Начало тестирования страницы регистрации");
		
		log.debug("Тестирование доступа неаутентифицированного пользователя к странице регистрации");
		mockMvc.perform(get(URL_REGISTER))
				.andExpect(status().isOk())
				.andExpect(cookie().exists(COOKIE_XSRF_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_XSRF_TOKEN, false))
				.andExpect(view().name(VIEW_TEMPLATE_REGISTER))
				.andExpect(model().attributeExists("request"));

		log.debug("Тестирование редиректа аутентифицированного пользователя");
		mockMvc.perform(get(URL_REGISTER)
						.with(user(USER_NAME).roles(USER_ROLE_STR)))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl(URL_ROOT));
		
		log.info("Тестирование страницы регистрации завершено успешно");
	}

	/**
	 * Тестирует процесс регистрации пользователей с различными входными данными.
	 * <p>
	 * Проверяет следующие сценарии валидации:
	 * <ul>
	 *   <li>Невалидное имя пользователя (слишком короткое)</li>
	 *   <li>Невалидные email адреса (различные форматы)</li>
	 *   <li>Несовпадающие пароли</li>
	 *   <li>Невалидный пароль (слишком короткий)</li>
	 *   <li>Успешную регистрацию с корректными данными</li>
	 *   <li>Конфликты при дублировании имени пользователя или email</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запросов
	 */
	@Test
	void registrationTest() throws Exception {
		log.info("Начало тестирования процесса регистрации");
		
		log.debug("Тестирование регистрации с невалидным именем пользователя");
		mockMvc.perform(post(URL_REGISTER)
						.param("username", USER_NAME_NON_VALID)
						.param("userEmail", USER_EMAIL)
						.param("password", USER_PASSWORD)
						.param("passwordConfirm", USER_PASSWORD)
						.with(csrf()))
				.andExpect(status().isBadRequest());

		log.debug("Тестирование регистрации с невалидным email (первый формат)");
		mockMvc.perform(post(URL_REGISTER)
						.param("username", USER_NAME)
						.param("userEmail", USER_EMAIL_NON_VALID_1)
						.param("password", USER_PASSWORD)
						.param("passwordConfirm", USER_PASSWORD)
						.with(csrf()))
				.andExpect(status().isBadRequest());

		log.debug("Тестирование регистрации с невалидным email (второй формат)");
		mockMvc.perform(post(URL_REGISTER)
						.param("username", USER_NAME)
						.param("userEmail", USER_EMAIL_NON_VALID_2)
						.param("password", USER_PASSWORD)
						.param("passwordConfirm", USER_PASSWORD)
						.with(csrf()))
				.andExpect(status().isBadRequest());

		log.debug("Тестирование регистрации с несовпадающими паролями");
		mockMvc.perform(post(URL_REGISTER)
						.param("username", USER_NAME)
						.param("userEmail", USER_EMAIL)
						.param("password", USER_PASSWORD)
						.param("passwordConfirm", USER_PASSWORD_2)
						.with(csrf()))
				.andExpect(status().isBadRequest());

		log.debug("Тестирование регистрации с невалидным паролем");
		mockMvc.perform(post(URL_REGISTER)
						.param("username", USER_NAME)
						.param("userEmail", USER_EMAIL)
						.param("password", USER_PASSWORD_NON_VALID)
						.param("passwordConfirm", USER_PASSWORD_NON_VALID)
						.with(csrf()))
				.andExpect(status().isBadRequest());

		log.debug("Тестирование успешной регистрации");
		mockMvc.perform(post(URL_REGISTER)
						.param("username", USER_NAME)
						.param("userEmail", USER_EMAIL)
						.param("password", USER_PASSWORD)
						.param("passwordConfirm", USER_PASSWORD)
						.with(csrf()))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl(URL_LOGIN));

		log.debug("Тестирование регистрации с существующим именем пользователя");
		mockMvc.perform(post(URL_REGISTER)
						.param("username", USER_NAME)
						.param("userEmail", USER_EMAIL_2)
						.param("password", USER_PASSWORD)
						.param("passwordConfirm", USER_PASSWORD)
						.with(csrf()))
				.andExpect(status().isConflict());

		log.debug("Тестирование регистрации с существующим email");
		mockMvc.perform(post(URL_REGISTER)
						.param("username", USER_NAME_2)
						.param("userEmail", USER_EMAIL)
						.param("password", USER_PASSWORD)
						.param("passwordConfirm", USER_PASSWORD)
						.with(csrf()))
				.andExpect(status().isConflict());
		
		log.info("Тестирование процесса регистрации завершено успешно");
	}

	/**
	 * Тестирует процессы аутентификации и выхода из системы.
	 * <p>
	 * Проверяет следующие сценарии:
	 * <ul>
	 *   <li>Успешный вход с корректными учетными данными</li>
	 *   <li>Установку JWT токенов (access и refresh) в cookies</li>
	 *   <li>Успешный выход из системы</li>
	 *   <li>Удаление JWT токенов при выходе</li>
	 *   <li>Обработку некорректных учетных данных</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запросов или парсинга JSON
	 */
	@Test
	void loginAndLogoutTest() throws Exception {
		log.info("Начало тестирования процессов входа и выхода");
		
		log.debug("Создание тестового пользователя");
		createTestUser();

		log.debug("Тестирование успешного входа в систему");
		mockMvc.perform(post(URL_LOGIN)
						.param("username", USER_NAME)
						.param("password", USER_PASSWORD)
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(cookie().exists(COOKIE_ACCESS_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_ACCESS_TOKEN, true))
				.andExpect(cookie().path(COOKIE_ACCESS_TOKEN, URL_ROOT))
				.andExpect(cookie().exists(COOKIE_REFRESH_TOKEN))
				.andExpect(cookie().httpOnly(COOKIE_REFRESH_TOKEN, true))
				.andExpect(cookie().path(COOKIE_REFRESH_TOKEN, URL_ROOT));

		log.debug("Тестирование выхода из системы");
		mockMvc.perform(post(URL_LOGOUT)
						.with(csrf()))
				.andExpect(status().isFound())
				.andExpect(cookie().maxAge(COOKIE_ACCESS_TOKEN, 0))
				.andExpect(cookie().maxAge(COOKIE_REFRESH_TOKEN, 0))
				.andExpect(header().stringValues("Set-Cookie", Matchers.hasItems(
						Matchers.containsString(COOKIE_ACCESS_TOKEN + "=;"),
						Matchers.containsString(COOKIE_REFRESH_TOKEN + "=;")
				)))
				.andExpect(redirectedUrl(URL_ROOT));

		log.debug("Тестирование входа с некорректными учетными данными");
		MvcResult mvcResult = mockMvc.perform(post(URL_LOGIN)
						.param("username", USER_NAME_2)
						.param("password", USER_PASSWORD)
						.with(csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(cookie().doesNotExist(COOKIE_ACCESS_TOKEN))
				.andExpect(cookie().doesNotExist(COOKIE_REFRESH_TOKEN))
				.andReturn();

		log.trace("Проверка содержимого ответа при неуспешной аутентификации");
		String responseJson = mvcResult.getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		ErrorResponse errorResponse = mapper.readValue(responseJson, ErrorResponse.class);

		assertEquals("Bad credentials", errorResponse.error(), 
			"Сообщение об ошибке должно быть 'Bad credentials'");
		
		log.info("Тестирование процессов входа и выхода завершено успешно");
	}

	/**
	 * Вспомогательный метод для создания тестового пользователя в базе данных.
	 * <p>
	 * Создает пользователя с предустановленными тестовыми данными из констант.
	 * Используется в тестах, требующих наличия существующего пользователя.
	 * </p>
	 */
	private void createTestUser() {
		log.trace("Создание тестового пользователя: {}", USER_NAME);
		userService.createUser(User.builder()
				.username(USER_NAME)
				.email(USER_EMAIL)
				.password(USER_PASSWORD)
				.role(UserRole.ROLE_USER)
				.build());
		log.trace("Тестовый пользователь создан успешно");
	}
}