package com.example.javaoffer.exam.cache.service;

import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.dto.TemporaryExamProgressDTO;
import com.example.javaoffer.exam.dto.ExamAbortResponseDTO;
import com.example.javaoffer.exam.dto.ExamNextQuestionRequestDTO;
import com.example.javaoffer.exam.enums.ExamDifficulty;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.Striped;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

/**
 * Сервис для управления кэшем прогресса экзаменационных сессий.
 * <p>
 * Обеспечивает потокобезопасное хранение, получение и удаление временного прогресса 
 * экзаменов с использованием Caffeine кэша и механизма тонкой блокировки (fine-grained locking).
 * Каждый прогресс экзамена идентифицируется по уникальному UUID и имеет ограниченное
 * время жизни в кэше.
 * 
 * <p>
 * Сервис использует Striped Lock для обеспечения потокобезопасности при конкурентном 
 * доступе к одному и тому же экзамену, что позволяет минимизировать блокировки 
 * при работе с разными экзаменами.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Service
@Slf4j
public class ExamSessionCacheService {

	private final Cache<UUID, TemporaryExamProgress> cache;
	private final Striped<Lock> locks = Striped.lock(10240);

	/**
	 * Создает новый экземпляр сервиса кэша прогресса экзаменов.
	 * 
	 * @param cache настроенный кэш для хранения прогресса экзаменов
	 */
	public ExamSessionCacheService(Cache<UUID, TemporaryExamProgress> cache) {
		this.cache = cache;
		log.info("ExamSessionCacheService инициализирован");
	}

	/**
	 * Сохраняет прогресс экзамена в кэше.
	 * <p>
	 * Использует блокировку для обеспечения корректной работы при конкурентном доступе.
	 * 
	 *
	 * @param examId идентификатор экзамена
	 * @param temporaryExamProgress объект с данными о прогрессе экзамена
	 * @throws IllegalArgumentException если объект прогресса равен null
	 */
	public void save(UUID examId, TemporaryExamProgress temporaryExamProgress) {
		if (temporaryExamProgress == null) {
			log.error("Попытка сохранить null в кэш прогресса для examId={}", examId);
			throw new IllegalArgumentException("temporaryExamProgress не может быть null");
		}
		
		Lock lock = locks.get(examId);
		lock.lock();
		try {
			cache.put(examId, temporaryExamProgress);
			log.debug("Сохранён прогресс для examId={}, режим={}", examId, temporaryExamProgress.getExamMode());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Создает новый прогресс экзамена и сохраняет его в кэше.
	 * <p>
	 * Инициализирует объект временного прогресса с начальными значениями и сохраняет
	 * его в кэше с указанным идентификатором. Использует блокировку для обеспечения
	 * корректной работы при конкурентном доступе.
	 * 
	 *
	 * @param examRequestDTO запрос на создание экзамена, содержащий идентификаторы
	 * @param initialExamDifficulty начальная сложность экзамена
	 * @param examMode режим экзамена
	 */
	public void createNewProgress(ExamNextQuestionRequestDTO examRequestDTO, ExamDifficulty initialExamDifficulty, ExamMode examMode) {
		UUID initialExamId = examRequestDTO.getExamId();
		Lock lock = locks.get(initialExamId);
		lock.lock();
		try {
			TemporaryExamProgress newProgress = TemporaryExamProgress.builder()
					.examMode(examMode)
					.currentDifficulty(TaskDifficulty.fromLevel(initialExamDifficulty.getLevel()))
					.lastTaskId(0L)
					.correctlyAnsweredQuestionsId(new CopyOnWriteArrayList<>())
					.failAnswersCount(0)
					.successAnswersCount(0)
					.failAnswersCountAbsolute(0)
					.successAnswersCountAbsolute(0)
					.nextQuestionRequestId(examRequestDTO.getRequestId())
					.progressCreateAt(LocalDateTime.now())
					.currentBasePoint(0)
					.userAnswers(new CopyOnWriteArrayList<>())
					.tabSwitchViolationCount(0)
					.textCopyViolationCount(0)
					.heartbeatMissedCount(0)
					.devToolsViolationCount(0)
					.domTamperingViolationCount(0)
					.functionTamperingViolationCount(0)
					.moduleTamperingViolationCount(0)
					.pageCloseViolationCount(0)
					.externalContentViolationCount(0)
					.antiOcrTamperingViolationCount(0)
					.terminatedByViolations(false)
					.terminatedByFailAnswerCount(false)
					.build();
			cache.put(initialExamId, newProgress);
			log.info("Создан новый прогресс для examId={}, mode={}, initialDifficulty={}", 
				initialExamId, examMode, initialExamDifficulty);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Получает копию прогресса экзамена из кэша.
	 * <p>
	 * Метод возвращает копию объекта прогресса для предотвращения случайных изменений
	 * объекта в кэше. Использует блокировку для обеспечения корректной работы
	 * при конкурентном доступе.
	 * 
	 * 
	 * @param examId идентификатор экзамена
	 * @return Optional, содержащий копию прогресса экзамена, если он существует
	 */
	public Optional<TemporaryExamProgress> get(UUID examId) {
		Lock lock = locks.get(examId);
		lock.lock();
		try {
			var existing = cache.getIfPresent(examId);
			if (existing != null) {
				log.debug("Найден прогресс для examId={}, режим={}", examId, existing.getExamMode());
				return Optional.of(TemporaryExamProgress.builder()
						.currentDifficulty(existing.getCurrentDifficulty())
						.lastTaskId(existing.getLastTaskId())
						// Важно создать новую коллекцию с теми же элементами для избежания ConcurrentModificationException
						.correctlyAnsweredQuestionsId(new CopyOnWriteArrayList<>(existing.getCorrectlyAnsweredQuestionsId()))
						.failAnswersCount(existing.getFailAnswersCount())
						.failAnswersCountAbsolute(existing.getFailAnswersCountAbsolute())
						.successAnswersCount(existing.getSuccessAnswersCount())
						.successAnswersCountAbsolute(existing.getSuccessAnswersCountAbsolute())
						.lastAnswerCheckRequestId(existing.getLastAnswerCheckRequestId())
						.nextAnswerCheckRequestId(existing.getNextAnswerCheckRequestId())
						.lastQuestionRequestId(existing.getLastQuestionRequestId())
						.nextQuestionRequestId(existing.getNextQuestionRequestId())
						.examMode(existing.getExamMode())
						.progressCreateAt(existing.getProgressCreateAt())
						.currentBasePoint(existing.getCurrentBasePoint())
						.timeOfLastQuestion(existing.getTimeOfLastQuestion())
						.userAnswers(new CopyOnWriteArrayList<>(existing.getUserAnswers()))
						.tabSwitchViolationCount(existing.getTabSwitchViolationCount())
						.textCopyViolationCount(existing.getTextCopyViolationCount())
						.heartbeatMissedCount(existing.getHeartbeatMissedCount())
						.devToolsViolationCount(existing.getDevToolsViolationCount())
						.domTamperingViolationCount(existing.getDomTamperingViolationCount())
						.functionTamperingViolationCount(existing.getFunctionTamperingViolationCount())
						.moduleTamperingViolationCount(existing.getModuleTamperingViolationCount())
						.pageCloseViolationCount(existing.getPageCloseViolationCount())
						.externalContentViolationCount(existing.getExternalContentViolationCount())
						.terminatedByViolations(existing.isTerminatedByViolations())
						.terminatedByFailAnswerCount(existing.isTerminatedByFailAnswerCount())
						.lastSessionToken(existing.getLastSessionToken())
						.lastHeartbeatTime(existing.getLastHeartbeatTime())
						.nextExpectedHeartbeatTime(existing.getNextExpectedHeartbeatTime())
						.antiOcrTamperingViolationCount(existing.getAntiOcrTamperingViolationCount())
						.build());
			} else {
				log.warn("Прогресс не найден для examId={}", examId);
				return Optional.empty();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Получает режим экзамена по идентификатору.
	 * <p>
	 * Выполняет прямой запрос к кэшу без копирования полного объекта прогресса,
	 * что делает операцию более эффективной.
	 * 
	 *
	 * @param examId идентификатор экзамена
	 * @return Optional, содержащий режим экзамена, если запись существует
	 */
	public Optional<ExamMode> getExamMode(UUID examId) {
		var existing = cache.getIfPresent(examId);
		if (existing != null) {
			log.trace("Получен режим экзамена {} для examId={}", existing.getExamMode(), examId);
			return Optional.of(existing.getExamMode());
		} else {
			log.debug("Попытка получить режим несуществующего экзамена с examId={}", examId);
			return Optional.empty();
		}
	}

	/**
	 * Удаляет прогресс экзамена из кэша.
	 * <p>
	 * Используется при завершении экзамена, отмене экзамена или для очистки устаревших данных.
	 * Использует блокировку для обеспечения атомарности операции.
	 * 
	 *
	 * @param examId идентификатор экзамена для удаления
	 */
	public void remove(UUID examId) {
		Lock lock = locks.get(examId);
		lock.lock();
		try {
			TemporaryExamProgress existing = cache.getIfPresent(examId);
			if (existing != null) {
				log.info("Удаление прогресса для examId={}, режим={}", examId, existing.getExamMode());
			} else {
				log.debug("Попытка удалить несуществующий прогресс для examId={}", examId);
			}
			cache.invalidate(examId);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Возвращает статистику прогресса экзамена для создания ответа об отмене экзамена.
	 * <p>
	 * Извлекает только необходимые данные для формирования ответа о прерывании экзамена,
	 * такие как режим экзамена и статистика ответов. Использует блокировку для обеспечения
	 * атомарности операции.
	 * 
	 *
	 * @param examId идентификатор экзамена
	 * @return Optional, содержащий DTO с данными о статистике экзамена
	 */
	public Optional<ExamAbortResponseDTO> getTotalStats(UUID examId) {
		Lock lock = locks.get(examId);
		lock.lock();
		try {
			var existing = cache.getIfPresent(examId);
			if (existing != null) {
				log.debug("Возвращена статистика для examId={}, режим={}, верные ответы={}, неверные ответы={}", 
					examId, existing.getExamMode(), 
					existing.getSuccessAnswersCountAbsolute(), existing.getFailAnswersCountAbsolute());
				return Optional.of(ExamAbortResponseDTO.builder()
						.examMode(existing.getExamMode())
						.successAnswersCountAbsolute(existing.getSuccessAnswersCountAbsolute())
						.failAnswersCountAbsolute(existing.getFailAnswersCountAbsolute())
						.build());
			} else {
				log.warn("Статистика не найдена для examId={}", examId);
				return Optional.empty();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Конвертирует сущность прогресса в кэше в DTO для передачи клиенту.
	 * <p>
	 * Создает объект DTO, содержащий только данные, необходимые клиенту для
	 * отображения состояния экзамена. Блокировка должна быть получена до вызова
	 * этого метода для обеспечения потокобезопасности.
	 * 
	 *
	 * @param temporaryExamProgress сущность прогресса в кэше
	 * @return объект DTO с данными для клиента
	 */
	public static TemporaryExamProgressDTO convertToDTO(TemporaryExamProgress temporaryExamProgress) {
		log.trace("Конвертация прогресса в DTO: режим={}, сложность={}, баллы={}",
				temporaryExamProgress.getExamMode(),
				temporaryExamProgress.getCurrentDifficulty(),
				temporaryExamProgress.getCurrentBasePoint());
				
		return TemporaryExamProgressDTO.builder()
				.examMode(temporaryExamProgress.getExamMode())
				.currentBasePoint(temporaryExamProgress.getCurrentBasePoint())
				.currentDifficulty(temporaryExamProgress.getCurrentDifficulty())
				.failAnswersCountAbsolute(temporaryExamProgress.getFailAnswersCountAbsolute())
				.successAnswersCountAbsolute(temporaryExamProgress.getSuccessAnswersCountAbsolute())
				.build();
	}

	/**
	 * Проверяет, был ли экзамен прерван из-за нарушений.
	 * <p>
	 * Возвращает состояние флага terminatedByViolations из объекта прогресса,
	 * если запись существует в кэше.
	 * 
	 *
	 * @param examId идентификатор экзамена
	 * @return true, если экзамен был прерван из-за нарушений, иначе false
	 */
	public boolean isExamTerminatedByViolations(UUID examId) {
		boolean result = get(examId)
				.map(TemporaryExamProgress::isTerminatedByViolations)
				.orElse(false);
				
		log.debug("Проверка terminatedByViolations для examId={}: {}", examId, result);
		return result;
	}

	/**
	 * Проверяет, был ли экзамен прерван из-за превышения лимита неверных ответов.
	 * <p>
	 * Возвращает состояние флага terminatedByFailAnswerCount из объекта прогресса,
	 * если запись существует в кэше.
	 * 
	 *
	 * @param examId идентификатор экзамена
	 * @return true, если экзамен был прерван из-за превышения лимита неверных ответов, иначе false
	 */
	public boolean isExamTerminatedByFailAnswerCountLimitExceeded(UUID examId) {
		boolean result = get(examId)
				.map(TemporaryExamProgress::isTerminatedByFailAnswerCount)
				.orElse(false);
				
		log.debug("Проверка terminatedByFailAnswerCount для examId={}: {}", examId, result);
		return result;
	}
}

