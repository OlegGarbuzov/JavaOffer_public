package com.example.javaoffer.controllerTest;

import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.entity.Answer;
import com.example.javaoffer.exam.entity.Task;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.enums.TaskTopic;
import com.example.javaoffer.exam.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static TestConstants.TestConstant.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тестовый класс для контроллера фильтрации вопросов в административной панели.
 * <p>
 * Проверяет функциональность фильтрации задач/вопросов по различным критериям:
 * <ul>
 *   <li>Фильтрация по теме (Java Core, Spring, и т.д.)</li>
 *   <li>Фильтрация по сложности (Easy, Medium, Hard)</li>
 *   <li>Фильтрация по уровню (Junior, Middle, Senior)</li>
 *   <li>Текстовый поиск по содержимому вопроса</li>
 *   <li>Комбинированная фильтрация по нескольким параметрам</li>
 * </ul>
 * </p>
 * <p>
 * Также тестирует контроль доступа - только администраторы должны иметь
 * возможность использовать функцию фильтрации вопросов.
 * </p>
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuestionFilterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TaskRepository taskRepository;

	/**
	 * Настройка тестовых данных перед каждым тестом.
	 * <p>
	 * Очищает репозиторий задач и создает новые тестовые данные
	 * для обеспечения изолированности тестов.
	 * </p>
	 */
	@BeforeEach
	void setUp() {
		log.debug("Настройка тестовых данных для контроллера фильтрации вопросов");
		taskRepository.deleteAll();
		log.trace("Очищены все задачи из репозитория");
		createTestTasks();
		log.debug("Тестовые задачи успешно созданы");
	}

	/**
	 * Создает тестовые задачи для проверки функциональности фильтрации.
	 * <p>
	 * Создает две задачи с разными характеристиками:
	 * <ul>
	 *   <li>Java Core задача: MEDIUM1 сложность, JUNIOR уровень</li>
	 *   <li>Spring задача: Easy1 сложность, MIDDLE уровень</li>
	 * </ul>
	 * </p>
	 */
	private void createTestTasks() {
		log.debug("Создание тестовых задач для фильтрации");
		
		Task javaTask = Task.builder()
				.question("Java Core Question")
				.topic(TaskTopic.CORE)
				.difficulty(TaskDifficulty.MEDIUM1)
				.grade(TaskGrade.JUNIOR)
				.createdAt(LocalDateTime.now())
				.build();

		Answer javaAnswer1 = Answer.builder()
				.content("Java Core Answer 1")
				.isCorrect(true)
				.explanation("This is correct")
				.task(javaTask)
				.build();

		Answer javaAnswer2 = Answer.builder()
				.content("Java Core Answer 2")
				.isCorrect(false)
				.explanation("This is incorrect")
				.task(javaTask)
				.build();

		javaTask.setAnswers(Arrays.asList(javaAnswer1, javaAnswer2));
		Task savedJavaTask = taskRepository.save(javaTask);
		log.trace("Создана Java Core задача с ID: {}", savedJavaTask.getId());

		Task springTask = Task.builder()
				.question("Spring Framework Question")
				.topic(TaskTopic.SPRING)
				.difficulty(TaskDifficulty.Easy1)
				.grade(TaskGrade.MIDDLE)
				.createdAt(LocalDateTime.now())
				.build();

		Answer springAnswer1 = Answer.builder()
				.content("Spring Answer 1")
				.isCorrect(true)
				.explanation("This is correct")
				.task(springTask)
				.build();

		Answer springAnswer2 = Answer.builder()
				.content("Spring Answer 2")
				.isCorrect(false)
				.explanation("This is incorrect")
				.task(springTask)
				.build();

		springTask.setAnswers(Arrays.asList(springAnswer1, springAnswer2));
		Task savedSpringTask = taskRepository.save(springTask);
		log.trace("Создана Spring задача с ID: {}", savedSpringTask.getId());
	}

	/**
	 * Тестирует фильтрацию вопросов с полным набором параметров.
	 * <p>
	 * Проверяет, что при указании всех фильтров возвращается точно соответствующий результат.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Фильтрация вопросов с параметрами должна возвращать отфильтрованные результаты")
	@WithMockUser(roles = "ADMIN")
	void filterQuestionsWithParametersShouldReturnFilteredResults() throws Exception {
		log.info("Тестирование фильтрации вопросов с полным набором параметров");
		
		MvcResult result = mockMvc.perform(get(URL_ADMIN_FILTER_QUESTIONS)
						.with(SecurityMockMvcRequestPostProcessors.csrf())
						.param("topic", TOPIC_CORE)
						.param("difficulty", DIFFICULTY_MEDIUM1)
						.param("grade", GRADE_JUNIOR)
						.param("search", "Java"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/fragments/questions-tbody :: rows"))
				.andExpect(model().attributeExists("tasks"))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<TaskDTO> tasks = (List<TaskDTO>) result.getModelAndView().getModel().get("tasks");
		log.debug("Найдено {} задач при фильтрации", tasks.size());
		assertEquals(1, tasks.size(), "Должна быть найдена ровно 1 задача");
		assertEquals("Java Core Question", tasks.getFirst().getQuestion());
		assertEquals(TaskTopic.CORE, tasks.getFirst().getTopic());
		assertEquals(TaskDifficulty.MEDIUM1, tasks.getFirst().getDifficulty());
		assertEquals(TaskGrade.JUNIOR, tasks.getFirst().getGrade());
		log.info("Фильтрация с параметрами завершена успешно");
	}

	/**
	 * Тестирует получение всех вопросов без фильтров.
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Фильтрация вопросов без параметров должна возвращать все результаты")
	@WithMockUser(roles = "ADMIN")
	void filterQuestionsWithoutParametersShouldReturnAllResults() throws Exception {
		log.info("Тестирование получения всех вопросов без фильтров");
		
		MvcResult result = mockMvc.perform(get(URL_ADMIN_FILTER_QUESTIONS)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/fragments/questions-tbody :: rows"))
				.andExpect(model().attributeExists("tasks"))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<TaskDTO> tasks = (List<TaskDTO>) result.getModelAndView().getModel().get("tasks");
		log.debug("Получено {} задач без фильтрации", tasks.size());
		assertEquals(2, tasks.size(), "Должны быть возвращены все 2 задачи");
		log.info("Получение всех вопросов завершено успешно");
	}

	/**
	 * Тестирует фильтрацию с частичными параметрами.
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Фильтрация вопросов с частичными параметрами должна работать корректно")
	@WithMockUser(roles = "ADMIN")
	void filterQuestionsWithPartialParametersShouldWorkCorrectly() throws Exception {
		log.info("Тестирование фильтрации по теме");
		
		MvcResult result = mockMvc.perform(get(URL_ADMIN_FILTER_QUESTIONS)
						.with(SecurityMockMvcRequestPostProcessors.csrf())
						.param("topic", TOPIC_CORE))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/fragments/questions-tbody :: rows"))
				.andExpect(model().attributeExists("tasks"))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<TaskDTO> tasks = (List<TaskDTO>) result.getModelAndView().getModel().get("tasks");
		log.debug("Найдено {} задач при фильтрации по теме CORE", tasks.size());
		assertEquals(1, tasks.size());
		assertEquals("Java Core Question", tasks.getFirst().getQuestion());
		log.info("Фильтрация по теме завершена успешно");
	}

	/**
	 * Тестирует поиск по содержимому вопроса.
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Фильтрация вопросов по поисковому запросу должна работать корректно")
	@WithMockUser(roles = "ADMIN")
	void filterQuestionsWithSearchQueryShouldWorkCorrectly() throws Exception {
		log.info("Тестирование поиска по тексту");
		
		MvcResult result = mockMvc.perform(get(URL_ADMIN_FILTER_QUESTIONS)
						.with(SecurityMockMvcRequestPostProcessors.csrf())
						.param("search", "Spring"))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/fragments/questions-tbody :: rows"))
				.andExpect(model().attributeExists("tasks"))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<TaskDTO> tasks = (List<TaskDTO>) result.getModelAndView().getModel().get("tasks");
		log.debug("Найдено {} задач при поиске по 'Spring'", tasks.size());
		assertEquals(1, tasks.size());
		assertEquals("Spring Framework Question", tasks.getFirst().getQuestion());
		log.info("Поиск по тексту завершен успешно");
	}

	/**
	 * Тестирует контроль доступа к функции фильтрации.
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@DisplayName("Доступ к фильтрации вопросов без прав администратора должен быть запрещен")
	@WithMockUser(roles = "USER")
	void accessToFilterQuestionsWithoutAdminRoleShouldBeForbidden() throws Exception {
		log.info("Тестирование контроля доступа к фильтрации");
		
		mockMvc.perform(get(URL_ADMIN_FILTER_QUESTIONS)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isForbidden());
		
		log.debug("Подтвержден запрет доступа для роли USER");
		log.info("Тестирование контроля доступа завершено успешно");
	}
} 