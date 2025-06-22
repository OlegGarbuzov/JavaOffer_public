package com.example.javaoffer.controllerTest;

import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.enums.TaskTopic;
import com.example.javaoffer.exam.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;

import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для административной панели приложения.
 * <p>
 * Тестирует функциональность админского контроллера:
 * <ul>
 *   <li>Управление вопросами экзамена (CRUD операции)</li>
 *   <li>Просмотр и управление пользователями</li>
 *   <li>Доступ к обратной связи</li>
 *   <li>Навигацию по административным страницам</li>
 * </ul>
 * </p>
 * <p>
 * Проверяет корректность:
 * <ul>
 *   <li>Аутентификации и авторизации администраторов</li>
 *   <li>HTTP статусов и редиректов</li>
 *   <li>Содержимого моделей представлений</li>
 *   <li>JSON API для работы с данными</li>
 *   <li>Валидации входных данных</li>
 * </ul>
 * </p>
 * <p>
 * Использует mock объекты для изоляции тестов от внешних зависимостей.
 * </p>
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@SpringBootTest
@Import(AdminControllerTest.MockedTaskServiceConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TaskService taskService;

	/**
	 * Настраивает тестовое окружение перед каждым тестом.
	 * <p>
	 * Выполняет:
	 * <ul>
	 *   <li>Переинициализацию MockMvc с веб-контекстом</li>
	 *   <li>Сброс состояния mock объектов</li>
	 * </ul>
	 * </p>
	 */
	@BeforeEach
	void setUp() {
		log.debug("Настройка тестового окружения для AdminControllerTest");
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		Mockito.reset(taskService);
		log.trace("Mock объекты сброшены, MockMvc переинициализирован");
	}

	/**
	 * Тестирует редирект с корневого пути админки на страницу управления вопросами.
	 * <p>
	 * Проверяет, что доступ к корневому пути админки перенаправляет пользователя
	 * на страницу управления вопросами как основную функцию.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testAdminRootPath() throws Exception {
		log.info("Тестирование редиректа с корневого пути админки");
		
		mockMvc.perform(get(URL_ADMIN_ROOT))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(VIEW_TEMPLATE_ADMIN_QUESTIONS_WITH_SLASH));
		
		log.debug("Редирект с корневого пути админки выполнен корректно");
	}

	/**
	 * Тестирует отображение страницы управления вопросами.
	 * <p>
	 * Проверяет:
	 * <ul>
	 *   <li>Загрузку всех вопросов из сервиса</li>
	 *   <li>Корректность возвращаемого представления</li>
	 *   <li>Наличие необходимых атрибутов модели</li>
	 *   <li>Вызов соответствующих методов сервиса</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testQuestionsPage() throws Exception {
		log.info("Тестирование страницы управления вопросами");
		
		log.debug("Создание тестовых данных вопросов");
		TaskDTO task1 = new TaskDTO();
		task1.setQuestion("Test Question 1");
		task1.setTopic(TaskTopic.CORE);
		task1.setDifficulty(TaskDifficulty.MEDIUM1);
		task1.setGrade(TaskGrade.JUNIOR);

		TaskDTO task2 = new TaskDTO();
		task2.setQuestion("Test Question 2");
		task2.setTopic(TaskTopic.SPRING);
		task2.setDifficulty(TaskDifficulty.HARD1);
		task2.setGrade(TaskGrade.SENIOR);

		List<TaskDTO> mockTasks = Arrays.asList(task1, task2);
		when(taskService.getAllTasks()).thenReturn(mockTasks);

		log.debug("Выполнение GET запроса к странице вопросов");
		mockMvc.perform(get(URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("admin/questions"))
				.andExpect(model().attributeExists("tasks"))
				.andExpect(model().attributeExists("newTask"))
				.andExpect(model().attributeExists("task"));

		verify(taskService, times(1)).getAllTasks();
		log.info("Страница управления вопросами протестирована успешно");
	}

	/**
	 * Тестирует создание нового вопроса через админскую панель.
	 * <p>
	 * Проверяет:
	 * <ul>
	 *   <li>Отправку POST запроса с данными вопроса</li>
	 *   <li>Вызов метода создания в сервисе</li>
	 *   <li>Редирект на страницу управления вопросами</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testCreateTask() throws Exception {
		log.info("Тестирование создания нового вопроса");
		
		log.debug("Подготовка данных для создания вопроса");
		TaskDTO taskDTO = new TaskDTO();
		taskDTO.setQuestion("Test Question");
		taskDTO.setTopic(TaskTopic.CORE);
		taskDTO.setDifficulty(TaskDifficulty.MEDIUM1);
		taskDTO.setGrade(TaskGrade.JUNIOR);

		when(taskService.createTask(any(TaskDTO.class))).thenReturn(taskDTO);

		log.debug("Отправка POST запроса для создания вопроса");
		mockMvc.perform(post(URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS)
						.with(SecurityMockMvcRequestPostProcessors.csrf())
						.param("question", taskDTO.getQuestion())
						.param("topic", taskDTO.getTopic().name())
						.param("difficulty", taskDTO.getDifficulty().name())
						.param("grade", taskDTO.getGrade().name()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS));

		verify(taskService, times(1)).createTask(any(TaskDTO.class));
		log.info("Создание нового вопроса протестировано успешно");
	}

	/**
	 * Тестирует редактирование существующего вопроса.
	 * <p>
	 * Проверяет:
	 * <ul>
	 *   <li>Загрузку данных конкретного вопроса для редактирования</li>
	 *   <li>Отображение формы редактирования</li>
	 *   <li>Корректность атрибутов модели</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testEditTask() throws Exception {
		log.info("Тестирование редактирования вопроса");
		
		Long taskId = 1L;
		log.debug("Подготовка тестовых данных для вопроса с ID: {}", taskId);
		
		TaskDTO mockTask = new TaskDTO();
		mockTask.setQuestion("Test Question");
		mockTask.setTopic(TaskTopic.CORE);
		mockTask.setDifficulty(TaskDifficulty.MEDIUM1);
		mockTask.setGrade(TaskGrade.JUNIOR);

		List<TaskDTO> mockTasks = List.of(mockTask);
		when(taskService.getAllTasks()).thenReturn(mockTasks);
		when(taskService.getTaskById(taskId)).thenReturn(mockTask);

		log.debug("Выполнение GET запроса для редактирования вопроса");
		mockMvc.perform(get(URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS + "/{id}", taskId)
						.with(SecurityMockMvcRequestPostProcessors.csrf())
						.requestAttr("request", new MockHttpServletRequest())
						.flashAttr("newTask", new TaskDTO())
						.flashAttr("task", mockTask)
						.flashAttr("tasks", mockTasks))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_ADMIN_QUESTIONS))
				.andExpect(model().attribute("task", mockTask))
				.andExpect(model().attribute("tasks", mockTasks))
				.andExpect(model().attribute("newTask", new TaskDTO()));

		verify(taskService, times(1)).getTaskById(taskId);
		log.info("Редактирование вопроса протестировано успешно");
	}

	/**
	 * Тестирует обновление данных вопроса через REST API.
	 * <p>
	 * Проверяет:
	 * <ul>
	 *   <li>Отправку PUT запроса с JSON данными</li>
	 *   <li>Вызов метода обновления в сервисе</li>
	 *   <li>Возврат статуса 200 OK</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса или сериализации JSON
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testUpdateTask() throws Exception {
		log.info("Тестирование обновления вопроса через REST API");
		
		Long taskId = 1L;
		log.debug("Подготовка данных для обновления вопроса с ID: {}", taskId);
		
		TaskDTO taskDTO = new TaskDTO();
		taskDTO.setQuestion("Updated Question");
		taskDTO.setTopic(TaskTopic.SPRING);
		taskDTO.setDifficulty(TaskDifficulty.MEDIUM2);
		taskDTO.setGrade(TaskGrade.MIDDLE);

		when(taskService.updateTask(eq(taskId), any(TaskDTO.class))).thenReturn(taskDTO);

		log.debug("Отправка PUT запроса с JSON данными");
		mockMvc.perform(put(URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS + "/{id}", taskId)
						.with(SecurityMockMvcRequestPostProcessors.csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(taskDTO)))
				.andExpect(status().isOk());

		verify(taskService, times(1)).updateTask(eq(taskId), any(TaskDTO.class));
		log.info("Обновление вопроса через REST API протестировано успешно");
	}

	/**
	 * Тестирует удаление вопроса из админской панели.
	 * <p>
	 * Проверяет:
	 * <ul>
	 *   <li>Отправку POST запроса на удаление</li>
	 *   <li>Вызов метода удаления в сервисе</li>
	 *   <li>Редирект на страницу управления вопросами</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testDeleteTask() throws Exception {
		log.info("Тестирование удаления вопроса");
		
		Long taskId = 1L;
		log.debug("Подготовка к удалению вопроса с ID: {}", taskId);
		
		doNothing().when(taskService).deleteTask(taskId);

		log.debug("Отправка POST запроса на удаление");
		mockMvc.perform(post(URL_ADMIN_QUESTIONS_ID_DELETE, taskId)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(VIEW_TEMPLATE_ADMIN_QUESTIONS_WITH_SLASH));

		verify(taskService, times(1)).deleteTask(taskId);
		log.info("Удаление вопроса протестировано успешно");
	}

	/**
	 * Тестирует получение данных вопроса в формате JSON.
	 * <p>
	 * Проверяет:
	 * <ul>
	 *   <li>GET запрос для получения данных конкретного вопроса</li>
	 *   <li>Возврат данных в формате JSON</li>
	 *   <li>Корректность Content-Type заголовка</li>
	 * </ul>
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testGetTaskData() throws Exception {
		log.info("Тестирование получения данных вопроса в JSON формате");
		
		Long taskId = 1L;
		log.debug("Подготовка тестовых данных для вопроса с ID: {}", taskId);
		
		TaskDTO mockTask = new TaskDTO();
		mockTask.setQuestion("Test Question");
		mockTask.setTopic(TaskTopic.HIBERNATE);
		mockTask.setDifficulty(TaskDifficulty.HARD2);
		mockTask.setGrade(TaskGrade.SENIOR);

		when(taskService.getTaskById(taskId)).thenReturn(mockTask);

		log.debug("Выполнение GET запроса для получения JSON данных");
		mockMvc.perform(get(URL_ADMIN_QUESTIONS_ID_DATA, taskId)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

		verify(taskService, times(1)).getTaskById(taskId);
		log.info("Получение данных вопроса в JSON формате протестировано успешно");
	}

	/**
	 * Тестирует отображение страницы управления пользователями.
	 * <p>
	 * Проверяет доступность административной страницы для управления
	 * пользователями системы.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testUsersPage() throws Exception {
		log.info("Тестирование страницы управления пользователями");
		
		mockMvc.perform(get(URL_ADMIN_ROOT + URL_ADMIN_USERS)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_ADMIN_USERS));
		
		log.info("Страница управления пользователями протестирована успешно");
	}

	/**
	 * Тестирует отображение страницы обратной связи в админской панели.
	 * <p>
	 * Проверяет доступность административной страницы для просмотра
	 * обратной связи от пользователей.
	 * </p>
	 *
	 * @throws Exception при ошибках выполнения HTTP запроса
	 */
	@Test
	@WithMockUser(roles = "ADMIN")
	void testContactPage() throws Exception {
		log.info("Тестирование страницы обратной связи");
		
		mockMvc.perform(get(URL_ADMIN_ROOT + URL_CONTACT)
						.with(SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name(VIEW_TEMPLATE_ADMIN_FEEDBACK));
		
		log.info("Страница обратной связи протестирована успешно");
	}

	/**
	 * Тестовая конфигурация для создания mock объектов.
	 * <p>
	 * Предоставляет mock реализацию TaskService для изоляции тестов
	 * от реальной бизнес-логики и базы данных.
	 * </p>
	 */
	@TestConfiguration
	static class MockedTaskServiceConfig {
		
		/**
		 * Создает mock объект TaskService для тестирования.
		 *
		 * @return mock объект TaskService
		 */
		@Bean
		public TaskService taskService() {
			return Mockito.mock(TaskService.class);
		}
	}
}