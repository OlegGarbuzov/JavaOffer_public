package com.example.javaoffer.security.service;

import com.example.javaoffer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Пользовательская реализация UserDetailsService для загрузки данных пользователей.
 * <p>
 * Этот сервис взаимодействует с репозиторием пользователей для загрузки данных
 * по имени пользователя. Он используется Spring Security для аутентификации
 * пользователей и получения их ролей и разрешений.
 * </p>
 * Аннотация @Primary указывает Spring использовать этот сервис как основной
 * для интерфейса UserDetailsService.
 *
 * @author Garbuzov Oleg
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	/**
	 * Загружает данные пользователя по имени пользователя.
	 * <p>
	 * Выполняет поиск пользователя в базе данных по имени пользователя.
	 * Если пользователь найден, возвращает объект UserDetails с его данными.
	 * В противном случае выбрасывает исключение UsernameNotFoundException.
	 *
	 * @param username имя пользователя для поиска
	 * @return объект UserDetails с данными найденного пользователя
	 * @throws UsernameNotFoundException если пользователь с указанным именем не найден
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.debug("Поиск пользователя с именем: {}", username);

		return userRepository.findByUsername(username)
				.map(user -> {
					log.trace("Пользователь {} найден в базе данных", username);
					return user;
				})
				.orElseThrow(() -> {
					log.warn("Пользователь с именем {} не найден", username);
					return new UsernameNotFoundException("Пользователь не найден: " + username);
				});
	}
} 