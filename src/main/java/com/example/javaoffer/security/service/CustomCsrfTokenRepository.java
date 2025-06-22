package com.example.javaoffer.security.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.example.javaoffer.common.constants.SecurityConstant.COOKIE_XSRF_TOKEN;
import static com.example.javaoffer.common.constants.SecurityConstant.CSRF_PARAMETER_NAME;

/**
 * Пользовательская реализация репозитория CSRF-токенов.
 * <p>
 * Этот класс отвечает за:
 * <ul>
 *   <li>Генерацию новых CSRF-токенов</li>
 *   <li>Сохранение токенов в куки браузера</li>
 *   <li>Загрузку токенов из запросов</li>
 * </ul>
 * <p>
 * CSRF-токены используются для защиты от Cross-Site Request Forgery атак,
 * предотвращая выполнение нежелательных действий от имени аутентифицированного
 * пользователя.
 * 
 *
 * @author Garbuzov Oleg
 */
@Component
@Slf4j
public class CustomCsrfTokenRepository implements CsrfTokenRepository {

	/**
	 * Генерирует новый CSRF-токен.
	 * <p>
	 * Создает уникальный токен на основе случайного UUID.
	 * Токен будет использован для защиты форм от CSRF-атак.
	 * 
	 *
	 * @param request текущий HTTP-запрос
	 * @return новый объект CsrfToken, содержащий случайный UUID
	 */
	@Override
	public CsrfToken generateToken(HttpServletRequest request) {
		String token = UUID.randomUUID().toString();
		log.debug("Сгенерирован новый CSRF токен для запроса: {}", request.getRequestURI());
		return new DefaultCsrfToken(COOKIE_XSRF_TOKEN, CSRF_PARAMETER_NAME, token);
	}

	/**
	 * Сохраняет CSRF-токен в HTTP-ответе как куки.
	 * <p>
	 * Куки настраивается следующим образом:
	 * <ul>
	 *   <li>Путь: "/" (доступен для всего сайта)</li>
	 *   <li>HttpOnly: false (доступен для JavaScript на фронтенде)</li>
	 *   <li>Secure: в зависимости от того, использует ли запрос HTTPS</li>
	 * </ul>
	 * 
	 *
	 * @param token    CSRF-токен для сохранения
	 * @param request  текущий HTTP-запрос
	 * @param response HTTP-ответ, в который будет добавлена куки
	 */
	@Override
	public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
		if (token == null) {
			log.trace("CSRF токен null, сохранение пропущено");
			return;
		}

		log.trace("Сохранение CSRF токена в куки для запроса: {}", request.getRequestURI());
		
		Cookie cookie = new Cookie(COOKIE_XSRF_TOKEN, token.getToken());
		cookie.setPath("/");
		cookie.setHttpOnly(false);
		cookie.setSecure(request.isSecure());

		response.addCookie(cookie);
	}

	/**
	 * Загружает CSRF-токен из HTTP-запроса.
	 * <p>
	 * Метод ищет куки с именем X-XSRF-TOKEN в запросе и, если находит,
	 * создает объект CsrfToken с соответствующим значением.
	 * 
	 *
	 * @param request HTTP-запрос, из которого нужно извлечь токен
	 * @return объект CsrfToken или null, если токен не найден
	 */
	@Override
	public CsrfToken loadToken(HttpServletRequest request) {
		log.trace("Поиск CSRF токена в запросе: {}", request.getRequestURI());
		
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (COOKIE_XSRF_TOKEN.equals(cookie.getName())) {
					log.trace("CSRF токен найден в куки");
					return new DefaultCsrfToken(COOKIE_XSRF_TOKEN, CSRF_PARAMETER_NAME, cookie.getValue());
				}
			}
		}
		
		log.trace("CSRF токен не найден в запросе");
		return null;
	}
}