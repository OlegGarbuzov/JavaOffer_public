package com.example.javaoffer.exam.strategy;

import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.dto.TemporaryExamProgressDTO;
import com.example.javaoffer.exam.cache.exception.NoEntryInCacheException;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.example.javaoffer.exam.dto.*;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.logic.QuestionFinder;
import com.example.javaoffer.exam.property.ModeProperties;
import com.example.javaoffer.exam.service.TaskService;
import com.example.javaoffer.user.service.UserService;
import com.google.common.util.concurrent.Striped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;

import static com.example.javaoffer.exam.utils.ExamAnswerCheckUtils.*;

/**
 * Абстрактный базовый класс для реализации различных стратегий проведения экзаменов.
 * <p>
 * Предоставляет общую логику и зависимости, используемые всеми конкретными стратегиями режимов экзамена.
 * Включает функциональность для получения следующих вопросов, валидации ответов, управления сложностью
 * заданий и обработки дублирующих запросов. Обеспечивает thread-safe обработку с использованием блокировок.
 * 
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@RequiredArgsConstructor
public abstract class ModeStrategyAbstract {

	final ExamSessionCacheService examSessionCacheService;
	final TaskService taskService;
	final QuestionFinder questionFinder;
	final ModeProperties properties;
	final UserService userService;
	private final Striped<Lock> examLocks = Striped.lock(10240);

	/**
	 * Обрабатывает запрос на получение следующего вопроса в экзамене.
	 * <p>
	 * Использует блокировку для thread-safe обработки. Проверяет дублирующие запросы,
	 * валидирует идентификаторы запросов, определяет сложность следующего вопроса,
	 * выбирает подходящий вопрос и обновляет прогресс экзамена.
	 * 
	 *
	 * @param examRequest запрос на получение следующего вопроса
	 * @return ответ с данными о следующем вопросе и текущем прогрессе
	 */
	ExamResumeResponseDTO doNextQuestionExamProcessing(ExamNextQuestionRequestDTO examRequest) {
		UUID examId = examRequest.getExamId();
		log.debug("examId={}: Начало обработки запроса экзамена", examId);

		Lock lock = examLocks.get(examId);
		lock.lock();
		try {
			TemporaryExamProgress progress = examSessionCacheService.get(examId).orElseThrow(() ->
					new NoEntryInCacheException("Ошибка. Сессия не найдена. Вероятно вас долго не было. Начните сначала"));

			Optional<ExamResumeResponseDTO> duplicateResponse = checkForDuplicateRequestElseGetCachedRequest(examRequest, progress, examId);
			if (duplicateResponse.isPresent()) {
				log.debug("examId={}: Обнаружен дублирующий запрос следующего вопроса, возвращаем предыдущий ответ", examId);
				return duplicateResponse.get();
			}

			nextQuestionValidationRequestID(examId, progress, examRequest);
			TaskDifficulty difficulty = getNextQuestionTaskDifficulty(progress, examId);
			TaskDTO nextTask = selectNextQuestion(progress, difficulty, examId);
			UUID nextAnswerCheckRequestId = UUID.randomUUID();

			nextQuestionUpdateProgressAndSave(progress, nextTask, examId, examRequest, nextAnswerCheckRequestId);

			log.debug("examId={}: Завершение обработки запроса экзамена", examId);

			TemporaryExamProgressDTO progressDTO = ExamSessionCacheService.convertToDTO(progress);
			progressDTO.setRequestId(nextAnswerCheckRequestId);

			return ExamResumeResponseDTO.builder()
					.taskDto(nextTask)
					.stats(progressDTO)
					.build();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Создает ответ для валидации ответа пользователя.
	 * <p>
	 * Проверяет на дублирующие запросы, валидирует идентификаторы,
	 * обновляет статистику экзамена и генерирует новый идентификатор запроса.
	 * 
	 *
	 * @param correctAnswer правильный ответ на вопрос
	 * @param isCorrect     результат проверки ответа пользователя
	 * @param examRequest   запрос на проверку ответа
	 * @param progress      текущий прогресс экзамена
	 * @param examId        идентификатор экзамена
	 * @return ответ с результатами валидации
	 */
	ValidateAnswerResponseDTO getValidateAnswerResponseDTO(
			AnswerDTO correctAnswer,
			boolean isCorrect,
			ExamCheckAnswerRequestDTO examRequest,
			TemporaryExamProgress progress,
			UUID examId) {

		ValidateAnswerResponseDTO response = buildNewValidateAnswerResponseDTO(correctAnswer, isCorrect);

		if (requestAnswerCheckIsDuplicate(examRequest, progress, examId))
			return buildResponseForDuplicateRequestAnswerCheck(progress, response);

		answerCheckValidationRequestID(examId, progress, examRequest);

		UUID nextQuestionRequestId = UUID.randomUUID();
		response.setRequestId(nextQuestionRequestId);

		validateAndRefreshExamStatisticByLastAnswer(progress, isCorrect, examRequest, examId);
		refreshRequestIdByLastAnswer(progress, examRequest, examId, nextQuestionRequestId);

		return response;
	}

	/**
	 * Выбирает следующий вопрос на основе текущего прогресса и указанной сложности.
	 *
	 * @param progress   текущий прогресс экзамена
	 * @param difficulty сложность, с которой нужно выбрать вопрос
	 * @param examId     идентификатор экзамена
	 * @return выбранный вопрос с перемешанными вариантами ответов
	 */
	TaskDTO selectNextQuestion(TemporaryExamProgress progress, TaskDifficulty difficulty, UUID examId) {
		List<TaskDTO> taskDTOList = questionFinder.findTasksByDifficulty(difficulty);
		log.debug("examId={}: Найдено {} заданий со сложностью {}", examId, taskDTOList.size(), difficulty);

		TaskDTO nextTask = findBestCandidateQuestion(taskDTOList, progress, examId);
		shuffleAnswers(nextTask);
		
		log.debug("examId={}: Выбрано задание: id={}, сложность={}",
				examId, nextTask.getId(), nextTask.getDifficulty());

		return nextTask;
	}

	/**
	 * Перемешивает порядок вариантов ответа для предотвращения запоминания
	 * позиции правильного ответа пользователями.
	 *
	 * @param nextTask вопрос, для которого нужно перемешать ответы
	 */
	private void shuffleAnswers(TaskDTO nextTask) {
		if (nextTask != null && nextTask.getAnswers() != null && !nextTask.getAnswers().isEmpty()) {
			List<AnswerDTO> answers = new ArrayList<>(nextTask.getAnswers());
			Collections.shuffle(answers);
			nextTask.setAnswers(answers);
		}
	}

	/**
	 * Находит наиболее подходящий вопрос среди доступных кандидатов.
	 * Исключает вопросы, на которые уже был дан правильный ответ, и последний заданный вопрос.
	 *
	 * @param taskDTOList список доступных вопросов
	 * @param progress    текущий прогресс экзамена
	 * @param examId      идентификатор экзамена
	 * @return выбранный вопрос или первый из списка, если подходящих кандидатов нет
	 */
	private TaskDTO findBestCandidateQuestion(List<TaskDTO> taskDTOList, TemporaryExamProgress progress, UUID examId) {
		List<Long> correctlyAnsweredQuestionsId = progress.getCorrectlyAnsweredQuestionsId();
		Long lastQuestionId = progress.getLastTaskId();

		List<TaskDTO> candidates = taskDTOList.stream()
				.filter(task -> !task.getId().equals(lastQuestionId))
				.filter(task -> !correctlyAnsweredQuestionsId.contains(task.getId()))
				.toList();

		log.debug("examId={}: Отфильтровано {} подходящих кандидатов из {} доступных заданий",
				examId, candidates.size(), taskDTOList.size());

		if (candidates.isEmpty()) {
			log.debug("examId={}: Не найдено подходящих кандидатов, используется первое задание из списка", examId);
			return taskDTOList.getFirst();
		} else {
			int selectedIndex = ThreadLocalRandom.current().nextInt(candidates.size());
			log.debug("examId={}: Случайно выбрано задание с индексом {} из {} кандидатов",
					examId, selectedIndex, candidates.size());
			return candidates.get(selectedIndex);
		}
	}

	/**
	 * Обновляет прогресс экзамена после получения следующего вопроса и сохраняет его.
	 * <p>
	 * Обновляет информацию о текущем вопросе и сложности, управляет идентификаторами запросов
	 * для обеспечения корректной последовательности операций.
	 * 
	 *
	 * @param progress                   текущий прогресс экзамена
	 * @param nextTask                   следующий вопрос
	 * @param examId                     идентификатор экзамена
	 * @param examRequest                запрос на получение следующего вопроса
	 * @param nextAnswerCheckRequestId   идентификатор для следующего запроса проверки ответа
	 */
	void nextQuestionUpdateProgressAndSave(
			TemporaryExamProgress progress,
			TaskDTO nextTask,
			UUID examId,
			ExamNextQuestionRequestDTO examRequest,
			UUID nextAnswerCheckRequestId) {
		
		refreshQuestionsAndDifficultyInfoByExamProgress(progress, nextTask, examId);

		progress.setLastQuestionRequestId(examRequest.getRequestId());
		log.debug("examId={}: Saved LastQuestionRequestId: {}", examId, examRequest.getRequestId());

		progress.setNextQuestionRequestId(null);
		log.debug("examId={}: Clear NextQuestionRequestId: {}", examId, null);

		progress.setNextAnswerCheckRequestId(nextAnswerCheckRequestId);
		log.debug("examId={}: Save NextAnswerCheckRequestId: {}", examId, nextAnswerCheckRequestId);

		examSessionCacheService.save(examId, progress);
	}

	/**
	 * Обновляет информацию о текущем вопросе и сложности в прогрессе экзамена.
	 *
	 * @param progress текущий прогресс экзамена
	 * @param nextTask следующий вопрос
	 * @param examId   идентификатор экзамена
	 */
	private void refreshQuestionsAndDifficultyInfoByExamProgress(TemporaryExamProgress progress, TaskDTO nextTask, UUID examId) {
		TaskDifficulty oldDifficulty = progress.getCurrentDifficulty();
		Long oldTaskId = progress.getLastTaskId();

		progress.setCurrentDifficulty(nextTask.getDifficulty());
		progress.setLastTaskId(nextTask.getId());
		progress.setTimeOfLastQuestion(Instant.now());

		log.debug("examId={}: Обновлена информация в прогрессе: lastTaskId: {} -> {}, difficulty: {} -> {}",
				examId, oldTaskId, nextTask.getId(), oldDifficulty, nextTask.getDifficulty());
	}

	/**
	 * Проверяет наличие дублирующего запроса и возвращает кэшированный ответ, если найден.
	 *
	 * @param examRequest запрос на получение следующего вопроса
	 * @param progress    текущий прогресс экзамена
	 * @param examId      идентификатор экзамена
	 * @return кэшированный ответ, если запрос дублирующий, иначе пустой Optional
	 */
	Optional<ExamResumeResponseDTO> checkForDuplicateRequestElseGetCachedRequest(ExamNextQuestionRequestDTO examRequest, TemporaryExamProgress progress, UUID examId) {
		if (Objects.equals(progress.getLastQuestionRequestId(), examRequest.getRequestId())) {
			log.warn("examId={}: Duplicate request detected: {}, returning previous response",
					examId, examRequest.getRequestId());

			TemporaryExamProgressDTO progressDTO = ExamSessionCacheService.convertToDTO(progress);
			UUID requestIdToUse = Optional.ofNullable(progress.getNextAnswerCheckRequestId())
					.orElse(progress.getLastAnswerCheckRequestId());

			log.warn("examId={}: Using requestId: {}", examId, requestIdToUse);
			progressDTO.setRequestId(requestIdToUse);

			return Optional.of(
					ExamResumeResponseDTO.builder()
							.taskDto(taskService.getTaskById(progress.getLastTaskId()))
							.stats(progressDTO)
							.build()
			);
		}
		return Optional.empty();
	}

	/**
	 * Валидирует и обновляет статистику экзамена на основе последнего ответа.
	 *
	 * @param progress    текущий прогресс экзамена
	 * @param isCorrect   правильность ответа
	 * @param examRequest запрос на проверку ответа
	 * @param examId      идентификатор экзамена
	 */
	void validateAndRefreshExamStatisticByLastAnswer(
			TemporaryExamProgress progress,
			boolean isCorrect,
			ExamCheckAnswerRequestDTO examRequest,
			UUID examId) {
		Long lastAnswerId = examRequest.getSelectedAnswer();
		if (isCorrect) {
			handleCorrectAnswer(progress, lastAnswerId, examId);
		} else {
			handleIncorrectAnswer(progress, examId);
		}
	}

	/**
	 * Обновляет идентификаторы запросов после обработки ответа.
	 *
	 * @param progress                текущий прогресс экзамена
	 * @param examRequest             запрос на проверку ответа
	 * @param examId                  идентификатор экзамена
	 * @param nextQuestionRequestId   идентификатор для следующего запроса вопроса
	 */
	void refreshRequestIdByLastAnswer(TemporaryExamProgress progress, ExamCheckAnswerRequestDTO examRequest, UUID examId, UUID nextQuestionRequestId) {
		progress.setLastAnswerCheckRequestId(examRequest.getRequestId());
		log.debug("examId={}: Saved LastAnswerCheckRequestId ID: {}", examId, examRequest.getRequestId());

		progress.setNextAnswerCheckRequestId(null);
		log.debug("examId={}: Saved NextAnswerCheckRequestId ID: {}", examId, null);

		progress.setNextQuestionRequestId(nextQuestionRequestId);
		log.debug("examId={}: Save NextQuestionRequestId request ID: {}", examId, nextQuestionRequestId);
	}

	/**
	 * Обрабатывает правильный ответ: увеличивает счетчик успешных ответов,
	 * сбрасывает счетчик неправильных ответов и добавляет ID вопроса в список правильно отвеченных.
	 *
	 * @param progress  текущий прогресс экзамена
	 * @param answerId  идентификатор ответа
	 * @param examId    идентификатор экзамена
	 */
	private void handleCorrectAnswer(TemporaryExamProgress progress, Long answerId, UUID examId) {
		log.debug("examId={}: Правильный ответ для taskId={}, answerId={}. Увеличиваем счетчик успешных ответов.",
				examId, progress.getLastTaskId(), answerId);

		progress.setSuccessAnswersCount(progress.getSuccessAnswersCount() + 1);
		progress.setSuccessAnswersCountAbsolute(progress.getSuccessAnswersCountAbsolute() + 1);
		progress.setFailAnswersCount(0);
		List<Long> correctlyAnsweredQuestionsId = progress.getCorrectlyAnsweredQuestionsId();
		correctlyAnsweredQuestionsId.add(answerId);
	}

	/**
	 * Обрабатывает неправильный ответ: сбрасывает счетчик успешных ответов,
	 * увеличивает счетчик неправильных ответов.
	 *
	 * @param progress текущий прогресс экзамена
	 * @param examId   идентификатор экзамена
	 */
	private void handleIncorrectAnswer(TemporaryExamProgress progress, UUID examId) {
		log.debug("examId={}: Неправильный ответ для taskId={}. Увеличиваем счетчик неудачных ответов.",
				examId, progress.getLastTaskId());

		progress.setFailAnswersCount(progress.getFailAnswersCount() + 1);
		progress.setFailAnswersCountAbsolute(progress.getFailAnswersCountAbsolute() + 1);
		progress.setSuccessAnswersCount(0);
	}

	/**
	 * Определяет сложность следующего вопроса на основе текущего прогресса.
	 * Увеличивает сложность при достижении порога успешных ответов,
	 * снижает сложность при достижении порога неправильных ответов.
	 *
	 * @param progress текущий прогресс экзамена
	 * @param examId   идентификатор экзамена
	 * @return определенная сложность для следующего вопроса
	 */
	TaskDifficulty getNextQuestionTaskDifficulty(TemporaryExamProgress progress, UUID examId) {
		TaskDifficulty current = progress.getCurrentDifficulty();
		int successCount = progress.getSuccessAnswersCount();
		int failCount = progress.getFailAnswersCount();

		log.debug("examId={}: Определение сложности следующего вопроса: текущая={}, правильных={}, неправильных={}",
				examId, current, successCount, failCount);

		int newLevel = current.getLevel();

		if (successCount >= properties.getSuccessAnswersCount()) {
			newLevel++;
			log.info("examId={}: Повышение уровня сложности с {} до {} после {} правильных ответов",
					examId, current.getLevel(), newLevel, successCount);
			progress.setSuccessAnswersCount(0);
			progress.setFailAnswersCount(0);
		} else if (failCount >= properties.getFailAnswersCount()) {
			newLevel--;
			log.info("examId={}: Понижение уровня сложности с {} до {} после {} неправильных ответов",
					examId, current.getLevel(), newLevel, failCount);
			progress.setSuccessAnswersCount(0);
			progress.setFailAnswersCount(0);
		}

		newLevel = Math.max(1, Math.min(newLevel, 10));
		TaskDifficulty nextDifficulty = TaskDifficulty.fromLevel(newLevel);

		if (current != nextDifficulty) {
			log.debug("examId={}: Итоговая сложность изменена: {} -> {}",
					examId, current, nextDifficulty);
		} else {
			log.debug("examId={}: Сложность остается прежней: {}",
					examId, current);
		}

		return nextDifficulty;
	}
}
