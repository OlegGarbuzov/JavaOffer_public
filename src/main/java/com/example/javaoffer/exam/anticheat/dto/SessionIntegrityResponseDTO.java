package com.example.javaoffer.exam.anticheat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа на события целостности сессии экзамена.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionIntegrityResponseDTO {
	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за нарушений
	 */
	private boolean examTerminatedByViolation;
} 