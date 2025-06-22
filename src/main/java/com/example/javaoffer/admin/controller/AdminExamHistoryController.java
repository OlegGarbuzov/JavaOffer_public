package com.example.javaoffer.admin.controller;

import com.example.javaoffer.exam.dto.UserScoreHistoryDTO;
import com.example.javaoffer.exam.service.UserScoreHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_ADMIN_EXAM_HISTORY;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_ADMIN_EXAM_HISTORY_DETAILS;

/**
 * Контроллер для административного интерфейса просмотра историй экзаменов.
 * <p>
 * Предоставляет функционал для:
 * <ul>
 *   <li>Просмотра списка историй экзаменов с фильтрацией и сортировкой</li>
 *   <li>Просмотра детальной информации об истории конкретного экзамена</li>
 *   <li>Удаления истории экзаменов</li>
 * </ul>
 * 
 *
 * @author Garbuzov Oleg
 */
@Controller
@RequestMapping(URL_ADMIN_ROOT + URL_ADMIN_EXAM_HISTORY_ROOT)
@RequiredArgsConstructor
@Slf4j
public class AdminExamHistoryController {

	private final UserScoreHistoryService userScoreHistoryService;

	/**
	 * Отображает страницу со списком историй экзаменов с возможностью фильтрации и сортировки.
	 *
	 * @param model              модель для передачи данных в представление
	 * @param page               номер страницы (начиная с 0)
	 * @param size               размер страницы
	 * @param sortField          поле для сортировки
	 * @param sortDir            направление сортировки (asc или desc)
	 * @param showViolationsOnly показывать только истории с нарушениями
	 * @param examId             фильтр по идентификатору экзамена
	 * @param userName           фильтр по имени пользователя
	 * @param request            HTTP-запрос
	 * @return имя представления страницы историй экзаменов
	 */
	@GetMapping
	public String showExamHistoryList(
			Model model,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@RequestParam(defaultValue = "createAt") String sortField,
			@RequestParam(defaultValue = "desc") String sortDir,
			@RequestParam(defaultValue = "false") boolean showViolationsOnly,
			@RequestParam(required = false) String examId,
			@RequestParam(required = false) String userName,
			HttpServletRequest request) {

		log.debug("Запрос списка историй экзаменов с параметрами: page={}, size={}, sortField={}, sortDir={}, " +
						"showViolationsOnly={}, examId={}, userName={}",
				page, size, sortField, sortDir, showViolationsOnly, examId, userName);

		Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
				Sort.by(sortField).ascending() : Sort.by(sortField).descending();

		Pageable pageable = PageRequest.of(page, size, sort);

		Page<UserScoreHistoryDTO> examHistoryPage = userScoreHistoryService.findAllWithFilters(
				pageable, showViolationsOnly, examId, userName);

		log.trace("Получено {} записей историй экзаменов из {} всего",
				examHistoryPage.getNumberOfElements(), examHistoryPage.getTotalElements());

		model.addAttribute("request", request);
		model.addAttribute("examHistoryPage", examHistoryPage);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", examHistoryPage.getTotalPages());
		model.addAttribute("totalItems", examHistoryPage.getTotalElements());
		model.addAttribute("sortField", sortField);
		model.addAttribute("sortDir", sortDir);
		model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
		model.addAttribute("showViolationsOnly", showViolationsOnly);
		model.addAttribute("examId", examId);
		model.addAttribute("userName", userName);

		return VIEW_TEMPLATE_ADMIN_EXAM_HISTORY;
	}

	/**
	 * Отображает детальную информацию об истории конкретного экзамена.
	 *
	 * @param model   модель для передачи данных в представление
	 * @param id      идентификатор истории экзамена
	 * @param request HTTP-запрос
	 * @return имя представления страницы с детальной информацией
	 */
	@GetMapping(URL_ADMIN_EXAM_HISTORY_DETAILS)
	public String showExamHistoryDetails(
			Model model,
			@RequestParam Long id,
			HttpServletRequest request) {

		log.debug("Запрос детальной информации об истории экзамена с id: {}", id);

		UserScoreHistoryDTO examHistory = userScoreHistoryService.findById(id);

		if (examHistory != null) {
			log.trace("Получена история экзамена: userId={}, examId={}, score={}",
					examHistory.getUser(), examHistory.getExamID(), examHistory.getScore());
		} else {
			log.warn("История экзамена с id {} не найдена", id);
		}

		model.addAttribute("examHistory", examHistory);
		model.addAttribute("request", request);

		return VIEW_TEMPLATE_ADMIN_EXAM_HISTORY_DETAILS;
	}

	/**
	 * Удаляет историю экзамена по идентификатору.
	 *
	 * @param id идентификатор истории экзамена
	 * @return редирект на страницу списка историй экзаменов
	 */
	@GetMapping(URL_ADMIN_EXAM_HISTORY_DELETE)
	public String deleteExamHistory(@RequestParam Long id) {
		log.debug("Запрос на удаление истории экзамена с id: {}", id);

		try {
			userScoreHistoryService.deleteById(id);
			log.info("История экзамена с id {} успешно удалена", id);
		} catch (Exception e) {
			log.error("Ошибка при удалении истории экзамена с id {}: {}", id, e.getMessage(), e);
		}

		return "redirect:" + URL_ADMIN_ROOT + URL_ADMIN_EXAM_HISTORY_ROOT;
	}
} 