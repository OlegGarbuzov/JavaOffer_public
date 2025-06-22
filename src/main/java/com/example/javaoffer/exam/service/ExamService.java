package com.example.javaoffer.exam.service;

import com.example.javaoffer.exam.dto.*;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.exception.NoStrategyForSelectModeException;
import com.example.javaoffer.exam.strategy.ExamModeStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для управления процессом прохождения экзаменов.
 * <p>
 * Предоставляет методы для запуска, продолжения, проверки ответов и завершения
 * экзаменов в различных режимах. Использует паттерн "Стратегия" для обработки
 * различных режимов экзамена (свободный, рейтинговый) через соответствующие
 * реализации {@link ExamModeStrategy}.
 * 
 *
 * @author Garbuzov Oleg
 */
@Service
@Slf4j
public class ExamService {
	private final ConcurrentMap<ExamMode, ExamModeStrategy> strategyMap;

	/**
	 * Конструктор с внедрением списка стратегий для различных режимов экзамена.
	 * <p>
	 * Инициализирует карту стратегий, индексированную по режиму экзамена,
	 * для быстрого доступа к нужной стратегии при обработке запросов.
	 * 
	 *
	 * @param strategies список стратегий обработки различных режимов экзамена
	 */
	public ExamService(List<ExamModeStrategy> strategies) {
		this.strategyMap = strategies.stream()
				.collect(Collectors.toConcurrentMap(
						ExamModeStrategy::getSupportedMode,
						Function.identity()
				));
		log.debug("ExamService инициализирован с {} стратегиями: {}",
				strategies.size(),
				strategies.stream().map(s -> s.getSupportedMode().name()).collect(Collectors.joining(", ")));
	}

	/**
	 * Запускает новый экзамен или продолжает существующий.
	 * <p>
	 * Метод выбирает соответствующую стратегию обработки экзамена
	 * на основе указанного режима и делегирует ей процесс получения
	 * следующего вопроса для пользователя.
	 * 
	 *
	 * @param request  запрос на получение следующего вопроса
	 * @param examMode режим экзамена (свободный, рейтинговый)
	 * @return ответ с данными для продолжения экзамена
	 * @throws NoStrategyForSelectModeException если для указанного режима нет стратегии
	 */
	public ExamResumeResponseDTO startOrResumeExam(ExamNextQuestionRequestDTO request, ExamMode examMode) {
		log.info("examId={}: [ExamService] Запуск или продолжение экзамена, mode={}, requestId={}",
				request.getExamId(), examMode, request.getRequestId());
		ExamModeStrategy strategy = getExamModeStrategy(request.getExamId(), examMode);
		ExamResumeResponseDTO response = strategy.nextQuestionExamProcess(request);
		log.debug("examId={}: [ExamService] Следующий вопрос: taskId={}, difficulty={}",
				request.getExamId(),
				response.getTaskDto() != null ? response.getTaskDto().getId() : null,
				response.getStats() != null ? response.getStats().getCurrentDifficulty() : null);
		return response;
	}

	/**
	 * Проверяет ответ пользователя и обновляет статистику экзамена.
	 * <p>
	 * Метод выбирает соответствующую стратегию обработки экзамена
	 * на основе указанного режима и делегирует ей процесс проверки
	 * ответа пользователя и обновления статистики.
	 * 
	 *
	 * @param request  запрос на проверку ответа пользователя
	 * @param examMode режим экзамена (свободный, рейтинговый)
	 * @return ответ с результатами проверки ответа
	 * @throws NoStrategyForSelectModeException если для указанного режима нет стратегии
	 */
	public ValidateAnswerResponseDTO answerValidationAndUpdateStats(ExamCheckAnswerRequestDTO request, ExamMode examMode) {
		log.info("examId={}: [ExamService] Проверка ответа пользователя, selectedAnswer={}",
				request.getExamId(), request.getSelectedAnswer());
		ExamModeStrategy strategy = getExamModeStrategy(request.getExamId(), examMode);
		ValidateAnswerResponseDTO response = strategy.answerProcess(request);
		log.debug("examId={}: [ExamService] Проверка ответа завершена: correctId={}, userChooseIsCorrect={}",
				request.getExamId(),
				response.getId(),
				response.getUserChooseIsCorrect());
		return response;
	}

	/**
	 * Получает соответствующую стратегию для обработки экзамена в указанном режиме.
	 * <p>
	 * Метод проверяет наличие режима экзамена в запросе и соответствующей
	 * стратегии в карте стратегий. Если стратегия не найдена, выбрасывает исключение.
	 * 
	 *
	 * @param examId   идентификатор экзамена
	 * @param examMode режим экзамена
	 * @return стратегия обработки экзамена для указанного режима
	 * @throws NoStrategyForSelectModeException если для указанного режима нет стратегии
	 */
	private ExamModeStrategy getExamModeStrategy(UUID examId, ExamMode examMode) {
		log.debug("Запрос на получение стратегии для экзамена: examId={}, mode={}",
				examId, examMode);

		if (examMode == null) {
			log.error("Режим экзамена не указан в запросе для examId={}", examId);
			throw new NoStrategyForSelectModeException("Ошибка сервера: режим экзамена не указан");
		}

		ExamModeStrategy strategy = strategyMap.get(examMode);
		if (strategy == null) {
			log.error("Не найдена стратегия для режима: {} (examId={})", examMode, examId);
			throw new NoStrategyForSelectModeException("Ошибка сервера: не найдена стратегия для указанного режима");
		}

		log.debug("Выбрана стратегия {} для обработки запроса экзамена (examId={})",
				strategy.getClass().getSimpleName(), examId);
		return strategy;
	}

	/**
	 * Завершает экзамен по запросу пользователя или системы.
	 * <p>
	 * Метод выбирает соответствующую стратегию обработки экзамена
	 * на основе указанного режима и делегирует ей процесс завершения
	 * экзамена и формирования итоговых результатов.
	 * 
	 *
	 * @param request  запрос на завершение экзамена
	 * @param examMode режим экзамена (свободный, рейтинговый)
	 * @return ответ с результатами экзамена
	 * @throws NoStrategyForSelectModeException если для указанного режима нет стратегии
	 */
	public ExamAbortResponseDTO abortExam(ExamAbortRequestDTO request, ExamMode examMode) {
		log.info("examId={}: [ExamService] Завершение экзамена", request.getExamId());
		ExamModeStrategy strategy = getExamModeStrategy(request.getExamId(), examMode);
		ExamAbortResponseDTO response = strategy.abortProcess(request);

		log.debug("examId={}: [ExamService] Экзамен завершён: success={}, fail={}, score={}, mode={}",
				request.getExamId(),
				response.getSuccessAnswersCountAbsolute(),
				response.getFailAnswersCountAbsolute(),
				response.getScore(),
				response.getExamMode());
		return response;
	}
}
