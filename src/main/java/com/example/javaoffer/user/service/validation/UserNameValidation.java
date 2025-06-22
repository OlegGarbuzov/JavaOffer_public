package com.example.javaoffer.user.service.validation;

import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.exception.UserAlreadyExistsException;
import com.example.javaoffer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация имени пользователя на уникальность.
 * <p>
 * Проверяет, что пользователь с указанным именем пользователя (логином)
 * еще не зарегистрирован в системе.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserNameValidation implements UserValidation {

	private final UserRepository userRepository;

	/**
	 * Выполняет валидацию имени пользователя на уникальность.
	 * 
	 * @param requestUser пользователь для валидации
	 * @throws UserAlreadyExistsException если пользователь с таким именем уже существует
	 */
	@Override
	public void validation(User requestUser) {
		log.debug("Валидация имени пользователя для: {}", requestUser.getUsername());
		
		Optional<User> existingUser = userRepository.findByUsername(requestUser.getUsername());
		
		if (existingUser.isPresent()) {
			log.warn("Попытка регистрации пользователя с уже существующим именем: {}", requestUser.getUsername());
			throw new UserAlreadyExistsException("Пользователь с таким именем уже существует");
		}
		
		log.trace("Имя пользователя {} прошло валидацию на уникальность", requestUser.getUsername());
	}

}
