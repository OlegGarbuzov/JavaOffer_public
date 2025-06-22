package com.example.javaoffer.exam.dto;

import com.example.javaoffer.exam.cache.dto.TemporaryExamProgressDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа на запрос возобновления или продолжения экзамена.
 * <p>
 * Предоставляет информацию о текущем вопросе, статистике прохождения экзамена, 
 * а также содержит флаги, указывающие на возможное прерывание экзамена.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResumeResponseDTO {
	/**
	 * Текущий вопрос, который должен быть показан пользователю
	 */
	private TaskDTO taskDto;

	/**
	 * Статистика прогресса текущего экзамена.
	 * Включает количество правильных/неправильных ответов, текущую сложность и т.д.
	 */
	private TemporaryExamProgressDTO stats;

	/**
	 * Результаты прерывания экзамена, если экзамен был прерван.
	 * Null, если экзамен продолжается нормально.
	 */
	private ExamAbortResponseDTO abortResults;

	/**
	 * Флаг, указывающий, что экзамен прерван из-за нарушений правил прохождения
	 * (например, использование запрещенных инструментов).
	 */
	@Builder.Default
	private boolean examTerminatedByViolation = false;

	/**
	 * Флаг, указывающий, что экзамен прерван из-за превышения допустимого
	 * количества неправильных ответов.
	 */
	@Builder.Default
	private boolean examTerminatedByFailAnswerCount = false;
}
