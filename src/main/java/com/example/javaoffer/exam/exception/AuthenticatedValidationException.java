package com.example.javaoffer.exam.exception;

import java.io.Serial;

/**
 * Исключение, выбрасываемое при валидации старта экзамена,
 * если пользователь не аутентифицирован.
 * <p>
 * Возникает в процессе проверки прав доступа к экзамену, когда
 * система обнаруживает, что запрос на начало экзамена поступил
 * от неаутентифицированного пользователя.
 *
 * @author Garbuzov Oleg
 */
public class AuthenticatedValidationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор с сообщением об ошибке.
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public AuthenticatedValidationException(String message) {
		super(message);
	}
}
