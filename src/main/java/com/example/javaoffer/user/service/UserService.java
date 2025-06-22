package com.example.javaoffer.user.service;

import com.example.javaoffer.user.dto.UserDTO;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.enums.UserRole;
import com.example.javaoffer.user.repository.UserRepository;
import com.example.javaoffer.user.service.validation.UserValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для управления пользователями в системе.
 * <p>
 * Предоставляет методы для создания, поиска и обновления пользователей,
 * а также для работы с их незавершенными экзаменами.
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final List<UserValidation> userValidationList;

	/**
	 * Создает нового пользователя в системе.
	 * <p>
	 * Выполняет валидацию пользователя с помощью всех зарегистрированных валидаторов,
	 * шифрует пароль и сохраняет пользователя в базе данных.
	 *
	 * @param user данные нового пользователя
	 * @return созданный пользователь с заполненным идентификатором
	 */
	@Transactional
	public User createUser(User user) {
		log.info("Создание нового пользователя: {}", user.getUsername());

		try {
			userValidationList.forEach(validator -> {
				log.debug("Применение валидации {} для пользователя {}",
						validator.getClass().getSimpleName(), user.getUsername());
				validator.validation(user);
			});

			User newUser = User.builder()
					.username(user.getUsername())
					.email(user.getEmail())
					.password(passwordEncoder.encode(user.getPassword()))
					.role(user.getRole())
					.accountNonLocked(true)
					.build();

			User savedUser = userRepository.save(newUser);
			log.info("Пользователь {} успешно создан с id: {}", savedUser.getUsername(), savedUser.getId());
			return savedUser;

		} catch (Exception e) {
			log.error("Ошибка при создании пользователя {}: {}", user.getUsername(), e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Устанавливает незавершенный экзамен для пользователя.
	 *
	 * @param user   пользователь
	 * @param examId идентификатор экзамена
	 */
	@Transactional
	public void setUnfinishedExam(User user, UUID examId) {
		log.debug("Установка незавершенного экзамена {} для пользователя {}:{}",
				examId, user.getUsername(), user.getId());

		try {
			user.setUnfinishedExamId(examId);
			userRepository.save(user);
			log.info("Незавершенный экзамен {} установлен для пользователя {}", examId, user.getUsername());

		} catch (Exception e) {
			log.error("Ошибка при установке незавершенного экзамена {} для пользователя {}: {}",
					examId, user.getUsername(), e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Очищает информацию о незавершенном экзамене пользователя.
	 *
	 * @param user пользователь
	 */
	@Transactional
	public void clearUnfinishedExam(User user) {
		log.debug("Очистка незавершенного экзамена для пользователя {}:{}",
				user.getUsername(), user.getId());

		try {
			user.setUnfinishedExamId(null);
			userRepository.save(user);
			log.info("Незавершенный экзамен очищен для пользователя {}", user.getUsername());

		} catch (Exception e) {
			log.error("Ошибка при очистке незавершенного экзамена для пользователя {}: {}",
					user.getUsername(), e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Находит пользователя по имени пользователя (логину).
	 *
	 * @param userName имя пользователя для поиска
	 * @return Optional с пользователем или пустой, если не найден
	 */
	@Transactional(readOnly = true)
	public Optional<User> findByUserName(String userName) {
		log.debug("Поиск пользователя по имени: {}", userName);

		try {
			Optional<User> user = userRepository.findByUsername(userName);

			if (user.isPresent()) {
				log.trace("Пользователь с именем {} найден, id: {}", userName, user.get().getId());
			} else {
				log.debug("Пользователь с именем {} не найден", userName);
			}

			return user;

		} catch (Exception e) {
			log.error("Ошибка при поиске пользователя по имени {}: {}", userName, e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Сохраняет пользователя в базе данных.
	 *
	 * @param user пользователь для сохранения
	 * @return сохраненный пользователь
	 */
	@Transactional
	public User save(User user) {
		log.info("Сохранение пользователя: {}", user.getUsername());

		try {
			User savedUser = userRepository.save(user);
			log.debug("Пользователь {} успешно сохранен", user.getUsername());
			return savedUser;

		} catch (Exception e) {
			log.error("Ошибка при сохранении пользователя {}: {}", user.getUsername(), e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Получает страницу пользователей с фильтрацией и пагинацией.
	 *
	 * @param email            фильтр по email (может быть null)
	 * @param username         фильтр по имени пользователя (может быть null)
	 * @param role             фильтр по роли (может быть null)
	 * @param accountNonLocked фильтр по статусу блокировки (может быть null)
	 * @param id               фильтр по идентификатору (может быть null)
	 * @param pageable         параметры пагинации
	 * @return страница пользователей в формате DTO
	 */
	@Transactional(readOnly = true)
	public Page<UserDTO> getUsers(String email, String username, String role, Boolean accountNonLocked, Long id, Pageable pageable) {
		log.debug("Получение пользователей с фильтрами: email={}, username={}, role={}, accountNonLocked={}, id={}",
				email, username, role, accountNonLocked, id);

		try {
			Page<UserDTO> result;

			if (id != null) {
				log.trace("Поиск пользователей по id: {}", id);
				result = userRepository.findAllById(id, pageable).map(this::toUserDTO);
			} else if (accountNonLocked != null) {
				log.trace("Поиск пользователей по статусу блокировки: {}", accountNonLocked);
				result = userRepository.findAllByAccountNonLocked(accountNonLocked, pageable).map(this::toUserDTO);
			} else if (role != null && !role.isEmpty()) {
				log.trace("Поиск пользователей с фильтрацией по роли: {}", role);
				UserRole roleEnum = UserRole.valueOf(role);
				result = userRepository.findAllByEmailContainingIgnoreCaseAndUsernameContainingIgnoreCaseAndRole(
						email == null ? "" : email,
						username == null ? "" : username,
						roleEnum,
						pageable
				).map(this::toUserDTO);
			} else {
				log.trace("Поиск пользователей с базовыми фильтрами");
				result = userRepository.findAllByEmailContainingIgnoreCaseAndUsernameContainingIgnoreCase(
						email == null ? "" : email,
						username == null ? "" : username,
						pageable
				).map(this::toUserDTO);
			}

			log.debug("Найдено {} пользователей на странице {} из {} общих элементов",
					result.getNumberOfElements(), result.getNumber(), result.getTotalElements());

			return result;

		} catch (Exception e) {
			log.error("Ошибка при получении пользователей: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Получает DTO пользователя по идентификатору.
	 *
	 * @param id идентификатор пользователя
	 * @return DTO пользователя или null, если не найден
	 */
	@Transactional(readOnly = true)
	public UserDTO getUserDTOById(Long id) {
		log.debug("Получение DTO пользователя по id: {}", id);

		try {
			UserDTO result = userRepository.findById(id).map(this::toUserDTO).orElse(null);

			if (result != null) {
				log.trace("DTO пользователя с id {} найден: {}", id, result.getUsername());
			} else {
				log.debug("Пользователь с id {} не найден", id);
			}

			return result;

		} catch (Exception e) {
			log.error("Ошибка при получении DTO пользователя по id {}: {}", id, e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Преобразует сущность пользователя в DTO.
	 *
	 * @param user сущность пользователя
	 * @return DTO пользователя
	 */
	private UserDTO toUserDTO(User user) {
		log.trace("Преобразование пользователя {} в DTO", user.getUsername());

		return UserDTO.builder()
				.id(user.getId())
				.email(user.getEmail())
				.username(user.getUsername())
				.role(user.getRole().name())
				.accountNonLocked(user.isAccountNonLocked())
				.userScoreHistoriesCount(user.getUserScoreHistories() != null ? user.getUserScoreHistories().size() : 0)
				.build();
	}
}