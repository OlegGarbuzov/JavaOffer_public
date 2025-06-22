package com.example.javaoffer.exam.cache.config;

import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.property.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.javaoffer.common.constants.JpaCacheName.*;

/**
 * Конфигурация кэша для хранения временного прогресса экзаменов и других часто используемых данных.
 * <p>
 * Использует библиотеку Caffeine для реализации высокопроизводительного кэша в памяти,
 * который автоматически удаляет записи по истечении времени их жизни или при достижении
 * максимального размера кэша.
 * 
 * <p>
 * Параметры кэша (максимальный размер и время жизни) для прогресса экзаменов настраиваются через
 * свойства приложения, определенные в {@link CacheProperties}.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Slf4j
@Configuration
@EnableCaching
public class QuizCacheConfig {

	private final CacheProperties cacheProperties;

	public QuizCacheConfig(CacheProperties cacheProperties) {
		this.cacheProperties = cacheProperties;
		log.debug("Инициализирована конфигурация кэша с параметрами: maximumSize={}, expireAfterWriteMinutes={}", 
				cacheProperties.getMaximumSize(), cacheProperties.getExpireAfterWriteMinutes());
	}

	/**
	 * Создает и настраивает CacheManager для аннотационного кеширования Spring.
	 * <p>
	 * Настраивает следующие кэши:
	 * <ul>
	 *   <li>Кэш для заданий по сложности с временем жизни 30 минут</li>
	 *   <li>Кэш для топ-5 глобального рейтинга с временем жизни 60 минут</li>
	 *   <li>Кэш для полного глобального рейтинга с временем жизни 60 минут</li>
	 * </ul>
	 * 
	 * 
	 * @return настроенный Spring CacheManager
	 */
	@Bean
	public CacheManager cacheManager() {
		SimpleCacheManager manager = new SimpleCacheManager();

		CaffeineCache allTasksByDifficultyCache = new CaffeineCache(CACHE_NAME_ALL_TASK_BY_DIFFICULTY,
				Caffeine.newBuilder()
						.maximumSize(5000)
						.expireAfterWrite(30, TimeUnit.MINUTES)
						.build());
		log.debug("Создан кэш {} с максимальным размером 5000 и временем жизни 30 минут", CACHE_NAME_ALL_TASK_BY_DIFFICULTY);
		
		CaffeineCache top5ByScore = new CaffeineCache(CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE,
				Caffeine.newBuilder()
						.maximumSize(5)
						.expireAfterWrite(60, TimeUnit.MINUTES)
						.build());
		log.debug("Создан кэш {} с максимальным размером 5 и временем жизни 60 минут", CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE);
		
		CaffeineCache globalRatingAll = new CaffeineCache(CACHE_NAME_GLOBAL_RATING,
				Caffeine.newBuilder()
						.maximumSize(2000)
						.expireAfterWrite(60, TimeUnit.MINUTES)
						.build());
		log.debug("Создан кэш {} с максимальным размером 2000 и временем жизни 60 минут", CACHE_NAME_GLOBAL_RATING);
		
		manager.setCaches(List.of(
				allTasksByDifficultyCache,
				top5ByScore,
				globalRatingAll
		));
		return manager;
	}

	/**
	 * Создает и настраивает кэш для хранения временного прогресса экзаменов.
	 * <p>
	 * Каждый прогресс идентифицируется по UUID и имеет ограниченное время жизни
	 * после последней операции записи. Когда кэш достигает максимального размера,
	 * наименее используемые записи автоматически удаляются.
	 * 
	 * <p>
	 * Параметры кэша загружаются из конфигурационных свойств приложения.
	 * 
	 *
	 * @return настроенный экземпляр кэша для хранения прогресса экзаменов
	 */
	@Bean(name = "temporaryExamProgressCache")
	public Cache<UUID, TemporaryExamProgress> quizProgressCache() {
		log.info("Создание кэша прогресса экзаменов с параметрами: maximumSize={}, expireAfterWriteMinutes={}",
				cacheProperties.getMaximumSize(), cacheProperties.getExpireAfterWriteMinutes());
				
		return Caffeine.newBuilder()
				.maximumSize(cacheProperties.getMaximumSize())
				.expireAfterWrite(cacheProperties.getExpireAfterWriteMinutes(), TimeUnit.MINUTES)
				.build();
	}
}
