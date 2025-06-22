package com.example.javaoffer.security.config;

import com.example.javaoffer.security.JwtAuthenticationFilter;
import com.example.javaoffer.security.dto.TokenResponseDTO;
import com.example.javaoffer.security.service.CustomCsrfTokenRepository;
import com.example.javaoffer.security.service.CustomUserDetailsService;
import com.example.javaoffer.security.service.JwtService;
import com.example.javaoffer.user.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static com.example.javaoffer.common.constants.SecurityConstant.*;
import static com.example.javaoffer.common.constants.UrlConstant.*;

/**
 * Конфигурация безопасности приложения.
 * <p>
 * Класс определяет настройки безопасности для всего приложения, включая:
 * <ul>
 *   <li>Маршруты, требующие аутентификации</li>
 *   <li>Управление JWT-токенами</li>
 *   <li>Защиту от CSRF-атак</li>
 *   <li>Настройки сессий (в данном случае - stateless)</li>
 *   <li>Конфигурацию аутентификации и авторизации</li>
 * </ul>
 *
 * @author Garbuzov Oleg
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

	// Пути для статических ресурсов
	private static final String[] STATIC_RESOURCES = {
			"/js/**",
			"/css/**",
			"/img/**",
			"/img/favicons/**",
			"/gifs/**"
	};

	// Пути для SEO-ресурсов
	private static final String[] SEO_RESOURCES = {
			"/robots.txt",
			"/sitemap.xml"
	};

	// Пути для публичного доступа
	private static final String[] PUBLIC_PATHS = {
			"/",
			"/modeSelect",
			"/contact",
			"/error",
			"/error/**",
			"/exam",
			"/testing/**",
			"/global-rating"
	};

	// Пути для админского доступа
	private static final String[] ADMIN_PATHS = {
			"/admin",
			"/admin/**",
			"/admin/debug/cache/**"
	};

	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;
	private final ObjectMapper objectMapper;
	private final CustomCsrfTokenRepository customCsrfTokenRepository;

	/**
	 * Определяет цепочку фильтров безопасности для приложения.
	 * <p>
	 * Настраивает следующие механизмы защиты:
	 * <ul>
	 *   <li>CSRF-защиту с использованием пользовательского репозитория токенов</li>
	 *   <li>Правила авторизации для различных URL-путей</li>
	 *   <li>Stateless сессии (без сохранения состояния на сервере)</li>
	 *   <li>Механизм выхода из системы с удалением соответствующих куки</li>
	 *   <li>Фильтр аутентификации JWT</li>
	 * </ul>
	 *
	 * @param http объект конфигурации HTTP-безопасности
	 * @return настроенная цепочка фильтров безопасности
	 * @throws Exception если возникает ошибка при конфигурации
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		log.info("Настройка цепочки фильтров безопасности");

		http
				.csrf(csrf -> csrf
						.csrfTokenRepository(customCsrfTokenRepository)
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
				)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								STATIC_RESOURCES
						).permitAll()
						.requestMatchers(
								SEO_RESOURCES
						).permitAll()
						.requestMatchers(
								PUBLIC_PATHS
						).permitAll()
						.requestMatchers(
								URL_LOGIN,
								URL_REGISTER
						).permitAll()
						.requestMatchers(
								ADMIN_PATHS
						).hasAnyAuthority(UserRole.ROLE_ADMIN.name())
						.anyRequest().authenticated()
				)
				.formLogin(form -> form
						.loginPage(URL_LOGIN)
						.loginProcessingUrl(URL_LOGIN)
						.successHandler(jsonAuthenticationSuccessHandler())
						.failureHandler(jsonAuthenticationFailureHandler())
						.permitAll()
				)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.logout(logout -> logout
						.logoutRequestMatcher(new AntPathRequestMatcher(URL_LOGOUT, HttpMethod.POST.name()))
						.logoutSuccessUrl(URL_ROOT)
						.deleteCookies(COOKIE_ACCESS_TOKEN, COOKIE_REFRESH_TOKEN, COOKIE_XSRF_TOKEN)
						.invalidateHttpSession(true)
				)
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

		log.info("Цепочка фильтров безопасности настроена успешно");
		return http.build();
	}

	/**
	 * Создает провайдер аутентификации, использующий хранилище пользователей
	 * и механизм кодирования паролей.
	 * <p>
	 * Провайдер отвечает за проверку учетных данных пользователя при входе в систему,
	 * используя загруженные из базы данных данные пользователя и сравнивая
	 * хешированные пароли.
	 *
	 * @return настроенный провайдер аутентификации
	 */
	@Bean
	public AuthenticationProvider authenticationProvider() {
		log.debug("Создание провайдера аутентификации");
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(customUserDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	/**
	 * Создает менеджер аутентификации из конфигурации аутентификации.
	 * <p>
	 * Менеджер аутентификации используется для программной аутентификации
	 * пользователей, например, при обработке REST API запросов.
	 *
	 * @param config конфигурация аутентификации
	 * @return менеджер аутентификации
	 * @throws Exception если возникла ошибка при создании менеджера аутентификации
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		log.debug("Создание менеджера аутентификации");
		return config.getAuthenticationManager();
	}

	/**
	 * Создает кодировщик паролей, используемый для хеширования паролей
	 * пользователей при регистрации и проверке при аутентификации.
	 * <p>
	 * Использует алгоритм BCrypt для безопасного хранения паролей.
	 * BCrypt автоматически генерирует соль и использует адаптивную функцию,
	 * которая замедляется по мере увеличения вычислительной мощности.
	 *
	 * @return кодировщик паролей BCrypt
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		log.debug("Создание кодировщика паролей BCrypt");
		return new BCryptPasswordEncoder();
	}

	/**
	 * Создает фильтр аутентификации JWT, отвечающий за проверку
	 * и обработку JWT-токенов в запросах.
	 * <p>
	 * Фильтр автоматически извлекает JWT-токены из заголовков или куки,
	 * проверяет их валидность и устанавливает контекст безопасности
	 * для аутентифицированного пользователя.
	 *
	 * @return настроенный фильтр аутентификации JWT
	 */
	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		log.debug("Создание фильтра аутентификации JWT");
		return new JwtAuthenticationFilter(jwtService, customUserDetailsService, objectMapper);
	}

	/**
	 * Создает обработчик успешной аутентификации.
	 * <p>
	 * При успешном входе пользователя в систему:
	 * <ul>
	 *   <li>Генерируются JWT-токены (access и refresh)</li>
	 *   <li>Токены сохраняются в HTTP-куки</li>
	 *   <li>Возвращается JSON-ответ с URL для перенаправления</li>
	 * </ul>
	 *
	 * @return обработчик успешной аутентификации
	 */
	@Bean
	public AuthenticationSuccessHandler jsonAuthenticationSuccessHandler() {
		return (_, response, authentication) -> {
			String username = authentication.getName();
			log.info("Успешная аутентификация пользователя: {}", username);

			UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
			TokenResponseDTO tokenResponseDTO = jwtService.generateTokens(userDetails);

			Cookie accessTokenCookie = new Cookie(COOKIE_ACCESS_TOKEN, tokenResponseDTO.getAccessToken());
			accessTokenCookie.setHttpOnly(true);
			accessTokenCookie.setPath("/");
			accessTokenCookie.setMaxAge((int) (tokenResponseDTO.getAccessTokenExpiration() / 1000));
			response.addCookie(accessTokenCookie);

			Cookie refreshTokenCookie = new Cookie(COOKIE_REFRESH_TOKEN, tokenResponseDTO.getRefreshToken());
			refreshTokenCookie.setHttpOnly(true);
			refreshTokenCookie.setPath("/");
			refreshTokenCookie.setMaxAge((int) (tokenResponseDTO.getRefreshTokenExpiration() / 1000));
			response.addCookie(refreshTokenCookie);

			response.setContentType("application/json;charset=UTF-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write("{\"redirect\":\"/\"}");

			log.debug("JWT токены установлены в куки для пользователя: {}", username);
		};
	}

	/**
	 * Создает обработчик неудачной аутентификации.
	 * <p>
	 * При неуспешной попытке входа в систему:
	 * <ul>
	 *   <li>Логируется попытка неудачной аутентификации</li>
	 *   <li>Возвращается JSON-ответ с описанием ошибки</li>
	 *   <li>Устанавливается HTTP статус 401 (Unauthorized)</li>
	 * </ul>
	 *
	 * @return обработчик неудачной аутентификации
	 */
	@Bean
	public AuthenticationFailureHandler jsonAuthenticationFailureHandler() {
		return (_, response, exception) -> {
			log.warn("Неудачная попытка аутентификации: {}", exception.getMessage());

			response.setContentType("application/json;charset=UTF-8");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			String message = exception.getMessage() != null ? exception.getMessage() : "Ошибка входа";
			response.getWriter().write("{\"error\":\"" + message.replace("\"", "'") + "\"}");
		};
	}
} 