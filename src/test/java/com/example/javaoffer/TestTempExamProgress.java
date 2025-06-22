package com.example.javaoffer;

import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Утилитный класс для создания тестовых объектов TemporaryExamProgress.
 * <p>
 * Класс предоставляет набор статических методов для создания различных конфигураций
 * временного прогресса экзамена, используемых в тестах. Каждый метод создает объект
 * с предустановленными значениями, соответствующими определенному тестовому сценарию.
 * </p>
 * <p>
 * Поддерживаемые тестовые сценарии:
 * <ul>
 *   <li>Базовые сценарии с различными уровнями сложности</li>
 *   <li>Сценарии с настраиваемыми параметрами запросов</li>
 *   <li>Сценарии с предопределенными правильными ответами</li>
 *   <li>Сценарии с различными счетчиками успешных/неуспешных ответов</li>
 * </ul>
 * </p>
 *
 * @author Garbuzov Oleg
 */
@Slf4j
public final class TestTempExamProgress {

	/**
	 * Приватный конструктор для предотвращения создания экземпляров утилитного класса.
	 */
	private TestTempExamProgress() {
		throw new AssertionError("Utility class cannot be instantiated");
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с базовой конфигурацией №1.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Уровень сложности: Easy2</li>
	 *   <li>Успешные ответы: 5, неуспешные: 1</li>
	 *   <li>Базовые баллы: 0</li>
	 *   <li>Нет нарушений античит-системы</li>
	 * </ul>
	 * </p>
	 * 
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData1(ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(0L)
				.currentDifficulty(TaskDifficulty.Easy2)
				.successAnswersCount(5)
				.failAnswersCount(1)
				.successAnswersCountAbsolute(2)
				.failAnswersCountAbsolute(1)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(0)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.nextQuestionRequestId(UUID.randomUUID())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с базовой конфигурацией №2.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Уровень сложности: Easy3</li>
	 *   <li>Успешные ответы: 1, неуспешные: 3</li>
	 *   <li>Базовые баллы: 0</li>
	 *   <li>Нет нарушений античит-системы</li>
	 * </ul>
	 * </p>
	 * 
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData2(ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(0L)
				.currentDifficulty(TaskDifficulty.Easy3)
				.successAnswersCount(1)
				.failAnswersCount(3)
				.successAnswersCountAbsolute(1)
				.failAnswersCountAbsolute(1)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(0)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.nextQuestionRequestId(UUID.randomUUID())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с базовой конфигурацией №3.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Уровень сложности: Easy1</li>
	 *   <li>Успешные ответы: 1, неуспешные: 3</li>
	 *   <li>Базовые баллы: 0</li>
	 *   <li>Нет нарушений античит-системы</li>
	 * </ul>
	 * </p>
	 * 
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData3(ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(0L)
				.currentDifficulty(TaskDifficulty.Easy1)
				.successAnswersCount(1)
				.failAnswersCount(3)
				.successAnswersCountAbsolute(1)
				.failAnswersCountAbsolute(1)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(0)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.nextQuestionRequestId(UUID.randomUUID())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с базовой конфигурацией №4.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Уровень сложности: Easy2</li>
	 *   <li>Успешные ответы: 1, неуспешные: 1</li>
	 *   <li>Базовые баллы: 0</li>
	 *   <li>Нет нарушений античит-системы</li>
	 * </ul>
	 * </p>
	 * 
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData4(ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(0L)
				.currentDifficulty(TaskDifficulty.Easy2)
				.successAnswersCount(1)
				.failAnswersCount(1)
				.successAnswersCountAbsolute(1)
				.failAnswersCountAbsolute(1)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(0)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.nextQuestionRequestId(UUID.randomUUID())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с расширенной конфигурацией №5.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Настраиваемые ID запросов для вопросов и проверки ответов</li>
	 *   <li>Настраиваемый ID последней задачи</li>
	 *   <li>Базовые баллы: 1000</li>
	 *   <li>Настроенное время heartbeat</li>
	 * </ul>
	 * </p>
	 * 
	 * @param lastQuestionRequestId ID последнего запроса вопроса
	 * @param nextQuestionRequestId ID следующего запроса вопроса
	 * @param lastAnswerCheckId ID последней проверки ответа
	 * @param nextAnswerCheckId ID следующей проверки ответа
	 * @param lastTaskId ID последней задачи
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData5(UUID lastQuestionRequestId,
												  UUID nextQuestionRequestId,
												  UUID lastAnswerCheckId,
												  UUID nextAnswerCheckId,
												  Long lastTaskId,
											  ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(lastTaskId)
				.currentDifficulty(TaskDifficulty.Easy1)
				.successAnswersCount(1)
				.failAnswersCount(0)
				.successAnswersCountAbsolute(1)
				.failAnswersCountAbsolute(0)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(0)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.lastQuestionRequestId(lastQuestionRequestId)
				.nextQuestionRequestId(nextQuestionRequestId)
				.lastAnswerCheckRequestId(lastAnswerCheckId)
				.nextAnswerCheckRequestId(nextAnswerCheckId)
				.currentBasePoint(1000)
				.timeOfLastQuestion(Instant.now())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.lastHeartbeatTime(Instant.now())
				.nextExpectedHeartbeatTime(Instant.now())
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с предустановленными правильными ответами.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Список ID правильно отвеченных вопросов</li>
	 *   <li>Настраиваемый ID последней задачи</li>
	 *   <li>Уровень сложности: Easy2</li>
	 *   <li>Обнуленные счетчики ответов</li>
	 * </ul>
	 * </p>
	 * 
	 * @param prepareCorrectlyAnsweredQuestionsId список ID правильно отвеченных вопросов
	 * @param lastTaskId ID последней задачи
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData6(List<Long> prepareCorrectlyAnsweredQuestionsId,
												  Long lastTaskId,
												  ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(lastTaskId)
				.currentDifficulty(TaskDifficulty.Easy2)
				.successAnswersCount(0)
				.failAnswersCount(0)
				.successAnswersCountAbsolute(0)
				.failAnswersCountAbsolute(0)
				.correctlyAnsweredQuestionsId(prepareCorrectlyAnsweredQuestionsId)
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(0)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с базовой конфигурацией №7.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Уровень сложности: Easy1</li>
	 *   <li>Успешные ответы: 1, неуспешные: 1</li>
	 *   <li>Базовые баллы: 1000</li>
	 *   <li>Нет нарушений античит-системы</li>
	 * </ul>
	 * </p>
	 * 
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData7(ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(0L)
				.currentDifficulty(TaskDifficulty.Easy1)
				.successAnswersCount(1)
				.failAnswersCount(1)
				.successAnswersCountAbsolute(1)
				.failAnswersCountAbsolute(1)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(1000)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с базовой конфигурацией №8.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Уровень сложности: Easy2</li>
	 *   <li>Успешные ответы: 6, неуспешные: 1</li>
	 *   <li>Базовые баллы: 100</li>
	 *   <li>Настроенное время heartbeat</li>
	 * </ul>
	 * </p>
	 * 
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData8(ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(0L)
				.currentDifficulty(TaskDifficulty.Easy2)
				.successAnswersCount(6)
				.failAnswersCount(1)
				.successAnswersCountAbsolute(2)
				.failAnswersCountAbsolute(1)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(100)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.nextQuestionRequestId(UUID.randomUUID())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.lastHeartbeatTime(Instant.now())
				.nextExpectedHeartbeatTime(Instant.now())
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с базовой конфигурацией №9.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Уровень сложности: Easy3</li>
	 *   <li>Успешные ответы: 1, неуспешные: 2</li>
	 *   <li>Базовые баллы: 100</li>
	 *   <li>Настроенное время heartbeat</li>
	 * </ul>
	 * </p>
	 * 
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData9(ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(0L)
				.currentDifficulty(TaskDifficulty.Easy3)
				.successAnswersCount(1)
				.failAnswersCount(2)
				.successAnswersCountAbsolute(2)
				.failAnswersCountAbsolute(1)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(100)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.nextQuestionRequestId(UUID.randomUUID())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.lastHeartbeatTime(Instant.now())
				.nextExpectedHeartbeatTime(Instant.now())
				.terminatedByViolations(false)
				.build();
	}

	/**
	 * Создает тестовый объект TemporaryExamProgress с базовой конфигурацией №10.
	 * <p>
	 * Конфигурация содержит:
	 * <ul>
	 *   <li>Уровень сложности: Easy1</li>
	 *   <li>Успешные ответы: 1, неуспешные: 2</li>
	 *   <li>Базовые баллы: 0</li>
	 *   <li>Нет нарушений античит-системы</li>
	 * </ul>
	 * </p>
	 * 
	 * @param examMode режим проведения экзамена
	 * @return настроенный объект TemporaryExamProgress
	 */
	public static TemporaryExamProgress testData10(ExamMode examMode) {
		return TemporaryExamProgress.builder()
				.examMode(examMode)
				.lastTaskId(0L)
				.currentDifficulty(TaskDifficulty.Easy1)
				.successAnswersCount(1)
				.failAnswersCount(2)
				.successAnswersCountAbsolute(1)
				.failAnswersCountAbsolute(1)
				.correctlyAnsweredQuestionsId(List.of())
				.userAnswers(new CopyOnWriteArrayList<>())
				.currentBasePoint(0)
				.timeOfLastQuestion(Instant.now())
				.progressCreateAt(LocalDateTime.now())
				.nextQuestionRequestId(UUID.randomUUID())
				.tabSwitchViolationCount(0)
				.textCopyViolationCount(0)
				.heartbeatMissedCount(0)
				.terminatedByViolations(false)
				.build();
	}
}
