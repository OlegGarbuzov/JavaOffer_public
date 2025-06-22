package com.example.javaoffer.exam.cache.dto;

import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * DTO для передачи клиенту избранных данных о прогрессе экзамена.
 * <p>
 * Этот класс содержит только необходимые для клиента данные о прогрессе экзамена,
 * извлеченные из {@link com.example.javaoffer.exam.cache.TemporaryExamProgress}.
 * В отличие от объекта кэша, этот DTO не содержит служебную информацию, историю
 * запросов и внутренние идентификаторы, а только следующий идентификатор запроса,
 * необходимый для обеспечения идемпотентности.
 * 
 * 
 * @author Garbuzov Oleg
 * @see com.example.javaoffer.exam.cache.TemporaryExamProgress
 */
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class TemporaryExamProgressDTO {

	/**
	 * Режим экзамена (рейтинговый или свободный)
	 */
	private ExamMode examMode;

	/**
	 * Текущая сложность экзамена
	 */
	private TaskDifficulty currentDifficulty;

	/**
	 * Общий счетчик неправильных ответов за всю сессию экзамена
	 */
	private int failAnswersCountAbsolute;

	/**
	 * Общий счетчик правильных ответов за всю сессию экзамена
	 */
	private int successAnswersCountAbsolute;

	/**
	 * Количество баллов, набранных за ответы на вопросы
	 */
	private int currentBasePoint;

	/**
	 * ID следующего запроса, с которым клиент должен обратиться к серверу.
	 * Используется для обеспечения идемпотентности операций.
	 */
	private UUID requestId;
}
