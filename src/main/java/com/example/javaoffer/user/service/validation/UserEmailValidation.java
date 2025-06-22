package com.example.javaoffer.user.service.validation;

import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.exception.UserAlreadyExistsException;
import com.example.javaoffer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация email-адреса пользователя на уникальность.
 * <p>
 * Проверяет, что пользователь с указанным email-адресом
 * еще не зарегистрирован в системе.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEmailValidation implements UserValidation {

	private final UserRepository userRepository;

	/**
	 * Выполняет валидацию email-адреса пользователя на уникальность.
	 * 
	 * @param requestUser пользователь для валидации
	 * @throws UserAlreadyExistsException если пользователь с таким email уже существует
	 */
	@Override
	public void validation(User requestUser) {
		log.debug("Валидация email для пользователя: {}", requestUser.getEmail());
		
		Optional<User> existingUser = userRepository.findByEmail(requestUser.getEmail());
		
		if (existingUser.isPresent()) {
			log.warn("Попытка регистрации пользователя с уже существующим email: {}", requestUser.getEmail());
			throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
		}
		
		log.trace("Email {} прошел валидацию на уникальность", requestUser.getEmail());
	}

}
