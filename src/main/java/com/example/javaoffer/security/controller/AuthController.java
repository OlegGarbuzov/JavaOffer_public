package com.example.javaoffer.security.controller;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.security.dto.RegistrationRequestDTO;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.enums.UserRole;
import com.example.javaoffer.user.exception.PasswordConfirmException;
import com.example.javaoffer.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import static com.example.javaoffer.common.constants.SecurityConstant.COOKIE_ACCESS_TOKEN;
import static com.example.javaoffer.common.constants.SecurityConstant.COOKIE_REFRESH_TOKEN;
import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_LOGIN;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_REGISTER;

/**
 * Контроллер для управления аутентификацией и регистрацией пользователей.
 * <p>
 * Обрабатывает запросы, связанные с:
 * <ul>
 *   <li>Входом пользователей в систему</li>
 *   <li>Регистрацией новых пользователей</li>
 *   <li>Выходом пользователей из системы</li>
 * </ul>
 * Использует JWT-токены для аутентификации пользователей и cookie для их хранения.
 * 
 *
 * @author Garbuzov Oleg
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

	private final UserService userService;

	/**
	 * Отображает страницу входа в систему.
	 * <p>
	 * Если пользователь уже аутентифицирован, перенаправляет на главную страницу.
	 * В противном случае отображает форму для входа в систему.
	 * 
	 *
	 * @param request HTTP-запрос для получения IP-адреса клиента
	 * @param model   модель для передачи данных в представление
	 * @return имя представления или строка перенаправления
	 */
	@GetMapping(URL_LOGIN)
	public String showLoginPage(HttpServletRequest request, Model model) {
		String clientIp = ClientUtils.getClientIp(request);
		model.addAttribute("request", request);
		boolean isAuthenticated = isAuthenticated();
		String returnAction = isAuthenticated ? "redirect:/" : VIEW_TEMPLATE_LOGIN;
		
		log.info("{}:GET {}: Запрос страницы входа. isAuthenticated={}, return={}",
				clientIp, URL_LOGIN, isAuthenticated, returnAction);
		
		return returnAction;
	}

	/**
	 * Отображает страницу регистрации нового пользователя.
	 * <p>
	 * Если пользователь уже аутентифицирован, перенаправляет на главную страницу.
	 * В противном случае отображает форму для регистрации нового пользователя.
	 * 
	 *
	 * @param request HTTP-запрос для получения IP-адреса клиента
	 * @param model   модель для передачи данных в представление
	 * @return имя представления или строка перенаправления
	 */
	@GetMapping(URL_REGISTER)
	public String showRegisterPage(HttpServletRequest request, Model model) {
		String clientIp = ClientUtils.getClientIp(request);
		model.addAttribute("request", request);
		boolean isAuthenticated = isAuthenticated();
		String returnAction = isAuthenticated ? "redirect:/" : VIEW_TEMPLATE_REGISTER;
		
		log.info("{}:GET {}: Запрос страницы регистрации. isAuthenticated={}, return={}",
				clientIp, URL_REGISTER, isAuthenticated, returnAction);
		
		return returnAction;
	}

	/**
	 * Обрабатывает запрос на регистрацию нового пользователя.
	 * <p>
	 * Выполняет следующие шаги:
	 * <ol>
	 *   <li>Проверяет совпадение пароля и его подтверждения</li>
	 *   <li>Создает новый объект пользователя с ролью ROLE_USER</li>
	 *   <li>Сохраняет пользователя в базе данных через UserService</li>
	 *   <li>Перенаправляет на страницу входа для аутентификации</li>
	 * </ol>
	 * 
	 *
	 * @param registrationRequestDTO данные для регистрации нового пользователя
	 * @param httpServletRequest     HTTP-запрос для получения IP-адреса клиента
	 * @return строка перенаправления на страницу входа
	 * @throws PasswordConfirmException если пароли не совпадают
	 */
	@PostMapping(URL_REGISTER)
	public String registration(@Valid @ModelAttribute RegistrationRequestDTO registrationRequestDTO,
							   HttpServletRequest httpServletRequest) {
		String clientIp = ClientUtils.getClientIp(httpServletRequest);
		String username = registrationRequestDTO.getUsername();
		
		log.info("{}:POST {}: Попытка регистрации пользователя {}", clientIp, URL_REGISTER, username);

		if (!registrationRequestDTO.getPassword().equals(registrationRequestDTO.getPasswordConfirm())) {
			log.warn("{}:POST {}: Пароли не совпадают для пользователя {}", clientIp, URL_REGISTER, username);
			throw new PasswordConfirmException("Пароли не совпадают");
		}

		try {
			User newUser = User.builder()
					.username(username)
					.email(registrationRequestDTO.getUserEmail())
					.password(registrationRequestDTO.getPassword())
					.role(UserRole.ROLE_USER)
					.build();

			userService.createUser(newUser);
			log.info("{}:POST {}: Пользователь {} успешно зарегистрирован", clientIp, URL_REGISTER, username);
		} catch (Exception e) {
			log.error("{}:POST {}: Ошибка при регистрации пользователя {}: {}", 
					clientIp, URL_REGISTER, username, e.getMessage());
			throw e;
		}

		return "redirect:" + URL_LOGIN;
	}

	/**
	 * Обрабатывает запрос на выход из системы.
	 * <p>
	 * Удаляет JWT-токены из куки браузера, устанавливая их значение в пустую строку
	 * и время жизни в 0. Это эффективно удаляет токены из браузера пользователя.
	 * 
	 *
	 * @param response           HTTP-ответ для удаления куки
	 * @param httpServletRequest HTTP-запрос для получения IP-адреса клиента
	 * @return строка перенаправления на главную страницу
	 */
	@PostMapping(URL_LOGOUT)
	public String logout(HttpServletResponse response, HttpServletRequest httpServletRequest) {
		String clientIp = ClientUtils.getClientIp(httpServletRequest);
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String userName = (auth != null && auth.getPrincipal() != null) ? 
				auth.getName() : "unknown";
		
		log.info("{}:POST {}: Выход пользователя {} из системы", clientIp, URL_LOGOUT, userName);

		try {
			Cookie accessTokenCookie = new Cookie(COOKIE_ACCESS_TOKEN, "");
			accessTokenCookie.setHttpOnly(true);
			accessTokenCookie.setPath("/");
			accessTokenCookie.setMaxAge(0);
			response.addCookie(accessTokenCookie);

			Cookie refreshTokenCookie = new Cookie(COOKIE_REFRESH_TOKEN, "");
			refreshTokenCookie.setHttpOnly(true);
			refreshTokenCookie.setPath("/");
			refreshTokenCookie.setMaxAge(0);
			response.addCookie(refreshTokenCookie);

			log.info("{}:POST {}: Пользователь {} успешно вышел из системы", clientIp, URL_LOGOUT, userName);
		} catch (Exception e) {
			log.error("{}:POST {}: Ошибка при выходе пользователя {} из системы: {}", 
					clientIp, URL_LOGOUT, userName, e.getMessage());
		}

		return "redirect:" + URL_ROOT;
	}

	/**
	 * Проверяет, аутентифицирован ли текущий пользователь.
	 * <p>
	 * Проверяет наличие объекта аутентификации в контексте безопасности
	 * и убеждается, что это не анонимный пользователь.
	 * 
	 *
	 * @return true, если пользователь аутентифицирован, иначе false
	 */
	private boolean isAuthenticated() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean authenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
		
		log.trace("Проверка аутентификации: auth={}, authenticated={}", 
				auth != null ? auth.getName() : "null", authenticated);
		
		return authenticated;
	}
} 