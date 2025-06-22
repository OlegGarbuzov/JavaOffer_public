package com.example.javaoffer.exam.dto;

import com.example.javaoffer.exam.enums.ExamMode;
import lombok.Builder;
import lombok.Data;

/**
 * DTO для передачи данных ответа на запрос завершения экзамена.
 * <p>
 * Содержит статистику экзамена, включая количество правильных и неправильных ответов,
 * затраченное время, набранные очки и бонусы. Используется для отображения
 * итоговых результатов пользователю после завершения экзамена.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
public class ExamAbortResponseDTO {

	/**
	 * Режим экзамена (FREE, RATING и т.д.).
	 * Определяет, какие правила и ограничения применялись во время экзамена.
	 */
	private final ExamMode examMode;

	/**
	 * Общее количество правильных ответов за всю сессию экзамена.
	 */
	private final int successAnswersCountAbsolute;

	/**
	 * Общее количество неправильных ответов за всю сессию экзамена.
	 */
	private final int failAnswersCountAbsolute;

	/**
	 * Время в секундах, затраченное пользователем на прохождение экзамена.
	 * Учитывается период от начала экзамена до его завершения.
	 */
	private final double timeTakenToComplete;

	/**
	 * Количество базовых очков за ответы.
	 * Рассчитывается на основе сложности вопросов и правильности ответов.
	 */
	private int totalBasePoints;

	/**
	 * Бонус за время прохождения экзамена.
	 * Применяется для стимулирования более быстрого прохождения.
	 */
	private double bonusByTime;

	/**
	 * Итоговое количество набранных очков в экзамене.
	 * Рассчитывается как сумма базовых очков и бонуса за время.
	 */
	private long score;
}
