package com.example.javaoffer.exam.cache;

import com.example.javaoffer.exam.dto.UserAnswerDTO;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Представляет временный прогресс выполнения экзамена, хранящийся в кэше.
 * <p>
 * Этот класс содержит полную информацию о текущем состоянии сессии экзамена, включая:
 * <ul>
 *     <li>Основные параметры экзамена (режим, сложность)</li>
 *     <li>Статистику ответов пользователя</li>
 *     <li>Идентификаторы запросов для обеспечения идемпотентности</li>
 *     <li>Данные системы античита</li>
 *     <li>Информацию о heartbeat-сообщениях</li>
 * </ul>
 * Используется для временного хранения данных в сессии пользователя.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Slf4j
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class TemporaryExamProgress {
	// --- Основные параметры экзамена ---
	
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

	// --- Информация о вопросах и ответах ---
	
	/**
	 * ID последнего вопроса, показанного пользователю
	 */
	private Long lastTaskId;

	/**
	 * Время выдачи последнего вопроса
	 */
	private Instant timeOfLastQuestion;

	/**
	 * Список ID вопросов, на которые пользователь правильно ответил
	 */
	private List<Long> correctlyAnsweredQuestionsId;

	/**
	 * Список ответов, данных пользователем в процессе экзамена
	 */
	private List<UserAnswerDTO> userAnswers;

	// --- Статистика ответов ---
	
	/**
	 * Счетчик неправильных ответов подряд (сбрасывается при изменении сложности)
	 */
	private int failAnswersCount;

	/**
	 * Счетчик правильных ответов подряд (сбрасывается при изменении сложности)
	 */
	private int successAnswersCount;

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

	// --- Идентификаторы запросов для обеспечения идемпотентности ---
	
	/**
	 * ID последнего обработанного запроса на получение вопроса
	 */
	private UUID lastQuestionRequestId;

	/**
	 * ID для следующего запроса, который клиент должен использовать для получения нового вопроса
	 */
	private UUID nextQuestionRequestId;

	/**
	 * ID последнего обработанного запроса для проверки ответа
	 */
	private UUID lastAnswerCheckRequestId;

	/**
	 * ID для следующего запроса, который клиент должен использовать для проверки ответа
	 */
	private UUID nextAnswerCheckRequestId;

	// --- Нарушения и система античита ---
	
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
	 * Счетчик нарушений: внедрение внешнего контента
	 */
	private int externalContentViolationCount;

	/**
	 * Счетчик нарушений: вмешательство в работу модуля Anti-OCR
	 */
	private int antiOcrTamperingViolationCount;

	// --- Флаги состояния экзамена ---
	
	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за нарушений
	 */
	private boolean terminatedByViolations;

	/**
	 * Флаг, указывающий, был ли экзамен прерван из-за превышения лимита неверных ответов
	 */
	private boolean terminatedByFailAnswerCount;

	// --- Информация о сессии и heartbeat ---
	
	/**
	 * Последний действительный токен сессии
	 */
	private String lastSessionToken;

	/**
	 * Время последнего успешного heartbeat-запроса
	 */
	private Instant lastHeartbeatTime;

	/**
	 * Ожидаемое время следующего heartbeat-запроса
	 */
	private Instant nextExpectedHeartbeatTime;
}
