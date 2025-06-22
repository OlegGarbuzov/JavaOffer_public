package com.example.javaoffer.exam.entity;

import com.example.javaoffer.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Сущность истории результатов пользователя в экзаменах.
 * <p>
 * Хранит подробную информацию о каждой попытке прохождения экзамена пользователем,
 * включая статистику ответов, набранные баллы, затраченное время и данные о возможных
 * нарушениях. Каждая запись представляет одну завершенную попытку прохождения экзамена.
 *
 * @author Garbuzov Oleg
 */
@Getter
@Setter
@Entity
@Table(name = "user_score_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserScoreHistory {
	/**
	 * Уникальный идентификатор записи истории в базе данных
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_score_history_seq_gen")
	@SequenceGenerator(name = "user_score_history_seq_gen", sequenceName = "user_score_history_seq", allocationSize = 1)
	private Long id;

	/**
	 * Пользователь, которому принадлежит результат прохождения экзамена.
	 * При удалении пользователя каскадно удаляются все его результаты.
	 */
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private User user;

	/**
	 * Список ответов, данных пользователем в процессе экзамена.
	 * Каждый элемент содержит информацию об одном ответе: вопрос, выбранный вариант,
	 * правильность ответа и затраченное время.
	 */
	@Builder.Default
	@OneToMany(mappedBy = "userScoreHistory", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserAnswer> userAnswers = new ArrayList<>();

	/**
	 * Дата и время начала экзамена.
	 * Устанавливается автоматически при создании записи.
	 */
	@Column(name = "create_at", nullable = false)
	private LocalDateTime createAt;

	/**
	 * Уникальный идентификатор сессии экзамена.
	 * Соответствует идентификатору, используемому в запросах во время прохождения экзамена.
	 */
	@Column(name = "exam_id", nullable = false)
	private UUID examID;

	/**
	 * Общее количество правильных ответов в экзамене.
	 * Используется для расчета статистики и определения результата.
	 */
	@Column(name = "success_answers_count_absolute", nullable = false)
	private Integer successAnswersCountAbsolute;

	/**
	 * Общее количество неправильных ответов в экзамене.
	 * Используется для расчета статистики и определения результата.
	 */
	@Column(name = "fail_answers_count_absolute", nullable = false)
	private Integer failAnswersCountAbsolute;

	/**
	 * Время в секундах, затраченное на прохождение всего экзамена.
	 * Измеряется от начала до завершения или прерывания экзамена.
	 */
	@Column(name = "time_taken_to_complete", nullable = false)
	private double timeTakenToComplete;

	/**
	 * Количество базовых очков за ответы, без учета бонусов.
	 * Зависит от сложности вопросов и правильности ответов.
	 */
	@Column(name = "total_base_points", nullable = false)
	private Integer totalBasePoints;

	/**
	 * Бонус к очкам за скорость прохождения экзамена.
	 * Применяется для поощрения более быстрого прохождения.
	 */
	@Column(name = "bonus_time", nullable = false)
	private Double bonusByTime;

	/**
	 * Итоговое количество набранных очков в экзамене,
	 * включая базовые очки и все примененные бонусы.
	 */
	@Column(name = "score", nullable = false)
	private Long score;

	// Раздел статистики нарушений

	/**
	 * Количество нарушений типа "выход с вкладки".
	 * Фиксируется, когда пользователь переключается на другую вкладку браузера.
	 */
	@Column(name = "tab_switch_violations")
	private Integer tabSwitchViolations;

	/**
	 * Количество нарушений типа "копирование текста".
	 * Фиксируется при попытке скопировать текст с вопросом или вариантами ответов.
	 */
	@Column(name = "text_copy_violations")
	private Integer textCopyViolations;

	/**
	 * Количество пропущенных проверок связи с античитом.
	 * Фиксируется, когда клиент не отправляет регулярные heartbeat-сообщения.
	 */
	@Column(name = "heartbeat_missed_violations")
	private Integer heartbeatMissedViolations;

	/**
	 * Количество нарушений типа "использование инструментов разработчика".
	 * Фиксируется при открытии DevTools браузера.
	 */
	@Column(name = "dev_tools_violations")
	private Integer devToolsViolations;

	/**
	 * Количество нарушений типа "вмешательство в DOM".
	 * Фиксируется при попытке изменить структуру страницы.
	 */
	@Column(name = "dom_tampering_violations")
	private Integer domTamperingViolations;

	/**
	 * Количество нарушений типа "подмена функций".
	 * Фиксируется при перезаписи JavaScript-функций системы проверки.
	 */
	@Column(name = "function_tampering_violations")
	private Integer functionTamperingViolations;

	/**
	 * Количество нарушений типа "отключение или модификация модулей".
	 * Фиксируется при вмешательстве в работу JavaScript-модулей системы.
	 */
	@Column(name = "module_tampering_violations")
	private Integer moduleTamperingViolations;

	/**
	 * Количество нарушений типа "закрытие страницы".
	 * Фиксируется при попытке закрыть страницу экзамена.
	 */
	@Column(name = "page_close_violations")
	private Integer pageCloseViolations;

	/**
	 * Количество нарушений типа "внедрение внешнего контента".
	 * Фиксируется при попытке загрузить внешние ресурсы.
	 */
	@Column(name = "external_content_violations")
	private Integer externalContentViolations;

	/**
	 * Количество нарушений типа "вмешательство в работу модуля Anti-OCR".
	 * Фиксируется при попытке отключить или обойти защиту от распознавания текста.
	 */
	@Column(name = "anti_ocr_tampering_violations")
	private Integer antiOcrTamperingViolations;

	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за нарушений правил.
	 * True - если экзамен был прерван из-за достижения лимита нарушений.
	 */
	@Column(name = "terminated_by_violations")
	private Boolean terminatedByViolations;

	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за превышения
	 * допустимого количества неверных ответов.
	 */
	@Column(name = "terminated_by_fail_answer_count")
	private Boolean terminatedByFailAnswerCount;

	/**
	 * Текстовое описание причины прерывания экзамена, если применимо.
	 * Содержит подробную информацию о том, почему экзамен был прерван.
	 */
	@Column(name = "termination_reason")
	private String terminationReason;

	/**
	 * Метод, вызываемый перед сохранением объекта в базу данных.
	 * Устанавливает дату и время создания записи истории.
	 * Вызывается автоматически при первом сохранении сущности в базу данных.
	 */
	@PrePersist
	protected void onCreate() {
		this.createAt = LocalDateTime.now();
	}

	/**
	 * Сравнивает объекты с учетом особенностей Hibernate Proxy.
	 * <p>
	 * Метод переопределен для корректной работы с Hibernate-прокси объектами.
	 * Проверяет только на равенство идентификаторов, если они установлены.
	 *
	 * @param o объект для сравнения
	 * @return true если объекты представляют одну и ту же сущность
	 */
	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
		Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
		if (thisEffectiveClass != oEffectiveClass) return false;
		UserScoreHistory userScoreHistory = (UserScoreHistory) o;
		return getId() != null && Objects.equals(getId(), userScoreHistory.getId());
	}

	/**
	 * Вычисляет хеш-код объекта с учетом особенностей Hibernate Proxy.
	 * <p>
	 * Метод переопределен для корректной работы с Hibernate-прокси объектами.
	 * Использует класс объекта для вычисления хеш-кода.
	 *
	 * @return хеш-код объекта
	 */
	@Override
	public final int hashCode() {
		return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
	}
}
