package com.example.javaoffer.common.constants;

/**
 * Константы безопасности и авторизации.
 * <p>
 * Содержит имена cookie, заголовков и других параметров,
 * используемых при аутентификации и авторизации пользователей.
 * 
 */
public class SecurityConstant {

	/**
	 * Имя cookie для access токена
	 */
	public static final String COOKIE_ACCESS_TOKEN = "access_token";

	/**
	 * Имя cookie для refresh токена
	 */
	public static final String COOKIE_REFRESH_TOKEN = "refresh_token";

	/**
	 * Имя cookie для XSRF токена
	 */
	public static final String COOKIE_XSRF_TOKEN = "X-XSRF-TOKEN";

	/**
	 * Префикс заголовка авторизации
	 */
	public static final String BEARER_PREFIX = "Bearer ";

	/**
	 * Заголовок авторизации
	 */
	public static final String AUTHORIZATION_HEADER = "Authorization";

	/**
	 * Заголовок refresh-токена
	 */
	public static final String REFRESH_TOKEN_HEADER = "Refresh-Token";

	/**
	 * Название параметра csrf токена
	 */
	public static final String CSRF_PARAMETER_NAME = "_csrf";
}
