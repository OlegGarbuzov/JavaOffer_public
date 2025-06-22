package com.example.javaoffer.exam.exception;

import java.io.Serial;

/**
 * Исключение, выбрасываемое при отсутствии выбранного ответа в запросе.
 * <p>
 * Возникает, когда пользователь не выбрал ни один из вариантов ответа
 * на вопрос или произошла ошибка в передаче выбранного ответа. Это может
 * быть как ошибкой пользовательского интерфейса, так и попыткой отправить
 * некорректный запрос.
 *
 * @author Garbuzov Oleg
 */
public class NoSelectedAnswerInRequestException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор с сообщением об ошибке.
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public NoSelectedAnswerInRequestException(String message) {
		super(message);
	}
}
