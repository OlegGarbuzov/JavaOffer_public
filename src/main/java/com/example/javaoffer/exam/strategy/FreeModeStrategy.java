package com.example.javaoffer.exam.strategy;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.exception.NoEntryInCacheException;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.example.javaoffer.exam.dto.*;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.logic.QuestionFinder;
import com.example.javaoffer.exam.property.FreeModeProperties;
import com.example.javaoffer.exam.service.TaskService;
import com.example.javaoffer.user.service.UserService;
import com.google.common.util.concurrent.Striped;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

/**
 * Стратегия обработки экзамена в свободном режиме.
 * <p>
 * Реализует логику проведения экзамена в свободном режиме, где пользователь
 * может отвечать на вопросы без ограничений по времени или количеству попыток.
 * Обеспечивает thread-safe обработку запросов с использованием блокировок
 * для предотвращения race conditions.
 * 
 *
 * @author Garbuzov Oleg
 * @since 1.0
 */
@Slf4j
@Component
public class FreeModeStrategy extends ModeStrategyAbstract implements ExamModeStrategy {
	private final Striped<Lock> examLocks = Striped.lock(10240);

	/**
	 * Конструктор стратегии свободного режима экзамена.
	 *
	 * @param examSessionCacheService сервис для работы с кэшем сессий экзамена
	 * @param taskService             сервис для работы с заданиями
	 * @param freeModeProperties      настройки свободного режима экзамена
	 * @param freeModeQuestionFinder  сервис поиска вопросов для свободного режима
	 * @param userService             сервис для работы с пользователями
	 */
	public FreeModeStrategy(
			ExamSessionCacheService examSessionCacheService,
			TaskService taskService,
			FreeModeProperties freeModeProperties,
			QuestionFinder freeModeQuestionFinder,
			UserService userService) {
		super(examSessionCacheService, taskService, freeModeQuestionFinder, freeModeProperties, userService);
	}

	/**
	 * Возвращает режим экзамена, поддерживаемый данной стратегией.
	 *
	 * @return режим экзамена СВОБОДНЫЙ
	 */
	@Override
	public ExamMode getSupportedMode() {
		return properties.getMode();
	}

	/**
	 * Обрабатывает запрос на получение следующего вопроса в свободном режиме.
	 * <p>
	 * Логирует входящий запрос и результат обработки.
	 * 
	 *
	 * @param examRequest запрос с данными о текущем экзамене
	 * @return ответ с данными о следующем вопросе и текущем прогрессе
	 */
	@Override
	public ExamResumeResponseDTO nextQuestionExamProcess(ExamNextQuestionRequestDTO examRequest) {
		log.info("examId={}: [FreeMode] Запрос следующего вопроса", examRequest.getExamId());
		ExamResumeResponseDTO response = doNextQuestionExamProcessing(examRequest);
		log.debug("examId={}: [FreeMode] Следующий вопрос: taskId={}, difficulty={}",
				examRequest.getExamId(),
				response.getTaskDto() != null ? response.getTaskDto().getId() : null,
				response.getStats() != null ? response.getStats().getCurrentDifficulty() : null);
		return response;
	}

	/**
	 * Обрабатывает запрос на проверку ответа пользователя в свободном режиме.
	 * <p>
	 * Логирует входящий запрос и результат проверки.
	 * 
	 *
	 * @param examRequest запрос с выбранным пользователем ответом
	 * @return результат проверки ответа с правильным ответом и объяснением
	 */
	@Override
	public ValidateAnswerResponseDTO answerProcess(ExamCheckAnswerRequestDTO examRequest) {
		log.info("examId={}: [FreeMode] Проверка ответа пользователя", examRequest.getExamId());
		ValidateAnswerResponseDTO response = doAnswerProcessing(examRequest);
		log.debug("examId={}: [FreeMode] Проверка ответа завершена: correctId={}, userChooseIsCorrect={}",
				examRequest.getExamId(),
				response.getId(),
				response.getUserChooseIsCorrect());
		return response;
	}

	/**
	 * Обрабатывает запрос на завершение экзамена в свободном режиме.
	 * <p>
	 * Логирует входящий запрос и результат завершения экзамена.
	 * 
	 *
	 * @param examAbortRequestDTO запрос на завершение экзамена
	 * @return результат завершения экзамена со статистикой
	 */
	@Override
	public ExamAbortResponseDTO abortProcess(ExamAbortRequestDTO examAbortRequestDTO) {
		log.info("examId={}: [FreeMode] Запрос на завершение экзамена", examAbortRequestDTO.getExamId());
		ExamAbortResponseDTO response = doAbortProcessing(examAbortRequestDTO);
		log.debug("examId={}: [FreeMode] Экзамен завершён: success={}, fail={}, score={}",
				examAbortRequestDTO.getExamId(),
				response.getSuccessAnswersCountAbsolute(),
				response.getFailAnswersCountAbsolute(),
				response.getScore());
		return response;
	}

	/**
	 * Выполняет обработку ответа пользователя в свободном режиме.
	 * <p>
	 * Использует блокировку для обеспечения thread-safe обработки.
	 * Получает текущее задание, проверяет правильность ответа
	 * и обновляет статистику экзамена.
	 * 
	 *
	 * @param examRequest запрос на проверку ответа
	 * @return результат валидации ответа
	 */
	private ValidateAnswerResponseDTO doAnswerProcessing(ExamCheckAnswerRequestDTO examRequest) {
		UUID examId = examRequest.getExamId();
		log.debug("examId={}: Начало обработки запроса ответа пользователя", examId);

		Lock lock = examLocks.get(examId);
		lock.lock();
		try {
			TemporaryExamProgress progress = examSessionCacheService.get(examId).orElseThrow(() ->
					new NoEntryInCacheException("Прогресс не найден или устарел."));

			TaskDTO taskDTO = taskService.getTaskById(progress.getLastTaskId());
			AnswerDTO correctAnswer = taskService.getCorrectAnswerByTaskDto(taskDTO, examRequest.getSelectedAnswer());
			boolean isCorrect = taskService.answerIsCorrect(taskDTO, examRequest.getSelectedAnswer());

			ValidateAnswerResponseDTO validateAnswerResponseDTO = getValidateAnswerResponseDTO(
					correctAnswer,
					isCorrect,
					examRequest,
					progress,
					examId
			);

			examSessionCacheService.save(examId, progress);

			return validateAnswerResponseDTO;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Обрабатывает завершение экзамена и очищает связанные ресурсы.
	 * <p>
	 * Использует блокировку для обеспечения thread-safe обработки.
	 * Получает финальную статистику, очищает кэш и сбрасывает
	 * незавершенный экзамен у пользователя.
	 * 
	 *
	 * @param examAbortRequestDTO запрос на завершение экзамена
	 * @return результат завершения экзамена
	 */
	ExamAbortResponseDTO doAbortProcessing(ExamAbortRequestDTO examAbortRequestDTO) {
		UUID examId = examAbortRequestDTO.getExamId();
		log.debug("examId={}: Начало обработки завершения экзамена", examId);

		Lock lock = examLocks.get(examId);
		lock.lock();
		try {
			ExamAbortResponseDTO examAbortResponseDTO = examSessionCacheService.getTotalStats(examId)
					.orElseThrow(() -> new NoEntryInCacheException(
							"Прогресс не найден или устарел. Вероятно вас долго не было. Начните сначала."));

			examSessionCacheService.remove(examId);
			ClientUtils.getCurrentUser().ifPresent(userService::clearUnfinishedExam);

			log.info("examId={}: [FreeMode] Экзамен завершён: success={}, fail={}, score={}",
					examId,
					examAbortResponseDTO.getSuccessAnswersCountAbsolute(),
					examAbortResponseDTO.getFailAnswersCountAbsolute(),
					examAbortResponseDTO.getScore());

			return examAbortResponseDTO;
		} finally {
			lock.unlock();
		}
	}
}
