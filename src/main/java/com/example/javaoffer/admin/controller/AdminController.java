package com.example.javaoffer.admin.controller;

import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.dto.UserScoreHistoryDTO;
import com.example.javaoffer.exam.service.TaskService;
import com.example.javaoffer.exam.service.UserScoreHistoryService;
import com.example.javaoffer.feedback.dto.FeedBackResponseDTO;
import com.example.javaoffer.feedback.service.FeedBackService;
import com.example.javaoffer.user.dto.UserDTO;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.repository.UserRepository;
import com.example.javaoffer.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.*;

/**
 * Контроллер административной панели.
 * <p>
 * Предоставляет доступ к функциям управления системой:
 * <ul>
 *   <li>Управление вопросами (просмотр, создание, редактирование, удаление)</li>
 *   <li>Управление пользователями (просмотр, создание, редактирование)</li>
 *   <li>Просмотр обратной связи от пользователей</li>
 *   <li>Настройки системы защиты от OCR</li>
 * </ul>
 * 
 * <p>
 * Доступ к этому контроллеру имеют только пользователи с административными правами.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Controller
@RequestMapping(URL_ADMIN_ROOT)
@RequiredArgsConstructor
@Slf4j
public class AdminController {
	private final TaskService taskService;
	private final UserService userService;
	private final UserScoreHistoryService userScoreHistoryService;
	private final UserRepository userRepository;
	private final FeedBackService feedBackService;

	/**
	 * Обрабатывает запрос к корневому пути административной панели.
	 * Перенаправляет на страницу управления вопросами.
	 *
	 * @param request HTTP-запрос
	 * @param model   модель для передачи данных в представление
	 * @return строка перенаправления на страницу вопросов
	 */
	@GetMapping
	public String admin(HttpServletRequest request, Model model) {
		log.debug("Получен запрос к корневому пути административной панели");
		model.addAttribute("request", request);
		return "redirect:" + URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS;
	}

	/**
	 * Отображает страницу управления вопросами.
	 *
	 * @param request HTTP-запрос
	 * @param model   модель для передачи данных в представление
	 * @return имя представления страницы вопросов
	 */
	@GetMapping(URL_ADMIN_QUESTIONS)
	public String questions(HttpServletRequest request, Model model) {
		log.debug("Отображение страницы управления вопросами");
		model.addAttribute("request", request);
		model.addAttribute("tasks", taskService.getAllTasks());
		model.addAttribute("newTask", new TaskDTO());
		model.addAttribute("task", new TaskDTO());
		return VIEW_TEMPLATE_ADMIN_QUESTIONS;
	}

	/**
	 * Обрабатывает запрос на создание нового вопроса.
	 *
	 * @param taskDTO            данные нового вопроса
	 * @param bindingResult      результат валидации данных
	 * @param redirectAttributes атрибуты перенаправления для передачи сообщений
	 * @return строка перенаправления на страницу вопросов
	 */
	@PostMapping(URL_ADMIN_QUESTIONS)
	public String createTasks(@Valid @ModelAttribute("newTask") TaskDTO taskDTO,
							  BindingResult bindingResult,
							  RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			log.warn("Ошибка валидации при создании вопроса: {}", bindingResult.getAllErrors());
			redirectAttributes.addFlashAttribute("error", "Пожалуйста, заполните все обязательные поля");
			return "redirect:" + URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS;
		}

		try {
			taskService.createTask(taskDTO);
			log.info("Успешно создан новый вопрос: {}", taskDTO.getQuestion());
			redirectAttributes.addFlashAttribute("success", "Вопрос успешно создан");
		} catch (Exception e) {
			log.error("Ошибка при создании вопроса: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("error", "Ошибка при создании вопроса: " + e.getMessage());
		}

		return "redirect:" + URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS;
	}

	/**
	 * Отображает страницу редактирования существующего вопроса.
	 *
	 * @param id      идентификатор вопроса
	 * @param request HTTP-запрос
	 * @param model   модель для передачи данных в представление
	 * @return имя представления страницы вопросов
	 */
	@GetMapping(URL_ADMIN_QUESTION_BY_ID)
	public String editTask(@PathVariable Long id, HttpServletRequest request, Model model) {
		log.debug("Отображение страницы редактирования вопроса с id: {}", id);
		model.addAttribute("request", request);
		model.addAttribute("task", taskService.getTaskById(id));
		return VIEW_TEMPLATE_ADMIN_QUESTIONS;
	}

	/**
	 * Обрабатывает запрос на обновление существующего вопроса.
	 *
	 * @param id            идентификатор вопроса
	 * @param taskDTO       обновленные данные вопроса
	 * @param bindingResult результат валидации данных
	 * @return обновленные данные задачи в формате JSON
	 * @throws IllegalArgumentException если данные не прошли валидацию
	 * @throws RuntimeException если произошла ошибка при обновлении вопроса
	 */
	@PutMapping(URL_ADMIN_QUESTION_BY_ID)
	@ResponseBody
	public TaskDTO updateTask(@PathVariable Long id,
							  @Valid @RequestBody TaskDTO taskDTO,
							  BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			String errorMessage = bindingResult.getAllErrors().stream()
					.map(DefaultMessageSourceResolvable::getDefaultMessage)
					.collect(Collectors.joining(", "));
			log.warn("Ошибка валидации при обновлении вопроса с id {}: {}", id, errorMessage);
			throw new IllegalArgumentException("Ошибка валидации: " + errorMessage);
		}
		try {
			TaskDTO updatedTask = taskService.updateTask(id, taskDTO);
			log.info("Успешно обновлен вопрос с id: {}", id);
			return updatedTask;
		} catch (Exception e) {
			log.error("Ошибка при обновлении вопроса с id {}: {}", id, e.getMessage(), e);
			throw new RuntimeException("Ошибка при обновлении вопроса: " + e.getMessage());
		}
	}

	/**
	 * Обрабатывает запрос на удаление вопроса.
	 *
	 * @param id                 идентификатор вопроса
	 * @param redirectAttributes атрибуты перенаправления для передачи сообщений
	 * @return строка перенаправления на страницу вопросов
	 */
	@PostMapping(URL_ADMIN_QUESTION_DELETE)
	public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			taskService.deleteTask(id);
			log.info("Успешно удален вопрос с id: {}", id);
			redirectAttributes.addFlashAttribute("success", "Вопрос успешно удален");
		} catch (Exception e) {
			log.error("Ошибка при удалении вопроса с id {}: {}", id, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("error", "Ошибка при удалении вопроса: " + e.getMessage());
		}

		return "redirect:" + URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS;
	}

	/**
	 * Отображает страницу управления пользователями.
	 *
	 * @param request HTTP-запрос
	 * @param model   модель для передачи данных в представление
	 * @return имя представления страницы пользователей
	 */
	@GetMapping(URL_ADMIN_USERS)
	public String users(HttpServletRequest request, Model model) {
		log.debug("Отображение страницы управления пользователями");
		model.addAttribute("request", request);
		return VIEW_TEMPLATE_ADMIN_USERS;
	}

	/**
	 * Отображает страницу обратной связи от пользователей.
	 *
	 * @param request HTTP-запрос
	 * @param model   модель для передачи данных в представление
	 * @return имя представления страницы обратной связи
	 */
	@GetMapping(URL_ADMIN_FEEDBACK)
	public String contact(HttpServletRequest request, Model model) {
		log.debug("Отображение страницы обратной связи от пользователей");
		model.addAttribute("request", request);
		return VIEW_TEMPLATE_ADMIN_FEEDBACK;
	}

	/**
	 * Отображает страницу настроек Anti-OCR.
	 *
	 * @param request HTTP-запрос
	 * @param model   модель для передачи данных в представление
	 * @return имя представления страницы настроек Anti-OCR
	 */
	@GetMapping(URL_ADMIN_OCR)
	public String interfaceSettings(HttpServletRequest request, Model model) {
		log.debug("Отображение страницы настроек Anti-OCR");
		model.addAttribute("request", request);
		return VIEW_TEMPLATE_ADMIN_ANTI_OCR_SETTINGS;
	}

	/**
	 * Возвращает данные задачи в формате JSON для редактирования.
	 *
	 * @param id идентификатор задачи
	 * @return данные задачи в формате JSON
	 */
	@GetMapping(URL_ADMIN_QUESTION_DATA)
	@ResponseBody
	public TaskDTO getTaskData(@PathVariable Long id) {
		log.debug("Запрос данных вопроса с id: {}", id);
		return taskService.getTaskById(id);
	}

	/**
	 * Получение списка пользователей с фильтрами и пагинацией.
	 *
	 * @param email имейл для фильтрации
	 * @param username имя пользователя для фильтрации
	 * @param role роль для фильтрации
	 * @param accountNonLocked статус блокировки для фильтрации
	 * @param id идентификатор пользователя для фильтрации
	 * @param page номер страницы
	 * @param size размер страницы
	 * @return страница с пользователями, соответствующими фильтрам
	 */
	@GetMapping(URL_ADMIN_API_USERS)
	@ResponseBody
	public Page<UserDTO> getUsers(
			@RequestParam(required = false) String email,
			@RequestParam(required = false) String username,
			@RequestParam(required = false) String role,
			@RequestParam(required = false) Boolean accountNonLocked,
			@RequestParam(required = false) Long id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size
	) {
		log.debug("Запрос списка пользователей с фильтрами: email={}, username={}, role={}, accountNonLocked={}, id={}, page={}, size={}",
				email, username, role, accountNonLocked, id, page, size);
		Pageable pageable = PageRequest.of(page, size);
		return userService.getUsers(email, username, role, accountNonLocked, id, pageable);
	}

	/**
	 * Получение данных одного пользователя по идентификатору.
	 *
	 * @param id идентификатор пользователя
	 * @return ответ с данными пользователя или 404, если пользователь не найден
	 */
	@GetMapping(URL_ADMIN_API_USER_BY_ID)
	@ResponseBody
	public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
		log.debug("Запрос данных пользователя с id: {}", id);
		UserDTO user = userService.getUserDTOById(id);
		if (user == null) {
			log.warn("Пользователь с id {} не найден", id);
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(user);
	}

	/**
	 * Получение истории экзаменов пользователя с фильтрацией и пагинацией.
	 *
	 * @param id идентификатор пользователя
	 * @param page номер страницы
	 * @param size размер страницы
	 * @param examId идентификатор экзамена для фильтрации (опционально)
	 * @return страница с историями экзаменов пользователя
	 */
	@GetMapping(URL_ADMIN_API_USER_SCORE_HISTORIES)
	@ResponseBody
	public Page<UserScoreHistoryDTO> getUserScoreHistories(
			@PathVariable Long id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String examId
	) {
		log.debug("Запрос истории экзаменов пользователя с id: {}, examId: {}, page: {}, size: {}", id, examId, page, size);
		return userScoreHistoryService.findByUserIdPaged(id, examId, PageRequest.of(page, size));
	}

	/**
	 * Создание нового пользователя.
	 *
	 * @param userDTO данные нового пользователя
	 * @return ответ с созданным пользователем или 400 в случае ошибки
	 */
	@PostMapping(URL_ADMIN_API_USERS)
	@ResponseBody
	public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
		try {
			log.debug("Запрос на создание нового пользователя: {}", userDTO.getUsername());
			User user = new User();
			user.setEmail(userDTO.getEmail());
			user.setUsername(userDTO.getUsername());
			user.setRole(com.example.javaoffer.user.enums.UserRole.valueOf(userDTO.getRole()));

			if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
				user.setPassword(userDTO.getPassword());
			} else {
				user.setPassword("defaultPassword");
			}

			User created = userService.createUser(user);
			log.info("Успешно создан новый пользователь с id: {}", created.getId());
			return ResponseEntity.ok(userService.getUserDTOById(created.getId()));
		} catch (Exception e) {
			log.error("Ошибка при создании пользователя: {}", e.getMessage(), e);
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Обновление данных пользователя.
	 *
	 * @param id идентификатор пользователя
	 * @param userDTO обновленные данные пользователя
	 * @return ответ с обновленными данными пользователя или 404, если пользователь не найден
	 */
	@PutMapping(URL_ADMIN_API_USER_BY_ID)
	@ResponseBody
	public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
		log.debug("Запрос на обновление пользователя с id: {}", id);
		Optional<User> userOpt = userRepository.findById(id);
		if (userOpt.isEmpty()) {
			log.warn("Пользователь с id {} не найден при попытке обновления", id);
			return ResponseEntity.notFound().build();
		}
		User user = userOpt.get();
		user.setEmail(userDTO.getEmail());
		user.setUsername(userDTO.getUsername());
		user.setRole(com.example.javaoffer.user.enums.UserRole.valueOf(userDTO.getRole()));
		user.setAccountNonLocked(userDTO.isAccountNonLocked());
		userService.save(user);
		log.info("Успешно обновлен пользователь с id: {}", id);
		return ResponseEntity.ok(userService.getUserDTOById(user.getId()));
	}

	/**
	 * Удаление истории экзамена пользователя.
	 *
	 * @param historyId идентификатор истории экзамена
	 * @return пустой ответ со статусом 200
	 */
	@DeleteMapping(URL_ADMIN_API_USER_SCORE_HISTORY_BY_ID)
	@ResponseBody
	public ResponseEntity<Void> deleteUserScoreHistory(@PathVariable Long historyId) {
		log.debug("Запрос на удаление истории экзамена с id: {}", historyId);
		userScoreHistoryService.deleteById(historyId);
		log.info("Успешно удалена история экзамена с id: {}", historyId);
		return ResponseEntity.ok().build();
	}

	/**
	 * Получение списка обратной связи с пагинацией.
	 *
	 * @param page номер страницы
	 * @param size размер страницы
	 * @return страница с обратной связью от пользователей
	 */
	@GetMapping(URL_ADMIN_API_FEEDBACK)
	@ResponseBody
	public Page<FeedBackResponseDTO> getFeedBacks(@RequestParam(defaultValue = "0") int page,
												  @RequestParam(defaultValue = "50") int size) {
		log.debug("Запрос списка обратной связи, page: {}, size: {}", page, size);
		Pageable pageable = PageRequest.of(page, size);
		return feedBackService.getAllFeedBacks(pageable);
	}

	/**
	 * Удаление обратной связи от пользователя.
	 *
	 * @param id идентификатор обратной связи
	 * @return пустой ответ со статусом 200
	 */
	@DeleteMapping(URL_ADMIN_API_FEEDBACK_BY_ID)
	@ResponseBody
	public ResponseEntity<Void> deleteFeedBack(@PathVariable Long id) {
		log.debug("Запрос на удаление обратной связи с id: {}", id);
		feedBackService.deleteFeedBack(id);
		log.info("Успешно удалена обратная связь с id: {}", id);
		return ResponseEntity.ok().build();
	}
}
