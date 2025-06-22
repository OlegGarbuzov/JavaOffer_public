package com.example.javaoffer.common.exception.dto;
import java.time.LocalDateTime;

/**
 * DTO для представления информации об ошибке, возвращаемой пользователю.
 * <p>
 * Содержит следующие поля:
 * <ul>
 *   <li>timestamp - время возникновения ошибки</li>
 *   <li>status - HTTP-статус ошибки</li>
 *   <li>error - тип ошибки</li>
 *   <li>message - сообщение об ошибке</li>
 *   <li>path - путь запроса, вызвавшего ошибку</li>
 * </ul>
 * 
 * @param timestamp время возникновения ошибки
 * @param status HTTP-статус ошибки
 * @param error тип ошибки
 * @param message сообщение об ошибке
 * @param path путь запроса, вызвавшего ошибку
 */
public record ErrorResponse(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		String path
) {
	/**
	 * Создает новый экземпляр ErrorResponse с текущим временем.
	 *
	 * @param status  HTTP-статус ошибки
	 * @param error   тип ошибки
	 * @param message сообщение об ошибке
	 * @param path    путь запроса, вызвавшего ошибку
	 * @return новый экземпляр ErrorResponse
	 */
	public static ErrorResponse of(int status, String error, String message, String path) {
		return new ErrorResponse(LocalDateTime.now(), status, error, message, path);
	}
}
