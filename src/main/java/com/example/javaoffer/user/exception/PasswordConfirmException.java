package com.example.javaoffer.user.exception;

/**
 * Исключение, выбрасываемое при несовпадении пароля и его подтверждения
 * в процессе регистрации или изменения пароля пользователя.
 * Гарантирует, что пользователь корректно ввел свой пароль.
 * 
 * @author Garbuzov Oleg
 */
public class PasswordConfirmException extends RuntimeException {
	/**
	 * Конструктор с сообщением об ошибке
	 * 
	 * @param message сообщение, описывающее причину исключения
	 */
	public PasswordConfirmException(String message) {
		super(message);
	}

}
