package com.example.javaoffer.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO для запроса проверки ответа в экзамене.
 * <p>
 * Используется, когда пользователь выбрал вариант ответа и отправил его на проверку.
 * Содержит идентификатор экзамена, выбранный ответ и идентификатор запроса для 
 * предотвращения дублирования.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamCheckAnswerRequestDTO {

	/**
	 * Уникальный идентификатор сессии экзамена.
	 * Используется для связки с текущим прогрессом экзамена в кэше.
	 */
	@NotNull
	private UUID examId;

	/**
	 * Идентификатор выбранного пользователем варианта ответа на текущий вопрос.
	 * Соответствует полю id в AnswerDTO.
	 */
	@NotNull
	private Long selectedAnswer;

	/**
	 * Уникальный идентификатор запроса для предотвращения дублирования.
	 * Позволяет избежать повторной обработки одного и того же запроса.
	 */
	@NotNull
	private UUID requestId;

}
