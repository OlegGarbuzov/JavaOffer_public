package com.example.javaoffer.exam.strategy;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.exam.anticheat.service.HeartbeatService;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.dto.TemporaryExamProgressDTO;
import com.example.javaoffer.exam.cache.exception.NoEntryInCacheException;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.example.javaoffer.exam.dto.*;
import com.example.javaoffer.exam.entity.UserScoreHistory;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.enums.TaskTopic;
import com.example.javaoffer.exam.logic.ExamRatingModeHelperService;
import com.example.javaoffer.exam.logic.QuestionFinder;
import com.example.javaoffer.exam.property.RatingModeProperties;
import com.example.javaoffer.exam.service.GlobalRatingScoreHistoryService;
import com.example.javaoffer.exam.service.TaskService;
import com.example.javaoffer.user.service.UserService;
import com.google.common.util.concurrent.Striped;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

import static com.example.javaoffer.exam.utils.ExamAnswerCheckUtils.requestAnswerCheckIsDuplicate;

/**
 * Стратегия обработки экзамена в рейтинговом режиме.
 * <p>
 * Реализует логику проведения экзамена в рейтинговом режиме с системой очков,
 * античит проверками, ограничениями по времени и количеству неверных ответов.
 * Поддерживает завершение экзамена при превышении лимитов и нарушениях правил.
 * Обеспечивает thread-safe обработку запросов с использованием блокировок.
 * 
 *
 * @author Garbuzov Oleg
 * @since 1.0
 */
@Slf4j
@Component
public class RatingModeStrategy extends ModeStrategyAbstract implements ExamModeStrategy {
	private final Striped<Lock> examLocks = Striped.lock(10240);
	private final ExamRatingModeHelperService examRatingModeHelperService;
	private final GlobalRatingScoreHistoryService globalRatingScoreHistoryService;
	private final ExamSessionCacheService examSessionCacheService;
	private final HeartbeatService heartbeatService;

	/**
	 * Конструктор стратегии рейтингового режима экзамена.
	 *
	 * @param taskService                        сервис для работы с заданиями
	 * @param ratingModeProperties               настройки рейтингового режима экзамена
	 * @param questionFinder                     сервис поиска вопросов
	 * @param userService                        сервис для работы с пользователями
	 * @param examRatingModeHelperService        вспомогательный сервис для рейтингового режима
	 * @param globalRatingScoreHistoryService    сервис для работы с глобальным рейтингом
	 * @param examSessionCacheService            сервис для работы с кэшем сессий экзамена
	 * @param heartbeatService                   сервис для античит проверок
	 */
	public RatingModeStrategy(
			TaskService taskService,
			RatingModeProperties ratingModeProperties,
			QuestionFinder questionFinder,
			UserService userService,
			ExamRatingModeHelperService examRatingModeHelperService,
			GlobalRatingScoreHistoryService globalRatingScoreHistoryService, 
			ExamSessionCacheService examSessionCacheService, 
			HeartbeatService heartbeatService) {
		super(examSessionCacheService, taskService, questionFinder, ratingModeProperties, userService);
		this.examRatingModeHelperService = examRatingModeHelperService;
		this.globalRatingScoreHistoryService = globalRatingScoreHistoryService;
		this.examSessionCacheService = examSessionCacheService;
		this.heartbeatService = heartbeatService;
	}

	/**
	 * Возвращает режим экзамена, поддерживаемый данной стратегией.
	 *
	 * @return режим экзамена РЕЙТИНГОВЫЙ
	 */
	@Override
	public ExamMode getSupportedMode() {
		return properties.getMode();
	}

	/**
	 * Обрабатывает запрос на получение следующего вопроса в рейтинговом режиме.
	 * <p>
	 * Проверяет условия завершения экзамена (нарушения или превышение лимита неверных ответов)
	 * перед выдачей следующего вопроса. Логирует входящий запрос и результат.
	 * 
	 *
	 * @param examRequest запрос с данными о текущем экзамене
	 * @return ответ с данными о следующем вопросе и текущем прогрессе
	 */
	@Override
	public ExamResumeResponseDTO nextQuestionExamProcess(ExamNextQuestionRequestDTO examRequest) {
		UUID examId = examRequest.getExamId();
		log.info("examId={}: [RatingMode] Запрос следующего вопроса", examId);

		if (examSessionCacheService.isExamTerminatedByViolations(examId)) {
			log.warn("examId={}: [RatingMode] Экзамен завершен из-за нарушений", examId);
			doAbortProcessing(ExamAbortRequestDTO.builder().examId(examId).build());
			return buildTerminatedByViolationResponse();
		}

		if (examSessionCacheService.isExamTerminatedByFailAnswerCountLimitExceeded(examId)) {
			log.warn("examId={}: [RatingMode] Экзамен завершен из-за превышения лимита неверных ответов", examId);
			ExamAbortResponseDTO abortResponseDTO = doAbortProcessing(
					ExamAbortRequestDTO.builder().examId(examId).build());
			return buildTerminatedByFailCountResponse(abortResponseDTO);
		}

		ExamResumeResponseDTO response = doNextQuestionExamProcessing(examRequest);

		log.debug("examId={}: [RatingMode] Следующий вопрос: taskId={}, difficulty={}",
				examId,
				response.getTaskDto() != null ? response.getTaskDto().getId() : null,
				response.getStats() != null ? response.getStats().getCurrentDifficulty() : null);
		return response;
	}

	/**
	 * Обрабатывает запрос на проверку выбранного ответа пользователя в рейтинговом режиме.
	 * <p>
	 * Проверяет дублирующие запросы, обновляет базовые очки и проверяет
	 * условия завершения экзамена. Логирует входящий запрос и результат.
	 * 
	 *
	 * @param examRequest запрос с данными о текущем экзамене
	 * @return ответ с данными о правильном ответе
	 */
	@Override
	public ValidateAnswerResponseDTO answerProcess(ExamCheckAnswerRequestDTO examRequest) {
		log.info("examId={}: [RatingMode] Проверка ответа пользователя", examRequest.getExamId());
		ValidateAnswerResponseDTO response = doAnswerProcessing(examRequest);
		log.debug("examId={}: [RatingMode] Проверка ответа завершена: correctId={}, userChooseIsCorrect={}",
				examRequest.getExamId(),
				response.getId(),
				response.getUserChooseIsCorrect());
		return response;
	}

	/**
	 * Обрабатывает запрос на завершение экзамена в рейтинговом режиме.
	 * <p>
	 * Подсчитывает финальные результаты, обновляет глобальный рейтинг
	 * и очищает связанные ресурсы. Логирует входящий запрос и результат.
	 * 
	 *
	 * @param examAbortRequestDTO запрос на завершение экзамена
	 * @return результат завершения экзамена со статистикой
	 */
	@Override
	public ExamAbortResponseDTO abortProcess(ExamAbortRequestDTO examAbortRequestDTO) {
		log.info("examId={}: [RatingMode] Запрос на завершение экзамена", examAbortRequestDTO.getExamId());
		ExamAbortResponseDTO response = doAbortProcessing(examAbortRequestDTO);
		log.debug("examId={}: [RatingMode] Экзамен завершён: success={}, fail={}, score={}",
				examAbortRequestDTO.getExamId(),
				response.getSuccessAnswersCountAbsolute(),
				response.getFailAnswersCountAbsolute(),
				response.getScore());
		return response;
	}

	/**
	 * Выполняет внутреннюю обработку ответа пользователя в рейтинговом режиме.
	 * <p>
	 * Использует блокировку для thread-safe обработки. Проверяет ответ,
	 * обновляет базовые очки, проверяет лимиты и античит условия.
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
			boolean isDuplicate = requestAnswerCheckIsDuplicate(examRequest, progress, examId);

			ValidateAnswerResponseDTO responseDTO = getValidateAnswerResponseDTO(
					correctAnswer,
					isCorrect,
					examRequest,
					progress,
					examId
			);

			if (!isDuplicate) {
				examRatingModeHelperService.refreshBasePoints(progress, examId, taskDTO, examRequest, isCorrect);
			}

			examRatingModeHelperService.terminateExamIfFailAnswerCountAbsoluteLimitExceeded(examId, progress);
			heartbeatService.heartBeatLongAbsenceCheck(progress, examId);

			return responseDTO;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Обрабатывает завершение экзамена и подсчитывает финальные результаты.
	 * <p>
	 * Использует блокировку для thread-safe обработки. Подсчитывает итоговые очки,
	 * обновляет глобальный рейтинг (если экзамен не был завершен из-за нарушений)
	 * и очищает связанные ресурсы.
	 * 
	 *
	 * @param examAbortRequestDTO запрос на завершение экзамена
	 * @return результат завершения экзамена
	 */
	private ExamAbortResponseDTO doAbortProcessing(ExamAbortRequestDTO examAbortRequestDTO) {
		UUID examId = examAbortRequestDTO.getExamId();
		log.debug("examId={}: Начало обработки завершения экзамена", examId);

		Lock lock = examLocks.get(examId);
		lock.lock();
		try {
			TemporaryExamProgress progress = examSessionCacheService.get(examId).orElseThrow(() ->
					new NoEntryInCacheException("Прогресс не найден или устарел"));

			UserScoreHistory userScoreHistory = examRatingModeHelperService.summingUp(progress, examId);

			if (!progress.isTerminatedByViolations()) {
				globalRatingScoreHistoryService.refreshBestUserScoreInGlobalRating(userScoreHistory);
			}

			examSessionCacheService.remove(examId);
			ClientUtils.getCurrentUser().ifPresent(userService::clearUnfinishedExam);

			return ExamAbortResponseDTO.builder()
					.examMode(progress.getExamMode())
					.successAnswersCountAbsolute(userScoreHistory.getSuccessAnswersCountAbsolute())
					.failAnswersCountAbsolute(userScoreHistory.getFailAnswersCountAbsolute())
					.totalBasePoints(userScoreHistory.getTotalBasePoints())
					.bonusByTime(userScoreHistory.getBonusByTime())
					.score(userScoreHistory.getScore())
					.timeTakenToComplete(userScoreHistory.getTimeTakenToComplete())
					.build();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Создает ответ для экзамена, завершенного из-за нарушений.
	 *
	 * @return ответ с флагом завершения из-за нарушений
	 */
	private ExamResumeResponseDTO buildTerminatedByViolationResponse() {
		return ExamResumeResponseDTO.builder()
				.taskDto(TaskDTO.builder().id(0L).topic(TaskTopic.OTHER).build())
				.stats(TemporaryExamProgressDTO.builder().examMode(properties.getMode()).build())
				.examTerminatedByViolation(true)
				.build();
	}

	/**
	 * Создает ответ для экзамена, завершенного из-за превышения лимита неверных ответов.
	 *
	 * @param abortResponseDTO результат завершения экзамена
	 * @return ответ с флагом завершения из-за превышения лимита неверных ответов
	 */
	private ExamResumeResponseDTO buildTerminatedByFailCountResponse(ExamAbortResponseDTO abortResponseDTO) {
		return ExamResumeResponseDTO.builder()
				.taskDto(TaskDTO.builder().id(0L).topic(TaskTopic.OTHER).build())
				.stats(TemporaryExamProgressDTO.builder().examMode(properties.getMode()).build())
				.abortResults(abortResponseDTO)
				.examTerminatedByFailAnswerCount(true)
				.build();
	}
}
