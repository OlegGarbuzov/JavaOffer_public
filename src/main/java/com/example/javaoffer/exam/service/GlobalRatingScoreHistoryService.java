package com.example.javaoffer.exam.service;

import com.example.javaoffer.exam.entity.GlobalRatingScoreHistory;
import com.example.javaoffer.exam.entity.UserScoreHistory;
import com.example.javaoffer.exam.repository.GlobalRatingScoreHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.javaoffer.common.constants.JpaCacheName.CACHE_NAME_GLOBAL_RATING;
import static com.example.javaoffer.common.constants.JpaCacheName.CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE;

/**
 * Сервис для работы с глобальными рейтингами пользователей.
 * <p>
 * Предоставляет методы для обновления, получения и управления глобальными
 * рейтингами пользователей на основе их лучших результатов в экзаменах.
 * Использует кэширование для оптимизации доступа к часто запрашиваемым данным.
 * 
 * <p>
 * Сервис отслеживает лучшие результаты каждого пользователя и обновляет
 * глобальный рейтинг при появлении новых, более высоких результатов.
 * 
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalRatingScoreHistoryService {
	private final GlobalRatingScoreHistoryRepository globalRatingScoreHistoryRepository;

	/**
	 * Обновляет лучший результат пользователя в глобальном рейтинге.
	 * <p>
	 * Метод проверяет, существует ли уже запись о пользователе в глобальном рейтинге.
	 * Если запись существует, сравнивает новый результат с существующим и обновляет
	 * запись, если новый результат лучше. Если записи нет, создает новую.
	 * 
	 * <p>
	 * При обновлении рейтинга происходит очистка кэша для обеспечения актуальности данных.
	 * 
	 *
	 * @param newScore новый результат пользователя для проверки и возможного обновления рейтинга
	 */
	@Transactional
	@Caching(evict = {
			@CacheEvict(cacheNames = CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE, allEntries = true),
			@CacheEvict(cacheNames = CACHE_NAME_GLOBAL_RATING, allEntries = true)
	})
	public void refreshBestUserScoreInGlobalRating(UserScoreHistory newScore) {
		log.info("Обновление глобального рейтинга для пользователя: {} (score={})", 
				newScore.getUser().getUsername(), newScore.getScore());
		
		globalRatingScoreHistoryRepository
				.findByBestUserScoreHistory_User(newScore.getUser())
				.ifPresentOrElse(
						existing -> {
							log.debug("Найден существующий рекорд: score={}", existing.getBestUserScoreHistory().getScore());
							if (newScore.getScore() > existing.getBestUserScoreHistory().getScore()) {
								existing.setBestUserScoreHistory(newScore);
								globalRatingScoreHistoryRepository.save(existing);
								log.info("Рекорд обновлён: новый score={}", newScore.getScore());
							} else {
								log.debug("Рекорд не обновлён: новый score={} не превышает существующий score={}", 
										newScore.getScore(), existing.getBestUserScoreHistory().getScore());
							}
						},
						() -> {
							GlobalRatingScoreHistory newRecord = GlobalRatingScoreHistory.builder()
									.bestUserScoreHistory(newScore)
									.build();
							globalRatingScoreHistoryRepository.save(newRecord);
							log.info("Добавлен новый рекорд для пользователя: {} (score={}, id={})", 
									newScore.getUser().getUsername(), 
									newScore.getScore(),
									newRecord.getId());
						}
				);
	}

	/**
	 * Возвращает топ-5 пользователей по набранным очкам.
	 * <p>
	 * Метод получает список из 5 лучших результатов пользователей,
	 * отсортированных по убыванию набранных очков. Результаты кэшируются
	 * для оптимизации производительности.
	 * 
	 *
	 * @return список из 5 лучших результатов пользователей
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = CACHE_NAME_GLOBAL_RATING_TOP_5_SCORE)
	public List<GlobalRatingScoreHistory> getTop5ByScore() {
		log.debug("Запрос на получение топ-5 пользователей по очкам");
		List<GlobalRatingScoreHistory> top5 = globalRatingScoreHistoryRepository.findTop5ByOrderByBestUserScoreHistory_ScoreDesc();
		log.trace("Получен список из {} пользователей для топ-5", top5.size());
		return top5;
	}

	/**
	 * Возвращает страницу с полным рейтингом пользователей.
	 * <p>
	 * Метод получает страницу с результатами всех пользователей,
	 * отсортированными по убыванию набранных очков. Результаты кэшируются
	 * для оптимизации производительности.
	 * 
	 *
	 * @param pageable параметры пагинации и сортировки
	 * @return страница с результатами пользователей
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = CACHE_NAME_GLOBAL_RATING)
	public Page<GlobalRatingScoreHistory> findAll(Pageable pageable) {
		log.debug("Запрос на получение страницы рейтинга: page={}, size={}", 
				pageable.getPageNumber(), pageable.getPageSize());
		Page<GlobalRatingScoreHistory> result = globalRatingScoreHistoryRepository
				.findAllByOrderByBestUserScoreHistory_ScoreDesc(pageable);
		log.trace("Получена страница рейтинга с {} записями из {} всего", 
				result.getNumberOfElements(), result.getTotalElements());
		return result;
	}
}
