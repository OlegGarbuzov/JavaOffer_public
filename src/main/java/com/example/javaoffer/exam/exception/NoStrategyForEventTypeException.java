package com.example.javaoffer.exam.exception;

import java.io.Serial;

/**
 * Исключение, выбрасываемое при отсутствии стратегии для обработки определенного типа событий античита.
 * <p>
 * Возникает, когда в системе не реализована стратегия для обработки события античита
 * определенного типа или произошла ошибка в выборе стратегии или на сервер передан не приавльный тип события.
 * Это системная ошибка,
 *
 * @author Garbuzov Oleg
 * @see com.example.javaoffer.exam.strategy.ExamModeStrategy
 */
public class NoStrategyForEventTypeException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор с сообщением об ошибке.
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public NoStrategyForEventTypeException(String message) {
		super(message);
	}
}
