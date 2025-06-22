package com.example.javaoffer.user.exception;

/**
 * Исключение, выбрасываемое при попытке получить пользователя, который не существует в системе.
 * Может возникать при поиске пользователя по id, имени пользователя или другим идентификаторам.
 * 
 * @author Garbuzov Oleg
 */
public class UserNotFoundException extends RuntimeException {
	/**
	 * Конструктор с сообщением об ошибке
	 * 
	 * @param message сообщение, описывающее причину исключения
	 */
	public UserNotFoundException(String message) {
		super(message);
	}

}
