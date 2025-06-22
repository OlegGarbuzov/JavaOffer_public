package com.example.javaoffer.exam.dto;

import com.example.javaoffer.exam.entity.UserScoreHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * DTO для передачи данных об ответе пользователя на вопрос экзамена.
 * <p>
 * Содержит информацию о выбранном пользователем варианте ответа, связанном вопросе,
 * признаке правильности ответа и затраченном времени. Используется для хранения истории
 * ответов пользователя и формирования статистики.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UserAnswerDTO {
	/**
	 * Уникальный идентификатор записи ответа пользователя в базе данных
	 */
	private Long id;

	/**
	 * История прохождения экзамена, к которой относится данный ответ.
	 * Связывает ответ с конкретной попыткой прохождения экзамена.
	 */
	private UserScoreHistory userScoreHistory;

	/**
	 * Задание (вопрос), на которое был дан ответ.
	 * Содержит текст вопроса, варианты ответа и другие характеристики.
	 */
	private TaskDTO taskDTO;

	/**
	 * Вариант ответа, выбранный пользователем.
	 * Содержит текст и идентификатор варианта ответа.
	 */
	private AnswerDTO answerDTO;

	/**
	 * Признак правильности ответа пользователя.
	 * True - если пользователь выбрал правильный вариант, false - если неправильный.
	 */
	private boolean isCorrect;

	/**
	 * Время в секундах, затраченное пользователем на ответ.
	 * Измеряется от момента показа вопроса до момента выбора ответа.
	 */
	private double timeTakenSeconds;
}
