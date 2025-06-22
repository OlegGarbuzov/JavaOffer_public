package com.example.javaoffer.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO для запроса следующего вопроса в экзамене.
 * <p>
 * Используется при инициализации нового экзамена или для запроса следующего
 * вопроса в ходе уже начатого экзамена.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamNextQuestionRequestDTO {

	/**
	 * Уникальный идентификатор сессии экзамена.
	 * Используется для связки с текущим прогрессом экзамена в кэше.
	 */
	@NotNull
	private UUID examId;

	/**
	 * Уникальный идентификатор запроса для предотвращения дублирования.
	 * Позволяет избежать повторной обработки одного и того же запроса.
	 */
	@NotNull
	private UUID requestId;

}
