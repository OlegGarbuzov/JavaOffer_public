package com.example.javaoffer.exam.repository;

import com.example.javaoffer.exam.entity.Task;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.enums.TaskTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с заданиями (вопросами) в системе экзаменов.
 * <p>
 * Предоставляет методы для доступа к заданиям в базе данных, их поиска
 * и фильтрации по различным критериям: сложность, грейд, тема.
 * Поддерживает как стандартные методы Spring Data JPA, так и
 * специализированные запросы для получения заданий с определенными характеристиками.
 *
 * @author Garbuzov Oleg
 * @see Task
 * @see TaskDifficulty
 * @see TaskGrade
 * @see TaskTopic
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

	/**
	 * Находит все задания с указанной сложностью.
	 * <p>
	 * Метод возвращает список всех заданий, имеющих указанный уровень сложности,
	 * включая связанные с ними варианты ответов.
	 *
	 * @param taskDifficulty уровень сложности заданий
	 * @return список заданий с указанной сложностью
	 */
	List<Task> findByDifficulty(TaskDifficulty taskDifficulty);

	/**
	 * Находит все задания по тексту вопроса.
	 * <p>
	 * Метод выполняет поиск заданий, текст вопроса которых точно соответствует
	 * указанной строке. Для поиска по частичному совпадению следует использовать
	 * специализированные запросы с оператором LIKE.
	 *
	 * @param question текст вопроса для поиска
	 * @return список заданий с указанным текстом вопроса
	 */
	List<Task> findByQuestion(String question);
}
