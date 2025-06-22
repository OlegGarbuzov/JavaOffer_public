package com.example.javaoffer.exam.exception;

import com.example.javaoffer.exam.enums.ExamMode;

import java.io.Serial;

/**
 * Исключение, выбрасываемое при отсутствии стратегии для выбранного режима экзамена.
 * <p>
 * Возникает, когда в системе не реализована стратегия для обработки экзамена
 * в запрошенном режиме ({@link ExamMode}) или произошла ошибка в выборе стратегии.
 * Это системная ошибка, требующая добавления соответствующей стратегии в код приложения.
 *
 * @author Garbuzov Oleg
 * @see com.example.javaoffer.exam.strategy.ExamModeStrategy
 * @see ExamMode
 */
public class NoStrategyForSelectModeException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор с сообщением об ошибке.
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public NoStrategyForSelectModeException(String message) {
		super(message);
	}
}
