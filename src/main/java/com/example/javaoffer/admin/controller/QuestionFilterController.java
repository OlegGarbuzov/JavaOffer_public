package com.example.javaoffer.admin.controller;

import com.example.javaoffer.exam.dto.TaskDTO;
import com.example.javaoffer.exam.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.example.javaoffer.common.constants.UrlConstant.URL_ADMIN_FILTER_QUESTIONS;
import static com.example.javaoffer.common.constants.UrlConstant.URL_ADMIN_FILTER_ROOT;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_ADMIN_QUESTIONS_TBODY;

/**
 * Контроллер для фильтрации вопросов в админ-панели.
 * <p>
 * Выделен в отдельный контроллер для избежания конфликта маршрутов.
 * Обрабатывает AJAX-запросы для динамической фильтрации вопросов
 * по различным критериям без полной перезагрузки страницы.
 *
 * @author Garbuzov Oleg
 */
@Controller
@RequestMapping(URL_ADMIN_FILTER_ROOT)
@RequiredArgsConstructor
@Slf4j
public class QuestionFilterController {

	private final TaskService taskService;

	/**
	 * Обрабатывает AJAX-запросы для фильтрации вопросов.
	 * <p>
	 * Возвращает HTML-фрагмент с отфильтрованными вопросами для динамического обновления таблицы.
	 * Поддерживает фильтрацию по теме, сложности, оценке, текстовому поиску и показ дублей.
	 *
	 * @param topic          тема для фильтрации (опционально)
	 * @param difficulty     уровень сложности для фильтрации (опционально)
	 * @param grade          оценка для фильтрации (опционально)
	 * @param search         строка поиска по тексту вопроса (опционально)
	 * @param showDuplicates показывать только вопросы с дублирующимся текстом как строка (опционально)
	 * @param model          модель для передачи данных в представление
	 * @return имя фрагмента с отфильтрованными строками таблицы вопросов
	 */
	@GetMapping(URL_ADMIN_FILTER_QUESTIONS)
	public String filterQuestions(
			@RequestParam(required = false) String topic,
			@RequestParam(required = false) String difficulty,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String search,
			@RequestParam(required = false, defaultValue = "false") String showDuplicates,
			Model model) {

		Boolean showDuplicatesBool = "true".equalsIgnoreCase(showDuplicates);

		log.debug("Запрос на фильтрацию вопросов с параметрами: topic={}, difficulty={}, grade={}, search={}, showDuplicates={}",
				topic, difficulty, grade, search, showDuplicatesBool);

		List<TaskDTO> filtered = taskService.filterTasks(topic, difficulty, grade, search, showDuplicatesBool);

		log.trace("Отфильтровано {} вопросов", filtered.size());

		model.addAttribute("tasks", filtered);
		return VIEW_TEMPLATE_ADMIN_QUESTIONS_TBODY;
	}
} 