package com.example.javaoffer.exam.anticheat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO для запроса статуса сессии экзамена в рамках системы анти-чита.
 * <p>
 * Этот класс используется для передачи информации о текущем состоянии сессии экзамена
 * от клиента к серверу. Содержит данные, необходимые для проверки валидности сессии
 * и отслеживания временных параметров для выявления возможных нарушений.
 * 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusRequestDTO {
	/**
	 * Уникальный идентификатор экзамена.
	 * Используется для связи события с конкретным экзаменом.
	 */
	@NotNull
	private UUID examId;

	/**
	 * ID текущего вопроса в экзамене.
	 * Позволяет определить, на каком вопросе находится пользователь.
	 */
	@NotNull
	private Long questionId;

	/**
	 * Токен аутентификации сессии.
	 * Используется для проверки подлинности запроса и связи с конкретной сессией.
	 */
	@NotNull
	private String token;

	/**
	 * Смещение часового пояса клиента в минутах относительно UTC.
	 * Позволяет учитывать разницу во времени между клиентом и сервером.
	 */
	private Integer timezoneOffset;

	/**
	 * Строковое представление часового пояса клиента.
	 * Содержит информацию о часовом поясе в формате, предоставляемом браузером.
	 */
	private String timezoneString;

	/**
	 * Время клиента в миллисекундах (timestamp).
	 * Используется для сравнения с серверным временем и выявления
	 * потенциальных манипуляций со временем.
	 */
	@NotNull
	private Long clientTime;
} 