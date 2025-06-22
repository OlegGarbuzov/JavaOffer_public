package com.example.javaoffer.security;

import com.example.javaoffer.security.dto.TokenResponseDTO;
import com.example.javaoffer.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static com.example.javaoffer.common.constants.SecurityConstant.*;

/**
 * Фильтр для обработки JWT-токенов в запросах.
 * <p>
 * Этот фильтр проверяет наличие и валидность JWT-токена в запросе
 * и устанавливает аутентификацию пользователя в SecurityContextHolder.
 * Также обрабатывает автоматическое обновление токенов при истечении
 * срока действия access-токена и наличии валидного refresh-токена.
 *
 * @author Garbuzov Oleg
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final ObjectMapper objectMapper;

	/**
	 * Основной метод фильтра, выполняющий проверку и обработку JWT-токенов.
	 * <p>
	 * Процесс обработки:
	 * <ol>
	 *   <li>Извлечение JWT-токена из запроса (заголовка или куки)</li>
	 *   <li>Если токен отсутствует, запрос передается дальше без изменений</li>
	 *   <li>Извлечение имени пользователя из токена</li>
	 *   <li>Если имя пользователя валидно и аутентификация не установлена, загрузка данных пользователя</li>
	 *   <li>Проверка валидности токена для данного пользователя</li>
	 *   <li>Если токен валиден, устанавливает аутентификацию пользователя</li>
	 *   <li>Если access-токен истек, но имеется валидный refresh-токен, генерирует новую пару токенов и устанавливает аутентификацию</li>
	 *   <li>Передача запроса следующему фильтру в цепочке</li>
	 * </ol>
	 *
	 * @param request     HTTP-запрос
	 * @param response    HTTP-ответ
	 * @param filterChain цепочка фильтров
	 * @throws ServletException если произошла ошибка в сервлете
	 * @throws IOException      если произошла ошибка ввода-вывода
	 */
	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		log.trace("Обработка запроса в JwtAuthenticationFilter: {}", request.getRequestURI());

		String jwt = getJwtFromRequest(request);
		if (jwt == null) {
			log.trace("JWT токен не найден в запросе: {}", request.getRequestURI());
			filterChain.doFilter(request, response);
			return;
		}

		String userName = jwtService.extractUsername(jwt);
		log.debug("Извлечено имя пользователя из JWT: {}", userName);

		if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				UserDetails userDetails = this.userDetailsService.loadUserByUsername(userName);
				log.trace("Загружены данные пользователя: {}", userName);

				if (jwtService.isTokenValid(jwt, userDetails)) {
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							userDetails,
							null,
							userDetails.getAuthorities()
					);
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
					log.debug("Успешная аутентификация пользователя: {}", userName);
				} else {
					log.debug("Access токен истек для пользователя: {}, пытаемся обновить", userName);

					String refreshToken = getRefreshTokenFromRequest(request);
					if (refreshToken != null && jwtService.isTokenValid(refreshToken, userDetails)) {
						log.info("Обновление токенов для пользователя: {}", userName);

						TokenResponseDTO newTokens = jwtService.generateTokens(userDetails);

						UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
								userDetails,
								null,
								userDetails.getAuthorities()
						);
						authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authToken);

						Cookie accessTokenCookie = new Cookie(COOKIE_ACCESS_TOKEN, newTokens.getAccessToken());
						accessTokenCookie.setHttpOnly(true);
						accessTokenCookie.setPath("/");
						accessTokenCookie.setMaxAge((int) (newTokens.getAccessTokenExpiration() / 1000));
						response.addCookie(accessTokenCookie);

						Cookie refreshTokenCookie = new Cookie(COOKIE_REFRESH_TOKEN, newTokens.getRefreshToken());
						refreshTokenCookie.setHttpOnly(true);
						refreshTokenCookie.setPath("/");
						refreshTokenCookie.setMaxAge((int) (newTokens.getRefreshTokenExpiration() / 1000));
						response.addCookie(refreshTokenCookie);

						log.info("Токены успешно обновлены для пользователя: {}", userName);
					} else {
						log.warn("Refresh токен недействителен или отсутствует для пользователя: {}", userName);
					}
				}
			} catch (Exception e) {
				log.error("Ошибка при аутентификации пользователя {}: {}", userName, e.getMessage());
			}
		}

		filterChain.doFilter(request, response);
	}

	/**
	 * Извлекает JWT-токен из HTTP-запроса.
	 * <p>
	 * Пытается получить токен в следующем порядке:
	 * <ol>
	 *   <li>Из заголовка Authorization (формат "Bearer {token}")</li>
	 *   <li>Из куки access_token</li>
	 * </ol>
	 *
	 * @param request HTTP-запрос
	 * @return JWT-токен или null, если токен не найден
	 */
	private String getJwtFromRequest(HttpServletRequest request) {
		String authHeader = request.getHeader(AUTHORIZATION_HEADER);
		if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
			log.trace("JWT токен получен из заголовка Authorization");
			return authHeader.substring(BEARER_PREFIX.length());
		}

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			Optional<Cookie> accessTokenCookie = Arrays.stream(cookies)
					.filter(cookie -> COOKIE_ACCESS_TOKEN.equals(cookie.getName()))
					.findFirst();
			if (accessTokenCookie.isPresent()) {
				log.trace("JWT токен получен из cookie");
				return accessTokenCookie.get().getValue();
			}
		}

		return null;
	}

	/**
	 * Извлекает refresh-токен из HTTP-запроса.
	 * <p>
	 * Пытается получить токен в следующем порядке:
	 * <ol>
	 *   <li>Из заголовка Refresh-Token</li>
	 *   <li>Из куки refresh_token</li>
	 * </ol>
	 *
	 * @param request HTTP-запрос
	 * @return refresh-токен или null, если токен не найден
	 */
	private String getRefreshTokenFromRequest(HttpServletRequest request) {
		String refreshTokenHeader = request.getHeader(REFRESH_TOKEN_HEADER);
		if (refreshTokenHeader != null) {
			log.trace("Refresh токен получен из заголовка");
			return refreshTokenHeader;
		}

		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			Optional<Cookie> refreshTokenCookie = Arrays.stream(cookies)
					.filter(cookie -> COOKIE_REFRESH_TOKEN.equals(cookie.getName()))
					.findFirst();
			if (refreshTokenCookie.isPresent()) {
				log.trace("Refresh токен получен из cookie");
				return refreshTokenCookie.get().getValue();
			}
		}

		return null;
	}
} 