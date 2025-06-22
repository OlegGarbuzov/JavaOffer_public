package com.example.javaoffer.exam.exception;

import java.io.Serial;

/**
 * Исключение, выбрасываемое, если запрошенный вопрос по идентификатору не найден в базе данных.
 * <p>
 * Может возникать, если передан некорректный идентификатор вопроса или вопрос с таким
 * идентификатором был удален из базы данных. Это может быть как ошибкой в запросе,
 * так и результатом несогласованности данных.
 *
 * @author Garbuzov Oleg
 */
public class QuestionNotFoundException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор по умолчанию.
	 */
	public QuestionNotFoundException() {
		super();
	}

	/**
	 * Конструктор с сообщением об ошибке.
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public QuestionNotFoundException(String message) {
		super(message);
	}
}
