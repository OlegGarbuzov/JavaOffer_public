package com.example.javaoffer.exam.repository;

import com.example.javaoffer.exam.entity.UserScoreHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для работы с историей прохождения экзаменов пользователями.
 * <p>
 * Предоставляет методы для доступа к историям экзаменов в базе данных,
 * их поиску и фильтрации по различным критериям. Использует EntityGraph
 * для оптимизации запросов с загрузкой связанных сущностей.
 *
 * @author Garbuzov Oleg
 * @see UserScoreHistory
 */
@Repository
public interface UserScoreHistoryRepository extends JpaRepository<UserScoreHistory, Long> {

	/**
	 * Находит все истории экзаменов для указанного пользователя.
	 * <p>
	 * Метод возвращает полный список историй прохождения экзаменов
	 * для пользователя с указанным идентификатором, включая связанные ответы.
	 *
	 * @param userId идентификатор пользователя
	 * @return список историй экзаменов пользователя
	 */
	@EntityGraph(attributePaths = "userAnswers")
	List<UserScoreHistory> findByUserId(Long userId);

	/**
	 * Находит истории экзаменов для указанного пользователя с пагинацией.
	 * <p>
	 * Метод возвращает страницу историй прохождения экзаменов для пользователя
	 * с указанным идентификатором, включая связанные ответы.
	 *
	 * @param userId   идентификатор пользователя
	 * @param pageable параметры пагинации и сортировки
	 * @return страница с историями экзаменов пользователя
	 */
	@EntityGraph(attributePaths = "userAnswers")
	Page<UserScoreHistory> findByUserId(Long userId, Pageable pageable);

	/**
	 * Находит истории экзаменов для указанного пользователя и идентификатора экзамена.
	 * <p>
	 * Метод возвращает страницу историй прохождения экзаменов для пользователя
	 * с указанным идентификатором и конкретным идентификатором экзамена.
	 *
	 * @param userId   идентификатор пользователя
	 * @param examId   идентификатор экзамена
	 * @param pageable параметры пагинации и сортировки
	 * @return страница с историями экзаменов пользователя по указанному экзамену
	 */
	@EntityGraph(attributePaths = "userAnswers")
	Page<UserScoreHistory> findByUserIdAndExamID(Long userId, UUID examId, Pageable pageable);

	/**
	 * Находит все истории экзаменов с применением комбинированных фильтров.
	 * <p>
	 * Метод возвращает страницу историй прохождения экзаменов с возможностью
	 * фильтрации по различным критериям: наличие нарушений, идентификатор экзамена,
	 * имя пользователя. Использует оптимизированный запрос с загрузкой связанных ответов.
	 *
	 * @param pageable           параметры пагинации и сортировки
	 * @param showViolationsOnly показывать только записи с нарушениями
	 * @param examId             фильтр по идентификатору экзамена (может быть null)
	 * @param userNamePattern    фильтр по имени пользователя с паттерном LIKE (может быть null)
	 * @return страница с отфильтрованными историями экзаменов
	 */
	@EntityGraph(attributePaths = "userAnswers")
	@Query("SELECT u FROM UserScoreHistory u " +
			"WHERE (:showViolationsOnly = false OR " +
			"       (u.tabSwitchViolations > 0 OR " +
			"        u.textCopyViolations > 0 OR " +
			"        u.heartbeatMissedViolations > 0 OR " +
			"        u.devToolsViolations > 0 OR " +
			"        u.domTamperingViolations > 0 OR " +
			"        u.functionTamperingViolations > 0 OR " +
			"        u.moduleTamperingViolations > 0 OR " +
			"        u.pageCloseViolations > 0 OR " +
			"        u.externalContentViolations > 0 OR " +
			"        u.terminatedByViolations = true)) " +
			"AND (:examId IS NULL OR u.examID = :examId) " +
			"AND (:userNamePattern IS NULL OR u.user.username LIKE :userNamePattern)")
	Page<UserScoreHistory> findAllWithFilters(
			Pageable pageable,
			Boolean showViolationsOnly,
			UUID examId,
			String userNamePattern);
}
