package com.example.javaoffer.exam.service;

import com.example.javaoffer.exam.dto.UserAnswerDTO;
import com.example.javaoffer.exam.dto.UserScoreHistoryDTO;
import com.example.javaoffer.exam.entity.UserScoreHistory;
import com.example.javaoffer.exam.repository.UserScoreHistoryRepository;
import com.example.javaoffer.user.dto.UserSimpleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.javaoffer.common.constants.JpaCacheName.CACHE_NAME_GLOBAL_RATING;
import static com.example.javaoffer.common.constants.JpaCacheName.CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE;

/**
 * Сервис для работы с историей прохождения экзаменов пользователями.
 * <p>
 * Предоставляет методы для сохранения, получения и управления результатами
 * экзаменов пользователей. Поддерживает фильтрацию и пагинацию результатов,
 * а также конвертацию между DTO и сущностями.
 *
 * <p>
 * Сервис интегрирован с системой кэширования для оптимизации доступа
 * к часто запрашиваемым данным о рейтингах пользователей.
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserScoreHistoryService {
	private final UserScoreHistoryRepository userScoreHistoryRepository;
	private final TaskService taskService;
	private final AnswerService answerService;

	/**
	 * Сохраняет историю прохождения экзамена пользователем.
	 * <p>
	 * Метод используется для сохранения результатов экзамена после его завершения.
	 *
	 * @param userScoreHistory объект с историей прохождения экзамена
	 */
	@Transactional
	@Caching(evict = {
			@CacheEvict(cacheNames = CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE, allEntries = true),
			@CacheEvict(cacheNames = CACHE_NAME_GLOBAL_RATING, allEntries = true)
	})
	public void save(UserScoreHistory userScoreHistory) {
		log.debug("Сохранение истории экзамена для пользователя: {}, examId: {}",
				userScoreHistory.getUser() != null ? userScoreHistory.getUser().getUsername() : "anonymous",
				userScoreHistory.getExamID());
		userScoreHistoryRepository.save(userScoreHistory);
		log.trace("История экзамена успешно сохранена с id: {}", userScoreHistory.getId());
	}

	/**
	 * Конвертирует сущность в DTO для передачи клиенту.
	 * <p>
	 * Преобразует объект сущности UserScoreHistory в объект DTO с необходимыми
	 * для клиента данными, включая информацию о пользователе и его ответах.
	 *
	 * @param userScoreHistory сущность истории прохождения экзамена
	 * @return объект DTO с данными истории экзамена
	 */
	private UserScoreHistoryDTO convertToDTO(UserScoreHistory userScoreHistory) {
		log.trace("Конвертация сущности UserScoreHistory с id: {} в DTO", userScoreHistory.getId());
		UserScoreHistoryDTO dto = UserScoreHistoryDTO.builder()
				.createAt(userScoreHistory.getCreateAt())
				.examID(userScoreHistory.getExamID())
				.successAnswersCountAbsolute(userScoreHistory.getSuccessAnswersCountAbsolute())
				.failAnswersCountAbsolute(userScoreHistory.getFailAnswersCountAbsolute())
				.score(userScoreHistory.getScore())
				.timeTakenToComplete(userScoreHistory.getTimeTakenToComplete())
				.bonusByTime(userScoreHistory.getBonusByTime())
				.totalBasePoints(userScoreHistory.getTotalBasePoints())
				.id(userScoreHistory.getId())
				.tabSwitchViolations(userScoreHistory.getTabSwitchViolations())
				.textCopyViolations(userScoreHistory.getTextCopyViolations())
				.heartbeatMissedViolations(userScoreHistory.getHeartbeatMissedViolations())
				.devToolsViolations(userScoreHistory.getDevToolsViolations())
				.domTamperingViolations(userScoreHistory.getDomTamperingViolations())
				.functionTamperingViolations(userScoreHistory.getFunctionTamperingViolations())
				.moduleTamperingViolations(userScoreHistory.getModuleTamperingViolations())
				.pageCloseViolations(userScoreHistory.getPageCloseViolations())
				.externalContentViolations(userScoreHistory.getExternalContentViolations())
				.terminatedByViolations(userScoreHistory.getTerminatedByViolations())
				.terminatedByFailAnswerCount(userScoreHistory.getTerminatedByFailAnswerCount())
				.terminationReason(userScoreHistory.getTerminationReason())
				.antiOcrTamperingViolations(userScoreHistory.getAntiOcrTamperingViolations())
				.userAnswers(userScoreHistory.getUserAnswers().stream()
						.map(userAnswer -> UserAnswerDTO.builder()
								.answerDTO(answerService.convertToDTO(userAnswer.getAnswer()))
								.taskDTO(taskService.convertToDTO(userAnswer.getTask()))
								.isCorrect(userAnswer.isCorrect())
								.timeTakenSeconds(userAnswer.getTimeTakenSeconds())
								.id(userAnswer.getId())
								.build())
						.collect(Collectors.toList()))
				.build();

		// Добавляем информацию о пользователе, если он существует
		if (userScoreHistory.getUser() != null) {
			dto.setUser(UserSimpleDTO.builder()
					.id(userScoreHistory.getUser().getId())
					.username(userScoreHistory.getUser().getUsername())
					.build());
		}

		return dto;
	}

	/**
	 * Находит все истории экзаменов для указанного пользователя.
	 * <p>
	 * Метод возвращает полный список историй прохождения экзаменов
	 * для пользователя с указанным идентификатором.
	 *
	 * @param userId идентификатор пользователя
	 * @return список DTO с историями экзаменов пользователя
	 */
	@Transactional(readOnly = true)
	public List<UserScoreHistoryDTO> findByUserId(Long userId) {
		log.debug("Поиск историй экзаменов для пользователя с id: {}", userId);
		List<UserScoreHistory> histories = userScoreHistoryRepository.findByUserId(userId);
		log.trace("Найдено {} историй экзаменов для пользователя с id: {}", histories.size(), userId);
		return histories.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	/**
	 * Находит истории экзаменов для указанного пользователя с пагинацией.
	 * <p>
	 * Метод возвращает страницу историй прохождения экзаменов для пользователя
	 * с указанным идентификатором. Опционально может фильтровать по идентификатору экзамена.
	 *
	 * @param userId   идентификатор пользователя
	 * @param examId   идентификатор экзамена для фильтрации (может быть null)
	 * @param pageable параметры пагинации и сортировки
	 * @return страница с историями экзаменов пользователя
	 */
	@Transactional(readOnly = true)
	public Page<UserScoreHistoryDTO> findByUserIdPaged(Long userId, String examId, Pageable pageable) {
		log.debug("Поиск историй экзаменов для пользователя с id: {}, examId: {}, page: {}",
				userId, examId, pageable.getPageNumber());

		if (examId != null && !examId.isEmpty()) {
			try {
				UUID uuid = UUID.fromString(examId);
				log.trace("Выполняется поиск с фильтром по examId: {}", uuid);
				return userScoreHistoryRepository.findByUserIdAndExamID(userId, uuid, pageable)
						.map(this::convertToDTO);
			} catch (IllegalArgumentException e) {
				log.warn("Некорректный формат examId: {}", examId, e);
				return Page.empty(pageable);
			}
		} else {
			log.trace("Выполняется поиск без фильтра по examId");
			return userScoreHistoryRepository.findByUserId(userId, pageable)
					.map(this::convertToDTO);
		}
	}

	/**
	 * Находит все истории экзаменов с применением фильтров.
	 * <p>
	 * Метод возвращает страницу историй прохождения экзаменов с возможностью
	 * фильтрации по различным критериям: наличие нарушений, идентификатор экзамена,
	 * имя пользователя.
	 *
	 * @param pageable           параметры пагинации и сортировки
	 * @param showViolationsOnly показывать только записи с нарушениями
	 * @param examId             фильтр по идентификатору экзамена (может быть null)
	 * @param userName           фильтр по имени пользователя (может быть null)
	 * @return страница с отфильтрованными историями экзаменов
	 */
	@Transactional(readOnly = true)
	public Page<UserScoreHistoryDTO> findAllWithFilters(
			Pageable pageable,
			Boolean showViolationsOnly,
			String examId,
			String userName) {

		log.debug("Поиск историй экзаменов с фильтрами: showViolationsOnly={}, examId={}, userName={}",
				showViolationsOnly, examId, userName);

		// Обработка examId - проверяем, что это валидный UUID, если передан
		UUID examUuid = null;
		if (examId != null && !examId.trim().isEmpty()) {
			try {
				examUuid = UUID.fromString(examId.trim());
				log.trace("Преобразован examId в UUID: {}", examUuid);
			} catch (IllegalArgumentException e) {
				log.warn("Некорректный формат examId: {}", examId, e);
				return Page.empty(pageable);
			}
		}

		// Обработка userName - убираем лишние пробелы и формируем паттерн для LIKE
		String userNamePattern = null;
		if (userName != null && !userName.trim().isEmpty()) {
			userNamePattern = "%" + userName.trim() + "%";
			log.trace("Сформирован паттерн для поиска по имени пользователя: {}", userNamePattern);
		}

		Page<UserScoreHistory> result = userScoreHistoryRepository.findAllWithFilters(
				pageable, showViolationsOnly, examUuid, userNamePattern);

		log.trace("Найдено {} историй экзаменов, соответствующих фильтрам", result.getTotalElements());
		return result.map(this::convertToDTO);
	}

	/**
	 * Находит историю экзамена по идентификатору.
	 * <p>
	 * Метод возвращает детальную информацию о конкретной истории
	 * прохождения экзамена по её идентификатору.
	 *
	 * @param id идентификатор истории экзамена
	 * @return DTO с данными истории экзамена
	 * @throws RuntimeException если история с указанным идентификатором не найдена
	 */
	@Transactional(readOnly = true)
	public UserScoreHistoryDTO findById(Long id) {
		log.debug("Поиск истории экзамена по id: {}", id);
		return userScoreHistoryRepository.findById(id)
				.map(this::convertToDTO)
				.orElseThrow(() -> {
					log.warn("История экзамена с ID {} не найдена", id);
					return new RuntimeException("История экзамена с ID " + id + " не найдена");
				});
	}

	/**
	 * Удаляет историю экзамена по идентификатору.
	 * <p>
	 * Метод удаляет запись об истории прохождения экзамена и очищает
	 * связанные кэши для обновления глобальных рейтингов.
	 *
	 * @param id идентификатор истории экзамена для удаления
	 */
	@Transactional
	@Caching(evict = {
			@CacheEvict(cacheNames = CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE, allEntries = true),
			@CacheEvict(cacheNames = CACHE_NAME_GLOBAL_RATING, allEntries = true)
	})
	public void deleteById(Long id) {
		log.debug("Удаление истории экзамена с id: {}", id);
		userScoreHistoryRepository.deleteById(id);
		log.info("История экзамена с id: {} успешно удалена", id);
	}
}
