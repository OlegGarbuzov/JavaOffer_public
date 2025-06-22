package com.example.javaoffer.exam.cache.property;

import com.example.javaoffer.exam.property.FreeModeProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационные свойства для кэша прогресса экзаменов.
 * <p>
 * Этот класс загружает параметры конфигурации кэша из файла настроек
 * приложения с префиксом "cache.quiz". Параметры используются для настройки
 * максимального размера кэша и времени жизни записей.
 * 
 * <p>
 * Пример конфигурации в application.properties:
 * <pre>
 * cache.quiz.maximum-size=10000
 * cache.quiz.expire-after-write-minutes=30
 * </pre>
 * 
 *
 * @author Garbuzov Oleg
 * @see com.example.javaoffer.exam.cache.config.QuizCacheConfig
 */
@Slf4j
@Component
@Data
@ConfigurationProperties("cache.quiz")
@EnableConfigurationProperties(FreeModeProperties.class)
public class CacheProperties {
	
	/**
	 * Максимальное количество записей в кэше.
	 * Определяет, сколько активных сессий экзаменов может быть одновременно в памяти.
	 * При превышении этого значения наименее используемые записи автоматически удаляются.
	 */
	private int maximumSize;
	
	/**
	 * Время жизни записи в кэше в минутах после последней операции записи.
	 * После истечения этого времени с момента последнего обновления записи,
	 * она автоматически удаляется из кэша.
	 */
	private int expireAfterWriteMinutes;
}
