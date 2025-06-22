package com.example.javaoffer.exam.anticheat.exception;

/**
 * Исключение, которое выбрасывается при попытке преобразовать неизвестное значение в тип события античита.
 * <p>
 * Это исключение возникает, когда клиент отправляет запрос с типом события,
 * который не соответствует ни одному из определенных типов в {@link com.example.javaoffer.exam.anticheat.enums.EventType}.
 * 
 */
public class NoMatchFieldAntiCheatEventTypeException extends RuntimeException {
	
	/**
	 * Создает новый экземпляр исключения с указанным сообщением об ошибке.
	 *
	 * @param message сообщение об ошибке, обычно содержит неизвестное значение типа события
	 */
	public NoMatchFieldAntiCheatEventTypeException(String message) {
		super(message);
	}
}
