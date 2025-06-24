package com.example.javaoffer.exam.service;

import com.example.javaoffer.exam.dto.AnswerDTO;
import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.entity.Answer;
import com.example.javaoffer.exam.entity.Task;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.exception.NoCorrectAnswerByTaskException;
import com.example.javaoffer.exam.exception.QuestionNotFoundException;
import com.example.javaoffer.exam.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.javaoffer.common.constants.JpaCacheName.CACHE_NAME_ALL_TASK_BY_DIFFICULTY;

/**
 * Сервис для работы с заданиями (вопросами) в системе экзаменов.
 * <p>
 * Предоставляет методы для создания, чтения, обновления и удаления заданий,
 * а также для получения заданий по различным критериям (сложность, текст вопроса).
 * Реализует валидацию заданий и проверку корректности ответов.
 *
 * <p>
 * Сервис использует кэширование для оптимизации доступа к часто запрашиваемым данным
 * и транзакционное управление для обеспечения целостности данных.
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
	private final TaskRepository taskRepository;

	/**
	 * Получает список всех заданий в системе.
	 * <p>
	 * Метод возвращает все задания, хранящиеся в базе данных,
	 * преобразуя их в формат DTO для передачи клиенту.
	 *
	 * @return список всех заданий в формате DTO
	 */
	@Transactional(readOnly = true)
	public List<TaskDTO> getAllTasks() {
		log.debug("Запрос на получение всех заданий");
		List<Task> tasks = taskRepository.findAll();
		log.debug("Получено {} заданий из базы данных", tasks.size());
		return tasks.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	/**
	 * Получает задание по его идентификатору.
	 * <p>
	 * Метод выполняет поиск задания в базе данных по указанному идентификатору
	 * и возвращает его в формате DTO. Если задание не найдено, генерируется исключение.
	 *
	 * @param id идентификатор задания
	 * @return задание в формате DTO
	 * @throws QuestionNotFoundException если задание с указанным id не найдено в БД
	 */
	@Transactional(readOnly = true)
	public TaskDTO getTaskById(Long id) {
		log.debug("Запрос на получение задания с id: {}", id);
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> {
					log.error("Задача с id {} не найдена", id);
					return new QuestionNotFoundException("Задача с id " + id + " не найдена");
				});
		log.trace("Задание с id {} успешно найдено: {}", id, task.getQuestion());
		return convertToDTO(task);
	}

	/**
	 * Получает список заданий с указанным уровнем сложности.
	 * <p>
	 * Метод возвращает все задания с указанной сложностью, исключая некорректные задания
	 * (без правильных ответов или с несколькими правильными ответами). Результаты кэшируются
	 * для оптимизации производительности.
	 *
	 * @param taskDifficulty уровень сложности заданий
	 * @return список заданий с указанной сложностью в формате DTO
	 */
	@Cacheable(CACHE_NAME_ALL_TASK_BY_DIFFICULTY)
	@Transactional(readOnly = true)
	public List<TaskDTO> getTasksByDifficulty(TaskDifficulty taskDifficulty) {
		log.debug("Запрос на получение заданий со сложностью: {}", taskDifficulty);
		List<Task> tasks = taskRepository.findByDifficulty(taskDifficulty);

		// Валидация вопросов. Найдем вопросы в которых нет корректных ответов вообще или где их больше 1
		Set<Long> invalidTaskIds = validateQuestion(tasks);
		if (!invalidTaskIds.isEmpty()) {
			log.warn("Обнаружены некорректные задания со сложностью {}: {}", taskDifficulty, invalidTaskIds);
		}

		log.debug("Найдено {} заданий со сложностью {}, из них корректных: {}",
				tasks.size(), taskDifficulty, tasks.size() - invalidTaskIds.size());
		return tasks.stream()
				.filter(task -> !invalidTaskIds.contains(task.getId()))
				.map(this::convertToDTOWithoutAnswersDetails)
				.toList();
	}

	/**
	 * Получает случайную задачу по указанной сложности.
	 * <p>
	 * Метод возвращает случайную задачу с указанной сложностью для тестирования.
	 * Важно: возвращает задачу с информацией о корректности ответов, поэтому
	 * не должен использоваться для передачи данных клиенту.
	 *
	 * @param taskDifficulty уровень сложности заданий
	 * @return опциональный объект с заданием в формате DTO
	 */
	@Transactional(readOnly = true)
	public Optional<TaskDTO> getTestRandomTasksByDifficulty(TaskDifficulty taskDifficulty) {
		log.debug("Запрос на получение случайной тестовой задачи со сложностью: {}", taskDifficulty);
		List<Task> tasks = taskRepository.findByDifficulty(taskDifficulty);

		if (tasks.isEmpty()) {
			log.debug("Не найдено заданий со сложностью {}", taskDifficulty);
			return Optional.empty();
		}

		Task randomTask = tasks.get(new Random().nextInt(tasks.size()));
		log.trace("Выбрана случайная задача с id: {}", randomTask.getId());
		return Optional.of(convertToDTO(randomTask));
	}

	/**
	 * Валидирует список заданий, проверяя, что у каждого задания ровно один корректный ответ.
	 * <p>
	 * Если у задания нет корректных ответов или их больше одного, метод считает такое задание некорректным
	 * и логирует соответствующую ошибку.
	 *
	 * @param taskList список заданий для валидации
	 * @return множество идентификаторов заданий, которые не прошли валидацию
	 */
	private Set<Long> validateQuestion(List<Task> taskList) {
		log.trace("Валидация {} заданий на корректность ответов", taskList.size());
		return taskList.stream()
				.filter(task -> {
					long correctCount = task.getAnswers().stream()
							.filter(Answer::isCorrect)
							.count();

					if (correctCount == 0) {
						log.error("Нет корректных ответов в задании с id: {}", task.getId());
						return true;
					} else if (correctCount > 1) {
						log.error("Слишком много корректных ответов в задании с id: {} (найдено: {})",
								task.getId(), correctCount);
						return true;
					}

					return false;
				})
				.map(Task::getId)
				.collect(Collectors.toSet());
	}

	/**
	 * Создает новое задание в системе.
	 * <p>
	 * Метод преобразует DTO в сущность, сохраняет её в базе данных
	 * и возвращает созданное задание с установленным идентификатором.
	 * При создании задания происходит очистка кэша для обеспечения актуальности данных.
	 *
	 * @param taskDTO данные нового задания
	 * @return созданное задание в формате DTO с установленным id
	 */
	@CacheEvict(value = CACHE_NAME_ALL_TASK_BY_DIFFICULTY, allEntries = true)
	@Transactional
	public TaskDTO createTask(TaskDTO taskDTO) {
		log.info("Запрос на создание нового задания: {}", taskDTO.getQuestion());
		Task task = convertToEntity(taskDTO);
		Task savedTask = taskRepository.save(task);
		log.info("Успешно создано новое задание с id: {}", savedTask.getId());
		return convertToDTO(savedTask);
	}

	/**
	 * Обновляет существующее задание.
	 * <p>
	 * Метод находит задание по идентификатору, обновляет его данные
	 * и сохраняет изменения в базе данных. При обновлении задания
	 * происходит очистка кэша для обеспечения актуальности данных.
	 *
	 * @param id      идентификатор задания для обновления
	 * @param taskDTO новые данные задания
	 * @return обновленное задание в формате DTO
	 * @throws EntityNotFoundException если задание с указанным id не найдено
	 */
	@CacheEvict(value = CACHE_NAME_ALL_TASK_BY_DIFFICULTY, allEntries = true)
	@Transactional
	public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
		log.info("Запрос на обновление задания с id: {}", id);
		Task existingTask = taskRepository.findById(id)
				.orElseThrow(() -> {
					log.error("Не удалось обновить: задание с id {} не найдено", id);
					return new EntityNotFoundException("Задача не найдена с id: " + id);
				});

		updateTaskFromDTO(existingTask, taskDTO);
		Task updatedTask = taskRepository.save(existingTask);
		log.info("Задание с id: {} успешно обновлено", id);
		return convertToDTO(updatedTask);
	}

	/**
	 * Удаляет задание по его идентификатору.
	 * <p>
	 * Метод проверяет существование задания и удаляет его из базы данных.
	 * При удалении задания происходит очистка кэша для обеспечения актуальности данных.
	 *
	 * @param id идентификатор задания для удаления
	 * @throws EntityNotFoundException если задание с указанным id не найдено
	 */
	@CacheEvict(value = CACHE_NAME_ALL_TASK_BY_DIFFICULTY, allEntries = true)
	@Transactional
	public void deleteTask(Long id) {
		log.info("Запрос на удаление задания с id: {}", id);
		if (!taskRepository.existsById(id)) {
			log.error("Не удалось удалить: задание с id {} не найдено", id);
			throw new EntityNotFoundException("Задача не найдена с id: " + id);
		}
		taskRepository.deleteById(id);
		log.info("Задание с id: {} успешно удалено", id);
	}

	/**
	 * Проверяет, является ли ответ с указанным идентификатором правильным.
	 * <p>
	 * Метод находит правильный ответ на задание и сравнивает его идентификатор
	 * с идентификатором предоставленного ответа.
	 *
	 * @param taskDTO      задание, на которое проверяется ответ
	 * @param lastAnswerID идентификатор ответа для проверки
	 * @return true, если ответ правильный, false - в противном случае
	 * @throws NoCorrectAnswerByTaskException если у задания нет правильного ответа
	 */
	@Transactional(readOnly = true)
	public Boolean answerIsCorrect(TaskDTO taskDTO, Long lastAnswerID) {
		log.debug("Проверка корректности ответа с id вопроса: {}, id ответа {}", taskDTO.getId(), lastAnswerID);
		AnswerDTO answerDTO = findCorrectAnswerByTaskDto(taskDTO);
		boolean isCorrect = Objects.equals(answerDTO.getId(), lastAnswerID);
		log.debug("Корректность: {}, с id вопроса: {}, id ответа {}", isCorrect, taskDTO.getId(), lastAnswerID);
		return isCorrect;
	}

	/**
	 * Возвращает корректный ответ на задание.
	 * <p>
	 * Метод находит и возвращает правильный ответ на указанное задание.
	 *
	 * @param taskDTO      задание, для которого требуется найти правильный ответ
	 * @param lastAnswerID идентификатор ответа пользователя (для логирования)
	 * @return объект DTO с данными правильного ответа
	 * @throws NoCorrectAnswerByTaskException если у задания нет правильного ответа
	 */
	@Transactional(readOnly = true)
	public AnswerDTO getCorrectAnswerByTaskDto(TaskDTO taskDTO, Long lastAnswerID) {
		log.debug("Запрос правильного ответа из задания с id: {}. Ответ пользователя: {}", taskDTO.getId(), lastAnswerID);
		AnswerDTO answerDTO = findCorrectAnswerByTaskDto(taskDTO);
		log.debug("Ответ корректен: {}, с id вопроса: {}. Ответ пользователя: {}. Возвращаем верный ответ",
				Objects.equals(answerDTO.getId(), lastAnswerID), taskDTO.getId(), lastAnswerID);
		return answerDTO;
	}

	/**
	 * Находит правильный ответ в задании.
	 * <p>
	 * Метод выполняет поиск правильного ответа среди всех ответов задания.
	 * Если правильный ответ не найден, генерируется исключение.
	 *
	 * @param taskDTO задание, для которого требуется найти правильный ответ
	 * @return объект DTO с данными правильного ответа
	 * @throws NoCorrectAnswerByTaskException если у задания нет правильного ответа
	 */
	private AnswerDTO findCorrectAnswerByTaskDto(TaskDTO taskDTO) {
		log.trace("Поиск правильного ответа для задания с id: {}", taskDTO.getId());
		return taskDTO.getAnswers().stream()
				.filter(AnswerDTO::getIsCorrect)
				.findFirst()
				.orElseThrow(() -> {
					log.error("Критическая ошибка. Нет корректных ответов в задании с id: {}", taskDTO.getId());
					return new NoCorrectAnswerByTaskException("Нет корректных ответов в задании с id:" + taskDTO.getId());
				});
	}

	/**
	 * Конвертирует сущность Task в DTO для передачи клиенту.
	 * <p>
	 * Метод преобразует объект сущности Task в объект DTO с необходимыми
	 * для клиента данными, включая информацию о вопросе и всех ответах.
	 *
	 * @param task сущность задания для конвертации
	 * @return объект DTO с данными задания
	 */
	public TaskDTO convertToDTO(Task task) {
		log.trace("Конвертация задания с id: {} в DTO", task.getId());
		return TaskDTO.builder()
				.id(task.getId())
				.question(task.getQuestion())
				.topic(task.getTopic())
				.difficulty(task.getDifficulty())
				.grade(task.getGrade())
				.answers(task.getAnswers().stream()
						.map(answer -> AnswerDTO.builder()
								.id(answer.getId())
								.content(answer.getContent())
								.isCorrect(answer.isCorrect())
								.explanation(answer.getExplanation())
								.build())
						.collect(Collectors.toList()))
				.build();
	}

	/**
	 * Конвертирует сущность Task в DTO для передачи клиенту без деталей ответов.
	 * <p>
	 * Метод преобразует объект сущности Task в объект DTO, но без информации
	 * о правильности ответов и их объяснений. Используется для передачи данных
	 * в процессе экзамена, чтобы скрыть правильные ответы от пользователя.
	 *
	 * @param task сущность задания для конвертации
	 * @return объект DTO с данными задания без деталей ответов
	 */
	private TaskDTO convertToDTOWithoutAnswersDetails(Task task) {
		log.trace("Конвертация задания с id: {} в DTO для передачи в экзамен(без подробностей ответов)", task.getId());
		return TaskDTO.builder()
				.id(task.getId())
				.question(task.getQuestion())
				.topic(task.getTopic())
				.difficulty(task.getDifficulty())
				.grade(task.getGrade())
				.answers(task.getAnswers().stream()
						.map(answer -> AnswerDTO.builder()
								.id(answer.getId())
								.content(answer.getContent())
								.build())
						.collect(Collectors.toList()))
				.build();
	}

	/**
	 * Конвертирует DTO в сущность Task для сохранения в базе данных.
	 * <p>
	 * Метод преобразует объект DTO с данными от клиента в объект сущности
	 * для последующего сохранения в базе данных, включая связанные ответы.
	 *
	 * @param dto объект DTO с данными задания
	 * @return сущность задания для сохранения
	 */
	public Task convertToEntity(TaskDTO dto) {
		log.trace("Конвертация DTO в сущность Task");
		Task task = Task.builder()
				.id(dto.getId())
				.question(dto.getQuestion())
				.topic(dto.getTopic())
				.difficulty(dto.getDifficulty())
				.grade(dto.getGrade())
				.build();

		if (dto.getAnswers() != null) {
			List<Answer> answers = new ArrayList<>();
			for (AnswerDTO answerDTO : dto.getAnswers()) {
				Answer answer = Answer.builder()
						.content(answerDTO.getContent())
						.isCorrect(answerDTO.getIsCorrect())
						.explanation(answerDTO.getExplanation())
						.task(task)
						.build();
				answers.add(answer);
			}
			task.setAnswers(answers);
			log.trace("Добавлено {} ответов к заданию", answers.size());
		}
		return task;
	}

	/**
	 * Обновляет данные существующей сущности Task из DTO.
	 * <p>
	 * Метод обновляет поля существующей сущности Task данными из DTO,
	 * включая обновление или создание связанных ответов.
	 *
	 * @param task сущность задания для обновления
	 * @param dto  объект DTO с новыми данными
	 */
	private void updateTaskFromDTO(Task task, TaskDTO dto) {
		log.trace("Обновление сущности Task с id: {} из DTO", task.getId());
		task.setQuestion(dto.getQuestion());
		task.setTopic(dto.getTopic());
		task.setDifficulty(dto.getDifficulty());
		task.setGrade(dto.getGrade());

		if (dto.getAnswers() != null) {
			// Создаем карту существующих ответов по ID
			Map<Long, Answer> existingAnswers = task.getAnswers().stream()
					.collect(Collectors.toMap(Answer::getId, answer -> answer));

			// Очищаем список ответов
			int previousAnswersCount = task.getAnswers().size();
			task.getAnswers().clear();
			log.trace("Очищен список из {} существующих ответов для задания с id: {}", previousAnswersCount, task.getId());

			// Обновляем или создаем новые ответы
			int newAnswersCount = 0;
			int updatedAnswersCount = 0;

			for (AnswerDTO answerDTO : dto.getAnswers()) {
				Answer answer;
				if (answerDTO.getId() != null && existingAnswers.containsKey(answerDTO.getId())) {
					// Обновляем существующий ответ
					answer = existingAnswers.get(answerDTO.getId());
					answer.setContent(answerDTO.getContent());
					answer.setCorrect(answerDTO.getIsCorrect());
					answer.setExplanation(answerDTO.getExplanation());
					updatedAnswersCount++;
				} else {
					// Создаем новый ответ
					answer = Answer.builder()
							.content(answerDTO.getContent())
							.isCorrect(answerDTO.getIsCorrect())
							.explanation(answerDTO.getExplanation())
							.task(task)
							.build();
					newAnswersCount++;
				}
				task.getAnswers().add(answer);
			}

			log.trace("Для задания с id: {} обновлено {} существующих ответов и добавлено {} новых",
					task.getId(), updatedAnswersCount, newAnswersCount);
		}
	}

	/**
	 * Фильтрует задания по теме, сложности, грейду, поисковому запросу и показу дублей.
	 * <p>
	 * Метод возвращает список заданий, соответствующих указанным критериям фильтрации.
	 * Если какой-либо параметр фильтрации не указан (null или пустая строка),
	 * фильтрация по этому критерию не выполняется.
	 *
	 * @param topic          фильтр по теме задания (может быть null)
	 * @param difficulty     фильтр по сложности задания (может быть null)
	 * @param grade          фильтр по грейду задания (может быть null)
	 * @param search         поисковый запрос для фильтрации по тексту вопроса (может быть null)
	 * @param showDuplicates показывать только задания с дублирующимся текстом вопроса
	 * @return список отфильтрованных заданий в формате DTO
	 */
	@Transactional(readOnly = true)
	public List<TaskDTO> filterTasks(String topic, String difficulty, String grade, String search, Boolean showDuplicates) {
		log.debug("Фильтрация заданий: topic={}, difficulty={}, grade={}, search={}, showDuplicates={}",
				topic, difficulty, grade, search, showDuplicates);

		List<Task> tasks = taskRepository.findAll();
		log.trace("Получено {} заданий из базы данных для фильтрации", tasks.size());

		// Если нужно показывать дубли, сначала найдем ID заданий с дублирующимся текстом
		Set<Long> duplicateTaskIds = new HashSet<>();
		if (showDuplicates != null && showDuplicates) {
			Map<String, List<Task>> tasksByQuestion = tasks.stream()
					.collect(Collectors.groupingBy(Task::getQuestion));

			duplicateTaskIds = tasksByQuestion.entrySet().stream()
					.filter(entry -> entry.getValue().size() > 1)
					.flatMap(entry -> entry.getValue().stream())
					.map(Task::getId)
					.collect(Collectors.toSet());

			log.debug("Найдено {} заданий с дублирующимся текстом", duplicateTaskIds.size());
		}

		List<TaskDTO> filteredTasks = applyFilters(tasks, topic, difficulty, grade, search, duplicateTaskIds, showDuplicates);

		log.debug("После фильтрации осталось {} заданий", filteredTasks.size());
		return filteredTasks;
	}

	/**
	 * Применяет фильтры к списку заданий и преобразует их в DTO.
	 * <p>
	 * Приватный метод для выполнения фильтрации заданий по всем критериям
	 * и преобразования результата в список DTO без деталей ответов.
	 *
	 * @param tasks            список всех заданий для фильтрации
	 * @param topic            фильтр по теме задания (может быть null)
	 * @param difficulty       фильтр по сложности задания (может быть null)
	 * @param grade            фильтр по грейду задания (может быть null)
	 * @param search           поисковый запрос для фильтрации по тексту вопроса (может быть null)
	 * @param duplicateTaskIds множество ID заданий с дублирующимся текстом
	 * @param showDuplicates   показывать только задания с дублирующимся текстом
	 * @return отфильтрованный список заданий в формате DTO
	 */
	private List<TaskDTO> applyFilters(List<Task> tasks, String topic, String difficulty, String grade,
									   String search, Set<Long> duplicateTaskIds, Boolean showDuplicates) {

		return tasks.stream()
				.filter(task -> topic == null || topic.isEmpty() || task.getTopic().name().equalsIgnoreCase(topic))
				.filter(task -> difficulty == null || difficulty.isEmpty() || task.getDifficulty().name().equalsIgnoreCase(difficulty))
				.filter(task -> grade == null || grade.isEmpty() || task.getGrade().name().equalsIgnoreCase(grade))
				.filter(task -> search == null || search.isEmpty() || task.getQuestion().toLowerCase().contains(search.toLowerCase()))
				.filter(task -> {
					// Фильтр дублей: если showDuplicates = true, показываем только дубли; если false - показываем все
					if (showDuplicates == null || !showDuplicates) {
						return true; // Показываем все задания
					} else {
						return duplicateTaskIds.contains(task.getId()); // Показываем только дубли
					}
				})
				.map(this::convertToDTOWithoutAnswersDetails)
				.toList();
	}

}
