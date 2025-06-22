package com.example.javaoffer.exam.cache.dto;

import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO класс для отображения информации о прогрессе экзамена на странице администрирования.
 * <p>
 * Используется для отображения данных о текущих экзаменационных сессиях на странице
 * admin/debug/cache/progress. Содержит основную информацию о состоянии экзамена,
 * статистике ответов, нарушениях и мониторинге heartbeat-сообщений.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheDebugProgressDTO {
	/**
	 * ID экзамена в кэше
	 */
	private UUID examId;

	/**
	 * Время начала экзамена
	 */
	private LocalDateTime progressCreateAt;

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
	 * Счетчик нарушений: выход с вкладки браузера
	 */
	private int tabSwitchViolationCount;

	/**
	 * Счетчик нарушений: копирование текста
	 */
	private int textCopyViolationCount;

	/**
	 * Счетчик нарушений: пропущенные проверки связи с античитом
	 */
	private int heartbeatMissedCount;

	/**
	 * Счетчик нарушений: использование инструментов разработчика
	 */
	private int devToolsViolationCount;

	/**
	 * Счетчик нарушений: вмешательство в DOM структуру страницы
	 */
	private int domTamperingViolationCount;

	/**
	 * Счетчик нарушений: подмена JavaScript функций
	 */
	private int functionTamperingViolationCount;

	/**
	 * Счетчик нарушений: отключение/модификация JavaScript модулей
	 */
	private int moduleTamperingViolationCount;

	/**
	 * Счетчик нарушений: закрытие страницы
	 */
	private int pageCloseViolationCount;

	/**
	 * Счетчик нарушений: вмешательство в работу модуля Anti-OCR
	 */
	private int antiOcrTamperingViolations;

	/**
	 * Счетчик нарушений: внедрение внешнего контента
	 */
	private int externalContentViolationCount;

	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за нарушений
	 */
	private boolean terminatedByViolations;

	/**
	 * Продолжительность экзамена в минутах с момента его создания
	 */
	private Long durationMinutes;

	/**
	 * Количество секунд с момента последнего heartbeat-запроса
	 */
	private Long lastHeartbeatSecondsAgo;

	/**
	 * Статус heartbeat-соединения (OK, ПРОБЛЕМА, Нет данных)
	 */
	private String heartbeatStatus;
} 