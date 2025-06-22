package com.example.javaoffer.exam.exception;

import java.io.Serial;

/**
 * Исключение, выбрасываемое при отсутствии вопросов (заданий) для выбранных параметров экзамена.
 * <p>
 * Может возникать, если в базе данных нет заданий с указанной сложностью, темой или грейдом.
 * Это может быть как ошибкой конфигурации, так и результатом слишком узких критериев поиска
 * вопросов.
 *
 * @author Garbuzov Oleg
 */
public class NoQuestionsException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор с сообщением об ошибке
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public NoQuestionsException(String message) {
		super(message);
	}

}
