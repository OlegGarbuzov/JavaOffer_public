package com.example.javaoffer.exam.exception;

import java.io.Serial;

/**
 * Исключение, выбрасываемое при отсутствии корректных ответов в задании.
 * <p>
 * Может возникать, если в базе данных не указан ни один корректный ответ
 * для задания, а проверка перед выдачей вопроса не сработала. Это критическая
 * ошибка, которая требует исправления данных в системе.
 *
 * @author Garbuzov Oleg
 */
public class NoCorrectAnswerByTaskException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор с сообщением об ошибке.
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public NoCorrectAnswerByTaskException(String message) {
		super(message);
	}
}
