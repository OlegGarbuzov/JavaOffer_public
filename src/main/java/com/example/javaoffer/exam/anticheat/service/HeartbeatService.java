package com.example.javaoffer.exam.anticheat.service;

import com.example.javaoffer.exam.anticheat.config.AntiCheatProperties;
import com.example.javaoffer.exam.anticheat.dto.SessionStatusResponseDTO;
import com.example.javaoffer.exam.anticheat.util.EventUtils;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.exception.NoEntryInCacheException;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.google.common.util.concurrent.Striped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

/**
 * Сервис для обработки heartbeat-запросов и проверки активности сессии.
 * <p>
 * Отвечает за обработку периодических запросов от клиента, которые подтверждают
 * активность пользователя и целостность сессии экзамена. Отслеживает пропущенные
 * heartbeat-запросы и принимает решение о прерывании экзамена при превышении
 * допустимого количества нарушений.
 * 
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HeartbeatService {

	private final ExamSessionCacheService examSessionCacheService;
	private final HeartBeatTokenService heartBeatTokenService;
	private final Random random = new Random();
	private final Striped<Lock> examLocks = Striped.lock(10240);
	private final AntiCheatProperties antiCheatProperties;

	/**
	 * Обрабатывает heartbeat-запрос для проверки статуса сессии.
	 * <p>
	 * Проверяет валидность токена, отслеживает пропущенные запросы и
	 * генерирует новый токен для следующего запроса.
	 * 
	 *
	 * @param examId     идентификатор экзамена
	 * @param token      токен для проверки подлинности
	 * @param questionId ID текущего вопроса
	 * @return DTO с результатом проверки статуса и данными для следующего запроса
	 */
	public SessionStatusResponseDTO processHeartbeat(UUID examId, String token, Long questionId) {
		Lock lock = examLocks.get(examId);
		lock.lock();

		try {
			TemporaryExamProgress progress = getExamProgress(examId);

			// Генерируем новые данные для следующего запроса
			String newToken = heartBeatTokenService.generateToken(examId, questionId != null ? questionId : progress.getLastTaskId());
			int nextInterval = getRandomHeartbeatInterval();
			String challenge = EventUtils.generateChallenge(nextInterval);

			// Проверяем, является ли это инициализационным запросом с временным токеном
			// Так же надо убедиться, что это первый инициализационный запрос, иначе считаем ошибку
			if (token != null && (token.startsWith("init_"))) {
				return processInitialRequest(examId, progress, nextInterval, newToken, challenge);
			}

			// Проверяем, не прерван ли уже экзамен
			if (examSessionCacheService.isExamTerminatedByViolations(examId)) {
				log.warn("examId={}: Экзамен уже прерван из-за нарушений", examId);
				return new SessionStatusResponseDTO();
			}

			// Проверяем валидность токена
			if (!heartBeatTokenService.validateToken(token, progress.getLastSessionToken())) {
				log.warn("examId={}: Невалидный токен: {}", examId, token);

				// Увеличиваем счетчик пропущенных heartbeat если токен невалидный
				progress.setHeartbeatMissedCount(progress.getHeartbeatMissedCount() + 1);
				updateHeartBeatInProgress(progress, examId, nextInterval, newToken);

				return new SessionStatusResponseDTO(
						newToken,
						terminateExamIfLimitExceeded(progress, examId),
						challenge);
			}

			//Проверяем и фиксируем пропуски Heartbeat
			boolean isTerminated = checkMissedHeartbeatsAndUpdateProgress(examId, progress);

			if (isTerminated) {
				log.warn("examId={}: Экзамен будет прерван из-за пропущенных heartbeat-запросов", examId);
				return new SessionStatusResponseDTO(null, true,
						"Процесс прерван из-за многочисленных нарушений правил сервиса");
			}

			updateHeartBeatInProgress(progress, examId, nextInterval, newToken);

			return new SessionStatusResponseDTO(newToken, false, challenge);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Проверяет наличие долгого отсутствия heartbeat-запросов.
	 * <p>
	 * Вызывается при запросе ответа для проверки, не было ли длительного
	 * отсутствия активности пользователя.
	 * 
	 *
	 * @param progress прогресс экзамена
	 * @param examId   идентификатор экзамена
	 */
	public void heartBeatLongAbsenceCheck(TemporaryExamProgress progress, UUID examId) {
		Instant now = Instant.now();
		int toleranceMillis = 60000;

		// Проверяем, что бы последний heartBeat был не очень давно
		if (EventUtils.isHeartbeatMissed(progress, now, toleranceMillis)) {
			// Регистрируем пропуск heartbeat
			progress.setHeartbeatMissedCount(progress.getHeartbeatMissedCount() + 1);

			log.warn("examId={}: Обнаружено долгое отсутствие heartbeat при запросе ответа, текущее количество: {}, установленная толерантность:{}",
					examId, progress.getHeartbeatMissedCount(),
					toleranceMillis);

			// Сохраняем обновленный прогресс
			examSessionCacheService.save(examId, progress);
		}
	}

	/**
	 * Обрабатывает инициализационный heartbeat-запрос.
	 * <p>
	 * Инициализирует данные для отслеживания heartbeat-запросов и
	 * генерирует первый токен для клиента.
	 * 
	 *
	 * @param examId       идентификатор экзамена
	 * @param progress     прогресс экзамена
	 * @param nextInterval интервал до следующего запроса
	 * @param newToken     новый токен для следующего запроса
	 * @param challenge    задача для выполнения на клиенте
	 * @return DTO с результатом инициализации
	 */
	private SessionStatusResponseDTO processInitialRequest(
			UUID examId,
			TemporaryExamProgress progress,
			int nextInterval,
			String newToken,
			String challenge) {
		try {
			// Проверим, что это первый инициализационный запрос, иначе ошибка
			if (progress.getLastSessionToken() != null)
				progress.setHeartbeatMissedCount(progress.getHeartbeatMissedCount() + 1);

			// Обновляем данные о последнем heartbeat
			progress.setLastHeartbeatTime(Instant.now());
			progress.setNextExpectedHeartbeatTime(Instant.now().plusMillis(nextInterval));
			progress.setLastSessionToken(newToken);

			// Сохраняем обновленный прогресс
			examSessionCacheService.save(examId, progress);

			// Возвращаем ответ с новым токеном
			return new SessionStatusResponseDTO(newToken, false, challenge);
		} catch (NoEntryInCacheException e) {
			log.warn("examId={}: {}", examId, e.getMessage());
			return new SessionStatusResponseDTO(null, true, challenge);
		}
	}

	/**
	 * Генерирует случайный интервал для heartbeat-запросов.
	 * Диапазон случайного числа выбирается от MinHeartbeatInterval до MaxHeartbeatInterval,
	 * согласно настройкам heartbeat.
	 *
	 * @return интервал в миллисекундах
	 */
	private int getRandomHeartbeatInterval() {
		int min = antiCheatProperties.getMinHeartbeatInterval();
		int max = antiCheatProperties.getMaxHeartbeatInterval();

		if (max <= min) {
			throw new IllegalArgumentException("Max heartbeat interval must be greater than min");
		}

		return min + random.nextInt(max - min + 1);
	}

	/**
	 * Проверяет наличие пропущенных heartbeat-запросов и обновляет прогресс.
	 * <p>
	 * Проверяет, был ли пропущен предыдущий heartbeat-запрос, и
	 * прерывает экзамен при превышении лимита пропущенных запросов.
	 * 
	 *
	 * @param examId   идентификатор экзамена
	 * @param progress прогресс экзамена
	 * @return true, если экзамен был прерван из-за превышения лимита
	 */
	private boolean checkMissedHeartbeatsAndUpdateProgress(UUID examId, TemporaryExamProgress progress) {
		try {
			// Проверяем наличие пропущенных heartbeat
			if (checkForMissedHeartbeat(progress, examId)) {
				// Если превышен лимит пропущенных heartbeat, прерываем экзамен
				return terminateExamIfLimitExceeded(progress, examId);
			}

			return false;
		} catch (Exception e) {
			log.error("examId={}: Ошибка при проверке пропущенных heartbeat: {}", examId, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Получает прогресс экзамена из кэша.
	 *
	 * @param examId идентификатор экзамена
	 * @return объект с прогрессом экзамена
	 * @throws NoEntryInCacheException если прогресс не найден в кэше
	 */
	private TemporaryExamProgress getExamProgress(UUID examId) {
		return examSessionCacheService.get(examId).orElseThrow(() ->
				new NoEntryInCacheException("Прогресс не найден или устарел."));
	}

	/**
	 * Проверяет наличие пропущенного heartbeat-запроса.
	 * <p>
	 * Учитывает возможные проблемы с часовым поясом и
	 * увеличивает счетчик пропущенных запросов при необходимости.
	 * 
	 *
	 * @param progress прогресс экзамена
	 * @param examId   идентификатор экзамена
	 * @return true, если был пропущен heartbeat-запрос
	 */
	private boolean checkForMissedHeartbeat(TemporaryExamProgress progress, UUID examId) {
		Instant now = Instant.now();

		// Определяем, может ли быть проблема с часовым поясом
		boolean isPotentialTimezoneIssue = EventUtils.isPotentialTimezoneIssue(
				progress, now, antiCheatProperties.getMaxHeartbeatInterval());

		// Рассчитываем допустимое отклонение
		int toleranceMillis = EventUtils.calculateToleranceMillis(
				antiCheatProperties.getHeartbeatTolerance(), isPotentialTimezoneIssue);

		if (isPotentialTimezoneIssue) {
			log.debug("examId={}: Возможная проблема синхронизации времени, увеличена толерантность до {}мс",
					examId, toleranceMillis);
		}

		// Проверяем, был ли пропущен heartbeat
		if (EventUtils.isHeartbeatMissed(progress, now, toleranceMillis)) {
			// Регистрируем пропуск heartbeat
			progress.setHeartbeatMissedCount(progress.getHeartbeatMissedCount() + 1);

			log.warn("examId={}: Обнаружен пропущенный heartbeat при запросе следующего вопроса, текущее количество: {}{}",
					examId, progress.getHeartbeatMissedCount(),
					isPotentialTimezoneIssue ? " (возможна проблема с часовым поясом)" : "");

			// Сохраняем обновленный прогресс
			examSessionCacheService.save(examId, progress);

			return true;
		}

		return false;
	}

	/**
	 * Проверяет, превышен ли лимит пропущенных heartbeat, и прерывает экзамен при необходимости.
	 *
	 * @param progress прогресс экзамена
	 * @param examId   идентификатор экзамена
	 * @return true, если экзамен прерван из-за превышения лимита
	 */
	private boolean terminateExamIfLimitExceeded(TemporaryExamProgress progress, UUID examId) {
		if (EventUtils.isHeartbeatLimitExceeded(
				progress.getHeartbeatMissedCount(), antiCheatProperties.getMaxHeartbeatMissed())) {

			log.warn("examId={}: Превышен лимит пропущенных heartbeat-запросов ({}), экзамен будет прерван",
					examId, antiCheatProperties.getMaxHeartbeatMissed());

			// Устанавливаем флаг прерывания экзамена из-за нарушений
			progress.setTerminatedByViolations(true);
			examSessionCacheService.save(examId, progress);

			return true;
		}

		return false;
	}

	/**
	 * Обновляет данные о последнем heartbeat в прогрессе экзамена.
	 *
	 * @param progress     прогресс экзамена
	 * @param examId       идентификатор экзамена
	 * @param nextInterval интервал до следующего запроса
	 * @param newToken     новый токен для следующего запроса
	 */
	private void updateHeartBeatInProgress(
			TemporaryExamProgress progress,
			UUID examId,
			int nextInterval,
			String newToken) {
		// Обновляем данные о последнем heartbeat
		progress.setLastHeartbeatTime(Instant.now());
		progress.setNextExpectedHeartbeatTime(Instant.now().plusMillis(nextInterval));
		progress.setLastSessionToken(newToken);

		// Сохраняем обновленный прогресс
		examSessionCacheService.save(examId, progress);
	}

} 