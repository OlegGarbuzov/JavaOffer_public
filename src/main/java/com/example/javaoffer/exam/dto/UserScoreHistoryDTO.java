package com.example.javaoffer.exam.dto;

import com.example.javaoffer.user.dto.UserSimpleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO для хранения и передачи истории результатов пользователя в экзаменах.
 * <p>
 * Содержит подробную информацию о попытке прохождения экзамена: данные пользователя,
 * список всех ответов, статистику правильных и неправильных ответов, затраченное время,
 * набранные очки, а также данные о нарушениях при прохождении. Используется для
 * отображения истории экзаменов пользователя и формирования аналитики.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class UserScoreHistoryDTO {
	/**
	 * Уникальный идентификатор записи истории в базе данных
	 */
	private Long id;

	/**
	 * Пользователь, которому принадлежит результат.
	 * Содержит базовую информацию о пользователе.
	 */
	private UserSimpleDTO user;

	/**
	 * Список всех ответов, данных пользователем в процессе экзамена.
	 * Позволяет детально проанализировать прохождение.
	 */
	@Builder.Default
	private List<UserAnswerDTO> userAnswers = new ArrayList<>();

	/**
	 * Дата и время создания записи истории (начала экзамена)
	 */
	private LocalDateTime createAt;

	/**
	 * Уникальный идентификатор сессии пройденного экзамена.
	 * Соответствует идентификатору, использованному в запросах во время экзамена.
	 */
	private UUID examID;

	/**
	 * Общее количество правильных ответов в экзамене
	 */
	private Integer successAnswersCountAbsolute;

	/**
	 * Общее количество неправильных ответов в экзамене
	 */
	private Integer failAnswersCountAbsolute;

	/**
	 * Время в секундах, затраченное на прохождение всего экзамена.
	 * Измеряется от начала до завершения или прерывания экзамена.
	 */
	private double timeTakenToComplete;

	/**
	 * Количество базовых очков за ответы, без учета бонусов.
	 * Зависит от сложности вопросов и правильности ответов.
	 */
	private Integer totalBasePoints;

	/**
	 * Итоговое количество набранных очков в экзамене,
	 * включая базовые очки и все примененные бонусы.
	 */
	private Long score;

	/**
	 * Бонус к очкам за скорость прохождения экзамена.
	 * Применяется для поощрения более быстрого прохождения.
	 */
	private Double bonusByTime;

	/**
	 * Количество нарушений типа "выход с вкладки".
	 * Фиксируется, когда пользователь переключается на другую вкладку браузера.
	 */
	private Integer tabSwitchViolations;

	/**
	 * Количество нарушений типа "копирование текста".
	 * Фиксируется при попытке скопировать текст с вопросом или вариантами ответов.
	 */
	private Integer textCopyViolations;

	/**
	 * Количество пропущенных проверок связи с античитом.
	 * Фиксируется, когда клиент не отправляет регулярные heartbeat-сообщения.
	 */
	private Integer heartbeatMissedViolations;

	/**
	 * Количество нарушений типа "использование инструментов разработчика".
	 * Фиксируется при открытии DevTools браузера.
	 */
	private Integer devToolsViolations;

	/**
	 * Количество нарушений типа "вмешательство в DOM".
	 * Фиксируется при попытке изменить структуру страницы.
	 */
	private Integer domTamperingViolations;

	/**
	 * Количество нарушений типа "подмена функций".
	 * Фиксируется при перезаписи JavaScript-функций системы проверки.
	 */
	private Integer functionTamperingViolations;

	/**
	 * Количество нарушений типа "отключение или модификация модулей".
	 * Фиксируется при вмешательстве в работу JavaScript-модулей системы.
	 */
	private Integer moduleTamperingViolations;

	/**
	 * Количество нарушений типа "закрытие страницы".
	 * Фиксируется при попытке закрыть страницу экзамена.
	 */
	private Integer pageCloseViolations;

	/**
	 * Количество нарушений типа "внедрение внешнего контента".
	 * Фиксируется при попытке загрузить внешние ресурсы.
	 */
	private Integer externalContentViolations;

	/**
	 * Количество нарушений типа "вмешательство в работу модуля Anti-OCR".
	 * Фиксируется при попытке отключить или обойти защиту от распознавания текста.
	 */
	private Integer antiOcrTamperingViolations;

	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за нарушений правил.
	 * True - если экзамен был прерван из-за достижения лимита нарушений.
	 */
	private Boolean terminatedByViolations;

	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за превышения
	 * допустимого количества неверных ответов.
	 */
	private Boolean terminatedByFailAnswerCount;

	/**
	 * Текстовое описание причины прерывания экзамена, если применимо.
	 * Содержит подробную информацию о том, почему экзамен был прерван.
	 */
	private String terminationReason;
}
