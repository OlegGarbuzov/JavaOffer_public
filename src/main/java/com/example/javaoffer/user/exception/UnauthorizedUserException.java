package com.example.javaoffer.user.exception;

/**
 * Исключение, выбрасываемое в случае, если действие требует авторизованного пользователя,
 * но текущий пользователь не прошёл аутентификацию (например, обращение от анонимного клиента).
 * <p>
 * Обычно приводит к возврату HTTP статуса {@code 401 Unauthorized}.
 * 
 * 
 * @author Garbuzov Oleg
 */
public class UnauthorizedUserException extends RuntimeException {

	/**
	 * Создаёт новое исключение с указанным сообщением.
	 *
	 * @param message описание причины, по которой пользователь считается неавторизованным
	 */
	public UnauthorizedUserException(String message) {
		super(message);
	}
}
