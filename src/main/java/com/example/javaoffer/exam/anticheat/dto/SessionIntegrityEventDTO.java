package com.example.javaoffer.exam.anticheat.dto;

import com.example.javaoffer.exam.anticheat.enums.EventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO для отправки событий целостности сессии экзамена.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionIntegrityEventDTO {
	/**
	 * Идентификатор экзамена
	 */
	@NotNull
	private UUID examId;

	/**
	 * Тип события
	 */
	@NotNull
	@JsonProperty(value = "eventType")
	private EventType eventType;
} 