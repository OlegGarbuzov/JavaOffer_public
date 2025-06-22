package com.example.javaoffer.common.constants;

/**
 * Константы имен кэшей JPA.
 * <p>
 * Содержит имена кэшей, используемых в приложении для кэширования
 * результатов запросов к базе данных с помощью JPA.
 * 
 */
public class JpaCacheName {

	/**
	 * Имя кэша для хранения топ-5 результатов глобального рейтинга по очкам
	 */
	public static final String CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE = "globalRatingTop5ByScore";
	
	/**
	 * Имя кэша для хранения данных глобального рейтинга
	 */
	public static final String CACHE_NAME_GLOBAL_RATING = "globalRating";
	
	/**
	 * Имя кэша для хранения всех задач, сгруппированных по сложности
	 */
	public static final String CACHE_NAME_ALL_TASK_BY_DIFFICULTY = "allTasksByDifficulty";

}
