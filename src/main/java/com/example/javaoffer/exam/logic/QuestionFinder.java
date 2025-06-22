package com.example.javaoffer.exam.logic;

import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.exception.NoQuestionsException;
import com.example.javaoffer.exam.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Компонент для поиска вопросов в режиме свободного экзамена.
 * <p>
 * Реализует интеллектуальный алгоритм поиска вопросов заданной сложности
 * с адаптивной стратегией выбора альтернативных уровней сложности,
 * если на указанном уровне вопросы отсутствуют.
 * 
 */
@Component
@RequiredArgsConstructor
public class QuestionFinder {

	private final TaskService taskService;

	/**
	 * Публичный метод для поиска задач по указанной сложности.
	 * Использует внутренний алгоритм поиска с адаптивной стратегией выбора уровня сложности.
	 *
	 * @param difficulty целевая сложность задач
	 * @return список найденных задач
	 * @throws NoQuestionsException если не удалось найти задачи ни на одном уровне сложности
	 */
	public List<TaskDTO> findTasksByDifficulty(TaskDifficulty difficulty) {
		return findTasksByDifficulty(difficulty, false);
	}

	/**
	 * Поиск задач по указанной сложности с возможностью поиска на других уровнях.
	 *
	 * @param difficulty        сложность задач
	 * @param restartedFromEasy флаг, который указывает, что уже была попытка начать поиск с уровня Easy1
	 * @return список найденных задач
	 * @throws NoQuestionsException если не удалось найти задачи ни на одном уровне сложности
	 */
	private List<TaskDTO> findTasksByDifficulty(TaskDifficulty difficulty, boolean restartedFromEasy) {
		// Шаг 1: Проверяем текущую сложность
		List<TaskDTO> tasks = findTasksAtExactDifficulty(difficulty);
		if (!tasks.isEmpty()) {
			return tasks;
		}

		// Шаг 2: Поочередный поиск вверх-вниз от исходного уровня
		tasks = findTasksAroundDifficulty(difficulty);
		if (!tasks.isEmpty()) {
			return tasks;
		}

		// Шаг 3: Если не нашли задачи ни на одном уровне
		return handleFallbackStrategy(difficulty, restartedFromEasy);
	}

	/**
	 * Поиск задач строго на указанном уровне сложности.
	 *
	 * @param difficulty уровень сложности
	 * @return список задач на указанном уровне сложности или пустой список
	 */
	private List<TaskDTO> findTasksAtExactDifficulty(TaskDifficulty difficulty) {
		return taskService.getTasksByDifficulty(difficulty);
	}

	/**
	 * Поиск задач вокруг указанного уровня сложности (поочередно вверх и вниз).
	 * <p>
	 * Алгоритм последовательно проверяет уровни сложности вверх и вниз от исходного,
	 * увеличивая расстояние от исходного уровня на каждой итерации.
	 * 
	 *
	 * @param difficulty исходный уровень сложности
	 * @return список задач на ближайшем доступном уровне сложности или пустой список
	 */
	private List<TaskDTO> findTasksAroundDifficulty(TaskDifficulty difficulty) {
		int initialLevel = difficulty.getLevel();
		int minLevel = TaskDifficulty.Easy1.getLevel();
		int maxLevel = TaskDifficulty.EXPERT.getLevel();

		// Поочередно проверяем уровни выше и ниже исходного
		for (int offset = 1; offset <= Math.max(maxLevel - initialLevel, initialLevel - minLevel); offset++) {
			// Проверяем уровень выше (+offset)
			List<TaskDTO> tasks = checkHigherDifficultyLevel(initialLevel, offset, maxLevel);
			if (!tasks.isEmpty()) {
				return tasks;
			}

			// Проверяем уровень ниже (-offset)
			tasks = checkLowerDifficultyLevel(initialLevel, offset, minLevel);
			if (!tasks.isEmpty()) {
				return tasks;
			}
		}

		return List.of(); // Пустой список, если задачи не найдены
	}

	/**
	 * Проверка уровня сложности выше текущего.
	 *
	 * @param currentLevel текущий уровень сложности
	 * @param offset       смещение от текущего уровня
	 * @param maxLevel     максимально допустимый уровень сложности
	 * @return список задач на проверяемом уровне сложности или пустой список
	 */
	private List<TaskDTO> checkHigherDifficultyLevel(int currentLevel, int offset, int maxLevel) {
		int higherLevel = currentLevel + offset;
		if (higherLevel <= maxLevel) {
			TaskDifficulty higherDifficulty = TaskDifficulty.fromLevel(higherLevel);
			return taskService.getTasksByDifficulty(higherDifficulty);
		}
		return List.of();
	}

	/**
	 * Проверка уровня сложности ниже текущего.
	 *
	 * @param currentLevel текущий уровень сложности
	 * @param offset       смещение от текущего уровня
	 * @param minLevel     минимально допустимый уровень сложности
	 * @return список задач на проверяемом уровне сложности или пустой список
	 */
	private List<TaskDTO> checkLowerDifficultyLevel(int currentLevel, int offset, int minLevel) {
		int lowerLevel = currentLevel - offset;
		if (lowerLevel >= minLevel) {
			TaskDifficulty lowerDifficulty = TaskDifficulty.fromLevel(lowerLevel);
			return taskService.getTasksByDifficulty(lowerDifficulty);
		}
		return List.of();
	}

	/**
	 * Обработка ситуации, когда задачи не найдены ни на одном уровне.
	 * <p>
	 * Если исходный уровень не был Easy1, метод пытается начать поиск с самого простого уровня.
	 * Если и это не помогает, генерирует исключение.
	 * 
	 *
	 * @param difficulty        исходный уровень сложности
	 * @param restartedFromEasy флаг, указывающий, что поиск уже был перезапущен с уровня Easy1
	 * @return список задач или генерирует исключение
	 * @throws NoQuestionsException если не удалось найти задачи ни на одном уровне сложности
	 */
	private List<TaskDTO> handleFallbackStrategy(TaskDifficulty difficulty, boolean restartedFromEasy) {
		int initialLevel = difficulty.getLevel();

		if (restartedFromEasy || initialLevel == TaskDifficulty.Easy1.getLevel()) {
			// Если уже пытались с Easy1 или сами начинали с Easy1, выбрасываем исключение
			throw new NoQuestionsException("Не найдено задач ни на одном уровне сложности");
		}

		// Последняя попытка - сбрасываемся на Easy1 и повторяем весь процесс
		return findTasksByDifficulty(TaskDifficulty.Easy1, true);
	}
} 