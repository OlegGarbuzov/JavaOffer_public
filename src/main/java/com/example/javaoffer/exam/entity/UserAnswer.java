package com.example.javaoffer.exam.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность, представляющая ответ пользователя на вопрос экзамена.
 * <p>
 * Связывает пользователя, задание, выбранный ответ и историю прохождения экзамена.
 * Хранит информацию о правильности ответа и времени, затраченном на ответ.
 * Используется для детального анализа прохождения экзамена и формирования статистики.
 * 
 *
 * @author Garbuzov Oleg
 */
@Getter
@Setter
@Entity
@Table(name = "user_answers")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {
	/**
	 * Уникальный идентификатор записи ответа пользователя в базе данных
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_answers_seq_gen")
	@SequenceGenerator(name = "user_answers_seq_gen", sequenceName = "user_answers_seq", allocationSize = 1)
	private Long id;

	/**
	 * История прохождения экзамена, к которой относится данный ответ.
	 * Связывает ответ с конкретной попыткой прохождения экзамена.
	 * Обратная сторона связи @OneToMany в классе UserScoreHistory.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_score_history_id", nullable = false)
	private UserScoreHistory userScoreHistory;

	/**
	 * Задание (вопрос), на которое был дан ответ.
	 * Содержит ссылку на вопрос в базе данных.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id", nullable = false)
	private Task task;

	/**
	 * Вариант ответа, выбранный пользователем.
	 * Содержит ссылку на выбранный вариант ответа в базе данных.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "answer_id", nullable = false)
	private Answer answer;

	/**
	 * Признак правильности ответа пользователя.
	 * True - если пользователь выбрал правильный вариант, false - если неправильный.
	 * Дублирует информацию из связанного answer.isCorrect для удобства запросов.
	 */
	@Column(name = "is_correct")
	private boolean isCorrect;

	/**
	 * Время в секундах, затраченное пользователем на ответ.
	 * Измеряется от момента показа вопроса до момента выбора ответа.
	 * Используется для анализа и статистики.
	 */
	@Column(name = "time_taken_seconds")
	private double timeTakenSeconds;
}
