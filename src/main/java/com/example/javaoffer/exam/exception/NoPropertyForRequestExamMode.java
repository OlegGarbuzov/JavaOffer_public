package com.example.javaoffer.exam.exception;

import com.example.javaoffer.exam.enums.ExamMode;

import java.io.Serial;

/**
 * Исключение, выбрасываемое, когда не удается найти свойства (properties),
 * соответствующие переданному режиму экзамена.
 * <p>
 * Может возникать, если передан некорректный {@link ExamMode} или соответствующие
 * properties отсутствуют в конфигурации проекта. Это системная ошибка, требующая
 * проверки конфигурации приложения.
 *
 * @author Garbuzov Oleg
 * @see ExamMode
 */
public class NoPropertyForRequestExamMode extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Конструктор по умолчанию.
	 */
	public NoPropertyForRequestExamMode() {
		super();
	}

	/**
	 * Конструктор с сообщением об ошибке.
	 *
	 * @param message сообщение, описывающее причину исключения
	 */
	public NoPropertyForRequestExamMode(String message) {
		super(message);
	}
}
