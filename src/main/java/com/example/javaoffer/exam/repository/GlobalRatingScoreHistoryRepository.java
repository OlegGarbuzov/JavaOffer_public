package com.example.javaoffer.exam.repository;

import com.example.javaoffer.exam.entity.GlobalRatingScoreHistory;
import com.example.javaoffer.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link GlobalRatingScoreHistory}.
 * <p>
 * Предоставляет методы для доступа к глобальным рекордам пользователей в базе данных,
 * их поиску и получению рейтинговых таблиц.
 * 
 *
 * @author Garbuzov Oleg
 */
@Repository
public interface GlobalRatingScoreHistoryRepository extends JpaRepository<GlobalRatingScoreHistory, Long> {

	/**
	 * Находит глобальный рейтинг пользователя.
	 * <p>
	 * Метод возвращает запись о лучшем результате указанного пользователя
	 * в глобальной рейтинговой таблице.
	 * 
	 *
	 * @param user пользователь, для которого выполняется поиск
	 * @return опциональный результат по пользователю из глобальной рейтинговой таблицы
	 */
	Optional<GlobalRatingScoreHistory> findByBestUserScoreHistory_User(User user);

	/**
	 * Возвращает топ-5 пользователей по сумме очков в рейтинговом режиме.
	 * <p>
	 * Метод используется для отображения лидеров на главной странице или
	 * в виджете рейтинга.
	 * 
	 *
	 * @return список из 5 лучших результатов пользователей
	 */
	List<GlobalRatingScoreHistory> findTop5ByOrderByBestUserScoreHistory_ScoreDesc();

	/**
	 * Возвращает страницу с результатами всех пользователей, отсортированными по убыванию очков.
	 * <p>
	 * Метод используется для постраничного отображения полной таблицы рейтинга.
	 * 
	 *
	 * @param pageable параметры пагинации и сортировки
	 * @return страница с результатами пользователей
	 */
	Page<GlobalRatingScoreHistory> findAllByOrderByBestUserScoreHistory_ScoreDesc(Pageable pageable);
}
