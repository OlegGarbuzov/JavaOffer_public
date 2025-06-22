package com.example.javaoffer.user.service.validation;

import com.example.javaoffer.user.entity.User;

/**
 * Интерфейс для валидации пользователей.
 * <p>
 * Определяет контракт для различных видов валидации пользователей
 * перед их созданием или изменением в системе.
 * 
 * 
 * @author Garbuzov Oleg
 */
public interface UserValidation {
	/**
	 * Выполняет валидацию пользователя.
	 * 
	 * @param user пользователь для валидации
	 * @throws RuntimeException если валидация не пройдена
	 */
	void validation(User user);
}
