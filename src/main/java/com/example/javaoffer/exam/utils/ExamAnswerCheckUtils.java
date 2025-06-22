package com.example.javaoffer.exam.utils;

import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.dto.AnswerDTO;
import com.example.javaoffer.exam.dto.ExamCheckAnswerRequestDTO;
import com.example.javaoffer.exam.dto.ExamNextQuestionRequestDTO;
import com.example.javaoffer.exam.dto.ValidateAnswerResponseDTO;
import com.example.javaoffer.exam.exception.InvalidRequestIdException;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Утилитный класс для проверки и валидации ответов в процессе экзамена.
 * <p>
 * Содержит статические методы для проверки дублирующих запросов,
 * валидации идентификаторов запросов и построения ответов.
 * Обеспечивает целостность процесса экзамена и предотвращает
 * некорректные операции.
 * 
 *
 * @author Garbuzov Oleg
 */
@Slf4j
public final class ExamAnswerCheckUtils {

	/**
	 * Приватный конструктор для предотвращения создания экземпляров утилитного класса.
	 */
	private ExamAnswerCheckUtils() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

	/**
	 * Проверяет, является ли запрос на проверку ответа дублирующим.
	 * <p>
	 * Сравнивает идентификатор запроса с последним обработанным запросом
	 * для предотвращения повторной обработки того же ответа.
	 * 
	 *
	 * @param examRequest запрос на проверку ответа
	 * @param progress    текущий прогресс экзамена
	 * @param examId      идентификатор экзамена
	 * @return true, если запрос является дублирующим
	 */
	public static boolean requestAnswerCheckIsDuplicate(ExamCheckAnswerRequestDTO examRequest, TemporaryExamProgress progress, UUID examId) {
		if (Objects.equals(progress.getLastAnswerCheckRequestId(), examRequest.getRequestId())) {
			log.debug("examId={}: Обнаружен дублирующий запрос на проверку ответа, статистика не обновлена", examId);
			return true;
		}
		return false;
	}

	/**
	 * Валидирует идентификатор запроса на проверку ответа.
	 * <p>
	 * Проверяет соответствие идентификатора запроса ожидаемому значению
	 * для обеспечения корректной последовательности операций экзамена.
	 * 
	 *
	 * @param examId      идентификатор экзамена
	 * @param progress    текущий прогресс экзамена
	 * @param examRequest запрос на проверку ответа
	 * @throws InvalidRequestIdException если идентификатор запроса некорректен
	 */
	public static void answerCheckValidationRequestID(UUID examId, TemporaryExamProgress progress, ExamCheckAnswerRequestDTO examRequest) {
		if (!Objects.equals(progress.getNextAnswerCheckRequestId(), examRequest.getRequestId())) {
			log.debug("examId={}: Не валидный id запроса. NextAnswerCheckRequestId:{}, ClientRequestId:{}",
					examId, progress.getNextAnswerCheckRequestId(), examRequest.getRequestId());
			throw new InvalidRequestIdException("Ошибка запроса. Не корректный запрос");
		}
	}

	/**
	 * Валидирует идентификатор запроса на получение следующего вопроса.
	 * <p>
	 * Проверяет соответствие идентификатора запроса ожидаемому значению
	 * для обеспечения корректной последовательности операций экзамена.
	 * 
	 *
	 * @param examId      идентификатор экзамена
	 * @param progress    текущий прогресс экзамена
	 * @param examRequest запрос на получение следующего вопроса
	 * @throws InvalidRequestIdException если идентификатор запроса некорректен
	 */
	public static void nextQuestionValidationRequestID(UUID examId, TemporaryExamProgress progress, ExamNextQuestionRequestDTO examRequest) {
		if (!Objects.equals(progress.getNextQuestionRequestId(), examRequest.getRequestId())) {
			log.debug("examId={}: Не валидный id запроса", examId);
			throw new InvalidRequestIdException("Ошибка запроса. Не корректный запрос");
		}
	}

	/**
	 * Строит ответ для дублирующего запроса на проверку ответа.
	 * <p>
	 * Возвращает ответ с корректным идентификатором следующего запроса
	 * на основе текущего состояния прогресса экзамена.
	 * 
	 *
	 * @param progress текущий прогресс экзамена
	 * @param response базовый ответ для модификации
	 * @return модифицированный ответ с корректным идентификатором запроса
	 */
	public static ValidateAnswerResponseDTO buildResponseForDuplicateRequestAnswerCheck(TemporaryExamProgress progress, ValidateAnswerResponseDTO response) {
		UUID nextQuestionRequestId = progress.getNextQuestionRequestId();
		UUID lastQuestionRequestId = progress.getLastQuestionRequestId();

		if (nextQuestionRequestId != null) {
			response.setRequestId(nextQuestionRequestId);
		} else {
			response.setRequestId(lastQuestionRequestId);
		}

		return response;
	}

	/**
	 * Создает новый ответ для валидации ответа пользователя.
	 * <p>
	 * Строит объект ответа с информацией о правильном ответе,
	 * объяснением и результатом проверки пользовательского выбора.
	 * 
	 *
	 * @param correctAnswer правильный ответ на вопрос
	 * @param isCorrect     результат проверки выбора пользователя
	 * @return новый объект ответа с результатами валидации
	 */
	public static ValidateAnswerResponseDTO buildNewValidateAnswerResponseDTO(AnswerDTO correctAnswer, boolean isCorrect) {
		return ValidateAnswerResponseDTO.builder()
				.id(correctAnswer.getId())
				.content(correctAnswer.getContent())
				.explanation(correctAnswer.getExplanation())
				.userChooseIsCorrect(isCorrect)
				.build();
	}

}
