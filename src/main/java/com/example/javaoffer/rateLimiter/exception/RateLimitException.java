package com.example.javaoffer.rateLimiter.exception;

/**
 * Исключение, выбрасываемое при превышении лимита запросов.
 * <p>
 * Используется в системе rate limiting для информирования о превышении
 * пользовательских или глобальных лимитов частоты вызовов методов,
 * аннотированных {@link com.example.javaoffer.rateLimiter.annotation.RateLimit}.
 * 
 * <p>
 * Это runtime исключение предназначено для обработки на уровне
 * глобального обработчика исключений приложения.
 * 
 *
 * @author Garbuzov Oleg
 */
public class RateLimitException extends RuntimeException {

	/**
	 * Создает новое исключение с указанным сообщением.
	 *
	 * @param message детальное сообщение об ошибке
	 */
	public RateLimitException(String message) {
		super(message);
	}

	/**
	 * Создает новое исключение с указанным сообщением и причиной.
	 *
	 * @param message детальное сообщение об ошибке
	 * @param cause   исключение-причина
	 */
	public RateLimitException(String message, Throwable cause) {
		super(message, cause);
	}
}
