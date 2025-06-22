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
 * Унифицированный DTO для запросов статуса сессии и событий нарушений.
 * Объединяет функциональность SessionStatusRequestDTO и SessionIntegrityEventDTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedRequestDTO {
	/**
	 * Идентификатор экзамена
	 */
	@NotNull
	private UUID examId;

	/**
	 * ID текущего вопроса (для heartbeat)
	 */
	private Long questionId;

	/**
	 * Токен для heartbeat
	 */
	private String token;

	/**
	 * Тип события/запроса
	 * UI_MENU_HB_CHECK - для heartbeat
	 * Другие значения - для нарушений целостности
	 */
	@NotNull
	@JsonProperty(value = "eventType")
	private EventType eventType;

	/**
	 * Смещение часового пояса клиента в минутах (для heartbeat)
	 */
	private Integer timezoneOffset;

	/**
	 * Строковое представление часового пояса клиента (для heartbeat)
	 */
	private String timezoneString;

	/**
	 * Время клиента в миллисекундах (timestamp) (для heartbeat)
	 */
	private Long clientTime;
} 