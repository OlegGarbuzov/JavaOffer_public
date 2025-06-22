package com.example.javaoffer.exam.logic;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.example.javaoffer.exam.dto.AnswerDTO;
import com.example.javaoffer.exam.dto.ExamCheckAnswerRequestDTO;
import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.dto.UserAnswerDTO;
import com.example.javaoffer.exam.entity.UserAnswer;
import com.example.javaoffer.exam.entity.UserScoreHistory;
import com.example.javaoffer.exam.property.RatingModeProperties;
import com.example.javaoffer.exam.service.AnswerService;
import com.example.javaoffer.exam.service.UserAnswerService;
import com.example.javaoffer.exam.service.UserScoreHistoryService;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.exception.UnauthorizedUserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Вспомогательный сервис для обработки логики рейтингового режима экзамена.
 * <p>
 * Отвечает за расчет времени, затраченного на ответы, подсчет баллов,
 * обработку нарушений и формирование итоговой статистики экзамена.
 * Используется в рейтинговом режиме для обеспечения корректного
 * подсчета очков и обнаружения нарушений.
 * 
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ExamRatingModeHelperService {
	private final AnswerService answerService;
	private final UserScoreHistoryService userScoreHistoryService;
	private final UserAnswerService userAnswerService;
	private final ExamSessionCacheService examSessionCacheService;
	private final RatingModeProperties ratingModeProperties;

	/**
	 * Вычисляет время, затраченное на ответ, и добавляет информацию об ответе в прогресс.
	 * <p>
	 * Метод рассчитывает время между моментом выдачи вопроса и получением ответа,
	 * создает объект UserAnswerDTO и добавляет его в список ответов в прогрессе.
	 * 
	 *
	 * @param progress    текущий прогресс экзамена
	 * @param taskDTO     задание, на которое был дан ответ
	 * @param examRequest запрос с ответом пользователя
	 * @param isCorrect   флаг, указывающий на правильность ответа
	 */
	public void durationAtAnswer(TemporaryExamProgress progress, TaskDTO taskDTO, ExamCheckAnswerRequestDTO examRequest, boolean isCorrect) {
		Instant timeOfLastQuestion = progress.getTimeOfLastQuestion();
		Instant currentTime = Instant.now();

		Duration duration = Duration.between(timeOfLastQuestion, currentTime);
		double timeTakenSeconds = duration.getSeconds() + duration.getNano() / 1_000_000_000.0;
		double roundedTime = Math.round(timeTakenSeconds * 1000.0) / 1000.0;

		log.debug("examId:{} Подсчет времени затраченного на ответ задачи:{} - timeOfLastQuestion:{}, currentTime:{} , duration:{}",
				examRequest.getExamId(),
				taskDTO.getId(),
				timeOfLastQuestion,
				currentTime,
				roundedTime);

		AnswerDTO answerDTO = answerService.getAnswerById(examRequest.getSelectedAnswer());
		UserAnswerDTO userAnswerDTO = UserAnswerDTO.builder()
				.answerDTO(answerDTO)
				.isCorrect(isCorrect)
				.taskDTO(taskDTO)
				.timeTakenSeconds(roundedTime)
				.build();

		List<UserAnswerDTO> currentUserAnswers = progress.getUserAnswers();
		currentUserAnswers.add(userAnswerDTO);

		progress.setUserAnswers(currentUserAnswers);
	}

	/**
	 * Рассчитывает базовые баллы пользователя за ответ на вопрос.
	 * <p>
	 * Метод вычисляет количество баллов в зависимости от сложности вопроса
	 * и правильности ответа. За правильный ответ баллы добавляются, за неправильный - вычитаются.
	 * Минимальное количество баллов не может быть меньше нуля.
	 * 
	 *
	 * @param examId      идентификатор экзамена
	 * @param progress    текущий прогресс экзамена
	 * @param taskDTO     задание, на которое был дан ответ
	 * @param isCorrect   флаг, указывающий на правильность ответа
	 */
	public void calculateUserScore(UUID examId, TemporaryExamProgress progress, TaskDTO taskDTO, boolean isCorrect) {
		int currentBasePoint = progress.getCurrentBasePoint();

		int basePointForQuestion = 10;
		int taskDifficultyLevel = taskDTO.getDifficulty().getLevel();

		int pointsForAnswer = basePointForQuestion * taskDifficultyLevel;

		currentBasePoint = isCorrect ? currentBasePoint + pointsForAnswer : currentBasePoint - pointsForAnswer;

		progress.setCurrentBasePoint(Math.max(currentBasePoint, 0));
		log.debug("examId={}: Подсчет базовых баллов пользователя за вопросы - isCorrect?:{}, currentBasePoint:{}, basePointForQuestion:{}, taskDifficultyLevel:{}, pointsForAnswer:{}, newCurrentBasePoint:{}",
				examId,
				isCorrect,
				currentBasePoint,
				basePointForQuestion,
				taskDifficultyLevel,
				pointsForAnswer,
				currentBasePoint);
	}

	/**
	 * Рассчитывает итоговые баллы и создает запись истории экзамена.
	 * <p>
	 * Метод вычисляет общее количество баллов с учетом бонуса за время,
	 * проверяет наличие нарушений правил и формирует объект истории прохождения экзамена.
	 * 
	 *
	 * @param examId        идентификатор экзамена
	 * @param progress      текущий прогресс экзамена
	 * @param examDuration  продолжительность экзамена в секундах
	 * @param currentUser   текущий пользователь
	 * @return              созданный объект истории экзамена
	 */
	public UserScoreHistory calculateTotalPointsAndReturnNewUserScoreHistory(UUID examId, TemporaryExamProgress progress, long examDuration, User currentUser) {
		log.debug("examId={}: Начало подсчёта итоговых баллов: totalBasePoints={}, successAnswers={}, failAnswers={}, examDuration={}",
				examId,
				progress.getCurrentBasePoint(),
				progress.getSuccessAnswersCountAbsolute(),
				progress.getFailAnswersCountAbsolute(),
				examDuration);

		int totalBasePoints = progress.getCurrentBasePoint();

		long totalQuestionsCountPerProgress
				= progress.getSuccessAnswersCountAbsolute()
				+ progress.getFailAnswersCountAbsolute();

		double bonusByTime = ((double) totalQuestionsCountPerProgress / examDuration * 10);
		bonusByTime = BigDecimal.valueOf(bonusByTime)
				.setScale(2, RoundingMode.HALF_UP)
				.doubleValue();

		long totalScore = (long) (totalBasePoints * bonusByTime);

		// Проверяем, есть ли нарушения правил
		boolean hasViolations = progress.getTabSwitchViolationCount() > 0
				|| progress.getTextCopyViolationCount() > 0
				|| progress.getHeartbeatMissedCount() > 0
				|| progress.getDevToolsViolationCount() > 0
				|| progress.getDomTamperingViolationCount() > 0
				|| progress.getFunctionTamperingViolationCount() > 0
				|| progress.getModuleTamperingViolationCount() > 0
				|| progress.getPageCloseViolationCount() > 0
				|| progress.getExternalContentViolationCount() > 0
				|| progress.getAntiOcrTamperingViolationCount() > 0;

		// Получаем информацию о прерывании из прогресса
		boolean terminatedByViolations = progress.isTerminatedByViolations();
		boolean terminatedByFailAnswerCount = progress.isTerminatedByFailAnswerCount();
		String terminationReason = terminatedByViolations ?
				"Тестирование прервано из-за нарушений правил" : 
				(terminatedByFailAnswerCount ? "Тестирование прервано из-за превышения количества неверных ответов" : null);

		// Создаем объект истории
		UserScoreHistory userScoreHistory = UserScoreHistory.builder()
				.examID(examId)
				.user(currentUser)
				.timeTakenToComplete(examDuration)
				.totalBasePoints(totalBasePoints)
				.bonusByTime(bonusByTime)
				.score(totalScore)
				.failAnswersCountAbsolute(progress.getFailAnswersCountAbsolute())
				.successAnswersCountAbsolute(progress.getSuccessAnswersCountAbsolute())
				.tabSwitchViolations(progress.getTabSwitchViolationCount())
				.textCopyViolations(progress.getTextCopyViolationCount())
				.heartbeatMissedViolations(progress.getHeartbeatMissedCount())
				.devToolsViolations(progress.getDevToolsViolationCount())
				.domTamperingViolations(progress.getDomTamperingViolationCount())
				.functionTamperingViolations(progress.getFunctionTamperingViolationCount())
				.moduleTamperingViolations(progress.getModuleTamperingViolationCount())
				.pageCloseViolations(progress.getPageCloseViolationCount())
				.externalContentViolations(progress.getExternalContentViolationCount())
				.antiOcrTamperingViolations(progress.getAntiOcrTamperingViolationCount())
				.terminatedByViolations(terminatedByViolations)
				.terminatedByFailAnswerCount(terminatedByFailAnswerCount)
				.terminationReason(terminationReason)
				.build();

		log.debug("examId={}: Подсчет итоговых баллов пользователя - totalBasePoints:{}, totalQuestionsCountPerProgress:{}, bonusByTime:{}, totalScore:{}",
				examId,
				totalBasePoints,
				totalQuestionsCountPerProgress,
				bonusByTime,
				totalScore);

		if (hasViolations) {
			log.warn("examId={}: Зафиксированы нарушения правил - tabSwitchViolations:{}, textCopyViolations:{}, heartbeatMissedViolations:{}, devToolsViolations:{}, domTamperingViolations:{}, functionTamperingViolations:{}, moduleTamperingViolations:{}, pageCloseViolations:{}, externalContentViolations:{}, antiOcrTamperingViolations:{}",
					examId,
					progress.getTabSwitchViolationCount(),
					progress.getTextCopyViolationCount(),
					progress.getHeartbeatMissedCount(),
					progress.getDevToolsViolationCount(),
					progress.getDomTamperingViolationCount(),
					progress.getFunctionTamperingViolationCount(),
					progress.getModuleTamperingViolationCount(),
					progress.getPageCloseViolationCount(),
					progress.getExternalContentViolationCount(),
					progress.getAntiOcrTamperingViolationCount());
		}

		return userScoreHistory;
	}

	/**
	 * Устанавливает пользовательские ответы и сохраняет историю экзамена.
	 * <p>
	 * Метод преобразует DTO ответов пользователя в сущности, связывает их с историей
	 * экзамена и сохраняет историю в базе данных.
	 * 
	 *
	 * @param userScoreHistory история экзамена для сохранения
	 * @param progress         текущий прогресс экзамена с ответами пользователя
	 */
	public void setUserAnswersAndSaveUserScoreHistory(UserScoreHistory userScoreHistory, TemporaryExamProgress progress) {
		List<UserAnswer> userAnswers = progress.getUserAnswers().stream()
				.map((answerDTO) -> userAnswerService.convertToEntity(answerDTO, userScoreHistory))
				.toList();
		userScoreHistory.setUserAnswers(userAnswers);

		userScoreHistoryService.save(userScoreHistory);
	}


	/**
	 * Обновляет базовые баллы пользователя после ответа на вопрос.
	 * <p>
	 * Метод вызывает необходимые методы для расчета времени ответа,
	 * обновления счетчика баллов и сохранения прогресса в кэше.
	 * 
	 *
	 * @param progress    текущий прогресс экзамена
	 * @param examId      идентификатор экзамена
	 * @param taskDTO     задание, на которое был дан ответ
	 * @param examRequest запрос с ответом пользователя
	 * @param isCorrect   флаг, указывающий на правильность ответа
	 */
	public void refreshBasePoints(
			TemporaryExamProgress progress,
			UUID examId,
			TaskDTO taskDTO,
			ExamCheckAnswerRequestDTO examRequest,
			boolean isCorrect) {
		//Считаем сколько времени прошло между выдачей задания и ответом
		durationAtAnswer(progress, taskDTO, examRequest, isCorrect);

		//Обновляем счетчик количества базовых баллов за ответы
		calculateUserScore(examId, progress, taskDTO, isCorrect);

		examSessionCacheService.save(examId, progress);
	}

	/**
	 * Подводит итоги экзамена и формирует историю прохождения.
	 * <p>
	 * Метод получает текущего пользователя, рассчитывает продолжительность экзамена,
	 * вычисляет итоговые баллы и сохраняет историю в базе данных.
	 * 
	 *
	 * @param progress текущий прогресс экзамена
	 * @param examId   идентификатор экзамена
	 * @return         созданный объект истории экзамена
	 */
	public UserScoreHistory summingUp(
			TemporaryExamProgress progress,
			UUID examId) {

		User currentUser = ClientUtils.getCurrentUser().orElseThrow(() ->
				new UnauthorizedUserException("Пользователь должен быть авторизован"));
		Duration examDuration = Duration.between(progress.getProgressCreateAt(), LocalDateTime.now());

		if (examDuration.getSeconds() < 1) log.error("examId={}: examDuration < 1. Взяли дефолтное значение:1", examId);

		UserScoreHistory userScoreHistory = calculateTotalPointsAndReturnNewUserScoreHistory(examId, progress, Math.max(examDuration.getSeconds(), 1), currentUser);

		setUserAnswersAndSaveUserScoreHistory(userScoreHistory,
				progress);

		log.info("examId={}: [RatingMode] Итоги экзамена: user={}, base score={}, success={}, fail={}, duration={}, time bonus={}, total score={}",
				examId,
				currentUser.getUsername(),
				userScoreHistory.getTotalBasePoints(),
				progress.getSuccessAnswersCountAbsolute(),
				progress.getFailAnswersCountAbsolute(),
				examDuration.getSeconds(),
				userScoreHistory.getBonusByTime(),
				userScoreHistory.getScore());

		return userScoreHistory;
	}

	/**
	 * Проверяет, превышен ли лимит неверных ответов, и прерывает экзамен при необходимости.
	 * <p>
	 * Метод сравнивает количество неверных ответов с абсолютным лимитом из настроек
	 * и устанавливает флаг прерывания экзамена, если лимит превышен.
	 * 
	 *
	 * @param examId   идентификатор экзамена
	 * @param progress текущий прогресс экзамена
	 */
	public void terminateExamIfFailAnswerCountAbsoluteLimitExceeded(UUID examId, TemporaryExamProgress progress) {
		int failAnswersCountAbsoluteLimit = ratingModeProperties.getFailAnswersCountAbsoluteLimit();
		int failAnswersCountAbsolute = progress.getFailAnswersCountAbsolute();
		
		if (failAnswersCountAbsolute >= failAnswersCountAbsoluteLimit) {
			log.info("examId={}: Превышен лимит неверных ответов: {} из {}. Экзамен будет прерван.",
					examId, failAnswersCountAbsolute, failAnswersCountAbsoluteLimit);
			progress.setTerminatedByFailAnswerCount(true);
			examSessionCacheService.save(examId, progress);
		}
	}
}
