package com.example.javaoffer.exam.exception;

import java.io.Serial;

/**
 * Исключение, выбрасываемое при получении некорректного идентификатора запроса.
 * <p>
 * Возникает, когда система получает запрос с идентификатором, который
 * не соответствует ожидаемому формату, отсутствует в системе или
 * не принадлежит текущему пользователю.
 *
 * @author Garbuzov Oleg
 */
public class InvalidRequestIdException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор с сообщением об ошибке.
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public InvalidRequestIdException(String message) {
		super(message);
	}
}
