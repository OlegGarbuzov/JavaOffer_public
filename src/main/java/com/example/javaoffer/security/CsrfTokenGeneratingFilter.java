package com.example.javaoffer.security;

import com.example.javaoffer.security.service.CustomCsrfTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр для генерации CSRF-токенов на каждый запрос.
 * <p>
 * Этот фильтр обеспечивает, что CSRF-токен будет сгенерирован и установлен
 * в запрос и ответ для каждого не-исключенного запроса. Это позволяет
 * фронтенду получать и использовать CSRF-токены для защиты от CSRF-атак.
 * </p>
 * Статические ресурсы (CSS, JS, изображения и т.д.) исключаются из обработки
 * для оптимизации производительности.
 *
 * @author Garbuzov Oleg
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CsrfTokenGeneratingFilter extends OncePerRequestFilter {

	private final CustomCsrfTokenRepository customCsrfTokenRepository;

	/**
	 * Основной метод фильтра, который генерирует и устанавливает CSRF-токен
	 * для каждого не-статического запроса.
	 * <p>
	 * Процесс работы:
	 * <ol>
	 *   <li>Проверяет, является ли запрос запросом к статическому ресурсу. Если да, пропускает запрос без изменений.</li>
	 *   <li>Пытается загрузить существующий CSRF-токен из запроса.</li>
	 *   <li>Если токен не найден, генерирует новый и сохраняет его в ответе (в куки).</li>
	 *   <li>Устанавливает токен как атрибут запроса для использования в представлениях.</li>
	 *   <li>Передает управление следующему фильтру в цепочке.</li>
	 * </ol>
	 *
	 * @param request     HTTP-запрос
	 * @param response    HTTP-ответ
	 * @param filterChain цепочка фильтров
	 * @throws ServletException если произошла ошибка в сервлете
	 * @throws IOException      если произошла ошибка ввода-вывода
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
			throws ServletException, IOException {

		String requestUri = request.getRequestURI();
		log.trace("Обработка запроса в CsrfTokenGeneratingFilter: {}", requestUri);

		if (requestUri.startsWith("/css")
				|| requestUri.startsWith("/js")
				|| requestUri.startsWith("/img")
				|| requestUri.startsWith("/gifs")
				|| requestUri.endsWith(".map")) {
			log.trace("Пропуск статического ресурса: {}", requestUri);
			filterChain.doFilter(request, response);
			return;
		}

		CsrfToken token = customCsrfTokenRepository.loadToken(request);
		if (token == null) {
			log.debug("CSRF токен не найден, генерируем новый для запроса: {}", requestUri);
			token = customCsrfTokenRepository.generateToken(request);
			customCsrfTokenRepository.saveToken(token, request, response);

			request.setAttribute(CsrfToken.class.getName(), token);
			request.setAttribute(token.getParameterName(), token);

			log.trace("Новый CSRF токен сгенерирован и установлен для запроса: {}", requestUri);
		} else {
			log.trace("Использован существующий CSRF токен для запроса: {}", requestUri);
		}

		filterChain.doFilter(request, response);
	}
}
