package com.example.javaoffer.exam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO для запроса на прерывание экзамена.
 * <p>
 * Используется при отправке запроса на досрочное завершение экзамена
 * пользователем или системой (в случае обнаружения нарушений).
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamAbortRequestDTO {

	/**
	 * Уникальный идентификатор сессии экзамена, который необходимо прервать
	 */
	@NotNull
	private UUID examId;
}
