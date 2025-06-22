package com.example.javaoffer.exam.strategy;

import com.example.javaoffer.exam.dto.*;
import com.example.javaoffer.exam.enums.ExamMode;

/**
 * Интерфейс для стратегий обработки экзаменационных запросов различных режимов тестирования.
 * <p>
 * Реализует паттерн "Стратегия", позволяя выбирать алгоритм обработки экзаменационных запросов
 * в зависимости от выбранного режима экзамена (свободный режим, рейтинговый режим и т.д.).
 * 
 * <p>
 * Каждая реализация этого интерфейса должна обрабатывать специфические для режима логики:
 * получение следующего вопроса, проверка ответа пользователя и завершение экзамена.
 * 
 *
 * @author Garbuzov Oleg
 */
public interface ExamModeStrategy {

	/**
	 * Возвращает режим экзамена, поддерживаемый данной стратегией.
	 * 
	 * @return режим экзамена, который обрабатывает данная стратегия
	 */
	ExamMode getSupportedMode();

	/**
	 * Обрабатывает запрос на получение следующего вопроса в экзамене.
	 * 
	 * @param examRequest запрос с информацией о текущем состоянии экзамена
	 * @return ответ с данными о следующем вопросе и текущей статистике экзамена
	 */
	ExamResumeResponseDTO nextQuestionExamProcess(ExamNextQuestionRequestDTO examRequest);

	/**
	 * Обрабатывает запрос на проверку ответа пользователя.
	 * 
	 * @param examRequest запрос с выбранным пользователем ответом
	 * @return результат проверки ответа с правильным ответом и объяснением
	 */
	ValidateAnswerResponseDTO answerProcess(ExamCheckAnswerRequestDTO examRequest);

	/**
	 * Обрабатывает запрос на прерывание экзамена.
	 *
	 * @param examAbortRequestDTO запрос, содержащий информацию о текущем состоянии экзамена
	 * @return результат завершения экзамена со статистикой
	 */
	ExamAbortResponseDTO abortProcess(ExamAbortRequestDTO examAbortRequestDTO);
}
