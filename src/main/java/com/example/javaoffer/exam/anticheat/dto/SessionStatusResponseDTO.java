package com.example.javaoffer.exam.anticheat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа на запрос статуса сессии экзамена.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusResponseDTO {
	/**
	 * Новый токен для следующего запроса
	 */
	private String nextToken;

	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за нарушений
	 */
	private boolean examTerminatedByViolation;

	/**
	 * Задача для выполнения на клиенте (опционально)
	 */
	private String challenge;
} 