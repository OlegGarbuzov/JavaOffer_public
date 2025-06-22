package com.example.javaoffer.common.utils;

import com.example.javaoffer.user.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Optional;

/**
 * Утилитарный класс для работы с информацией о клиенте.
 * <p>
 * Предоставляет статические методы для:
 * <ul>
 *   <li>Получения IP-адреса клиента с учетом прокси-серверов</li>
 *   <li>Получения ключа блокировки для синхронизации запросов</li>
 *   <li>Получения текущего аутентифицированного пользователя</li>
 * </ul>
 * 
 */
@Slf4j
public class ClientUtils {

	/**
	 * Получает реальный IP-адрес клиента с учетом прокси-серверов и балансировщиков нагрузки.
	 * <p>
	 * Метод проверяет наличие заголовка 'X-Forwarded-For', который обычно устанавливается 
	 * прокси-серверами и балансировщиками нагрузки. Если заголовок существует, метод берет 
	 * первый адрес из списка, который обычно является оригинальным IP-адресом клиента.
	 * 
	 * <p>
	 * Если заголовок 'X-Forwarded-For' отсутствует, метод использует стандартный метод 
	 * {@link HttpServletRequest#getRemoteAddr()} для получения IP-адреса.
	 * 
	 * 
	 * <p>
	 * Примеры:
	 * <ul>
	 *   <li>Без прокси: вернет значение из request.getRemoteAddr()</li>
	 *   <li>С прокси: X-Forwarded-For: 203.0.113.195, 70.41.3.18 - вернет 203.0.113.195</li>
	 * </ul>
	 * 
	 * 
	 * @param request HTTP-запрос, содержащий информацию о клиенте
	 * @return строка с IP-адресом клиента
	 */
	public static String getClientIp(HttpServletRequest request) {
		// Получаем заголовок X-Forwarded-For, если он существует
		String clientIp = request.getHeader("X-Forwarded-For");
		log.trace("Получение IP клиента. X-Forwarded-For: {}", clientIp);

		// Если заголовок отсутствует, берем remote address
		if (clientIp == null || clientIp.isEmpty()) {
			clientIp = request.getRemoteAddr();
			log.trace("X-Forwarded-For отсутствует, используем RemoteAddr: {}", clientIp);
		} else {
			// Если в заголовке несколько IP, берем первый
			clientIp = clientIp.split(",")[0];
			log.trace("Используем первый IP из X-Forwarded-For: {}", clientIp);
		}

		return clientIp;
	}

	/**
	 * Получает ключ блокировки для синхронизации запросов пользователя.
	 * <p>
	 * Сначала пытается получить XSRF-токен из cookie, если не найден — использует IP-адрес клиента.
	 * Этот ключ может использоваться для синхронизации запросов от одного пользователя,
	 * например, с помощью Striped Lock.
	 * 
	 *
	 * @param request HTTP-запрос пользователя
	 * @return объект-ключ для синхронизации
	 */
	public static Object getLockKeyByXsrfOrIp(HttpServletRequest request) {
		String clientIp = ClientUtils.getClientIp(request);
		log.trace("{}:getLockKey: Поиск ключа блокировки", clientIp);
		
		return Optional.ofNullable(request.getCookies()).stream()
				.flatMap(Arrays::stream)
				.filter(cookie -> "x-xsrf-token".equalsIgnoreCase(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.map(token -> {
					log.debug("{}:getLockKey: Используется XSRF токен из cookie: {}", clientIp, token);
					return token;
				})
				.orElseGet(() -> {
					log.warn("{}:getLockKey: XSRF токен не найден, используем IP", clientIp);
					return clientIp;
				});
	}

	/**
	 * Возвращает текущего авторизованного пользователя из контекста безопасности.
	 * <p>
	 * Метод проверяет, что:
	 * <ul>
	 *   <li>Аутентификация существует</li>
	 *   <li>Пользователь аутентифицирован</li>
	 *   <li>Пользователь не является анонимным</li>
	 *   <li>Объект Principal имеет тип User</li>
	 * </ul>
	 * 
	 *
	 * @return Optional с пользователем или пустой Optional, если пользователь - аноним
	 */
	public static Optional<User> getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		log.trace("Получение текущего пользователя из SecurityContext: {}", auth);
		if (auth != null &&
				auth.isAuthenticated() &&
				!(auth instanceof AnonymousAuthenticationToken) &&
				auth.getPrincipal() instanceof User) {
			log.debug("Пользователь аутентифицирован: {}", auth.getPrincipal());
			return Optional.of((User) auth.getPrincipal());
		}
		log.trace("Пользователь не аутентифицирован или анонимен");
		return Optional.empty();
	}
}
