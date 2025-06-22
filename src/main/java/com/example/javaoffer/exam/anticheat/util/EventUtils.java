package com.example.javaoffer.exam.anticheat.util;

import com.example.javaoffer.exam.anticheat.config.AntiCheatProperties;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Random;

/**
 * Утилитный класс для работы с heartbeat-запросами.
 * Содержит методы для проверки пропущенных heartbeat и расчета допустимого отклонения.
 */
@Slf4j
public class EventUtils {
	private EventUtils() {
	}

	/**
	 * Генерирует задачу для выполнения на клиенте.
	 *
	 * @return строка с JavaScript-кодом задачи
	 */
	public static String generateChallenge(int expectedResult) {
		Random random = new Random();
		int a, b;
		int operation = random.nextInt(2); // 0 - сложение, 1 - вычитание

		if (operation == 0) {
			a = random.nextInt(expectedResult + 1);
			b = expectedResult - a;
		} else {
			a = expectedResult + random.nextInt(10000);
			b = a - expectedResult;
		}

		return (operation == 0)
				? String.format("function(x){return %d+%d;}", a, b)
				: String.format("function(x){return %d-%d;}", a, b);
	}

	/**
	 * Определяет, может ли быть проблема с часовым поясом на основе времени последнего heartbeat.
	 *
	 * @param progress             прогресс экзамена
	 * @param now                  текущее время
	 * @param maxHeartbeatInterval максимальный интервал между heartbeat
	 * @return true, если возможна проблема с часовым поясом
	 */
	public static boolean isPotentialTimezoneIssue(TemporaryExamProgress progress, Instant now, int maxHeartbeatInterval) {
		long millisSinceLastHeartbeat = now.toEpochMilli() - progress.getLastHeartbeatTime().toEpochMilli();
		return millisSinceLastHeartbeat < maxHeartbeatInterval * 1.2;
	}

	/**
	 * Рассчитывает допустимое отклонение для проверки heartbeat с учетом возможных проблем с часовым поясом.
	 *
	 * @param baseToleranceMillis      базовое допустимое отклонение
	 * @param isPotentialTimezoneIssue флаг, указывающий на возможную проблему с часовым поясом
	 * @return допустимое отклонение в миллисекундах
	 */
	public static int calculateToleranceMillis(int baseToleranceMillis, boolean isPotentialTimezoneIssue) {
		if (isPotentialTimezoneIssue) {
			return (int) (baseToleranceMillis * 1.5);
		}
		return baseToleranceMillis;
	}

	/**
	 * Проверяет, был ли пропущен heartbeat на основе временных меток.
	 *
	 * @param progress        прогресс экзамена
	 * @param now             текущее время
	 * @param toleranceMillis допустимое отклонение в миллисекундах
	 * @return true, если heartbeat был пропущен
	 */
	public static boolean isHeartbeatMissed(TemporaryExamProgress progress, Instant now, int toleranceMillis) {
		return now.isAfter(progress.getNextExpectedHeartbeatTime().plusMillis(toleranceMillis));
	}

	/**
	 * Проверяет, превышен ли лимит пропущенных heartbeat.
	 *
	 * @param missedHeartbeatsCount количество пропущенных heartbeat
	 * @param maxHeartbeatMissed    максимально допустимое количество пропущенных heartbeat
	 * @return true, если лимит превышен
	 */
	public static boolean isHeartbeatLimitExceeded(int missedHeartbeatsCount, int maxHeartbeatMissed) {
		return missedHeartbeatsCount >= maxHeartbeatMissed;
	}

	/**
	 * Проверяет, превышено ли суммарное количество нарушений, связанных с вмешательством в работу приложения.
	 * <p>
	 * Метод суммирует все типы нарушений, связанных с вмешательством в работу приложения,
	 * и сравнивает с максимально допустимым значением из настроек.
	 * 
	 *
	 * @param progress прогресс экзамена с данными о количестве нарушений
	 * @param properties настройки античита с максимально допустимыми значениями
	 * @return true, если суммарное количество нарушений превышено
	 */
	public static boolean exceededTotalTamperingViolation(TemporaryExamProgress progress, AntiCheatProperties properties) {
		return (progress.getDevToolsViolationCount() +
				progress.getDomTamperingViolationCount() +
				progress.getFunctionTamperingViolationCount() +
				progress.getModuleTamperingViolationCount() +
				progress.getPageCloseViolationCount() +
				progress.getExternalContentViolationCount() +
				progress.getAntiOcrTamperingViolationCount())

				>= properties.getMaxTamperingViolations();
	}
} 