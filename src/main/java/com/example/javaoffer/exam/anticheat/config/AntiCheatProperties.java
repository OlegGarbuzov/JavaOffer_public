package com.example.javaoffer.exam.anticheat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационные свойства для механизма античита.
 * <p>
 * Этот класс загружает и хранит параметры конфигурации для механизма античита
 * из файла настроек приложения с префиксом "exam.anticheat".
 * 
 */
@Component
@Data
@ConfigurationProperties("exam.anticheat")
public class AntiCheatProperties {
	/**
	 * Максимально допустимое количество нарушений типа "выход с вкладки"
	 */
	private int maxTabSwitchViolations = 3;

	/**
	 * Максимально допустимое количество нарушений типа "копирование текста"
	 */
	private int maxTextCopyViolations = 3;

	/**
	 * Максимально допустимое количество пропущенных heartbeat-запросов
	 */
	private int maxHeartbeatMissed = 3;

	/**
	 * Максимально допустимое количество нарушений типа "вмешательство в работу приложения"
	 */
	private int maxTamperingViolations = 2;

	/**
	 * Минимальный интервал между heartbeat-запросами (мс)
	 */
	private int minHeartbeatInterval = 5000;

	/**
	 * Максимальный интервал между heartbeat-запросами (мс)
	 */
	private int maxHeartbeatInterval = 10000;

	/**
	 * Допустимое отклонение времени heartbeat-запроса (мс)
	 */
	private int heartbeatTolerance = 2000;

	/**
	 * Секретный ключ для шифрования токенов
	 */
	private String tokenSecret = "defaultSecretKey";

	/**
	 * Время жизни токена (секунды)
	 */
	private int tokenValiditySeconds = 60;
} 