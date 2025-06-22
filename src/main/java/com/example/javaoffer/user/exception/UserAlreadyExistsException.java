package com.example.javaoffer.user.exception;

/**
 * Исключение, выбрасываемое при попытке создать пользователя с логином или email,
 * которые уже зарегистрированы в системе.
 * Предотвращает дублирование учетных записей пользователей.
 * 
 * @author Garbuzov Oleg
 */
public class UserAlreadyExistsException extends RuntimeException {
	/**
	 * Конструктор с сообщением об ошибке
	 * 
	 * @param message сообщение, описывающее причину исключения
	 */
	public UserAlreadyExistsException(String message) {
		super(message);
	}

}
