package com.example.javaoffer.admin.controller;

import com.example.javaoffer.admin.dto.ImportExportHistoryDTO;
import com.example.javaoffer.admin.entity.ImportExportHistory;
import com.example.javaoffer.admin.service.ImportExportHistoryService;
import com.example.javaoffer.admin.service.QuestionExportService;
import com.example.javaoffer.admin.service.QuestionImportService;
import com.example.javaoffer.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_ADMIN_IMPORT_EXPORT_HISTORY;

/**
 * Контроллер для управления историей импорта и экспорта вопросов.
 * <p>
 * Предоставляет функционал для:
 * <ul>
 *   <li>Просмотра истории импорта/экспорта вопросов</li>
 *   <li>Экспорта вопросов в Excel-файл</li>
 *   <li>Импорта вопросов из Excel-файла</li>
 *   <li>Очистки истории импорта/экспорта</li>
 * </ul>
 *
 * @author Garbuzov Oleg
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ImportExportHistoryController {
	private final ImportExportHistoryService historyService;
	private final QuestionExportService questionExportService;
	private final QuestionImportService questionImportService;

	/**
	 * Отображает страницу с историей импорта и экспорта вопросов.
	 *
	 * @param model   модель для передачи данных в представление
	 * @param request HTTP-запрос
	 * @return имя представления страницы истории импорта/экспорта
	 */
	@GetMapping(URL_ADMIN_ROOT + URL_ADMIN_IMPORT_EXPORT_HISTORY)
	public String getHistoryPage(Model model, HttpServletRequest request) {
		log.debug("Запрос страницы истории импорта/экспорта вопросов");
		List<ImportExportHistoryDTO> historyList = historyService.findAll();
		log.trace("Получено {} записей истории импорта/экспорта", historyList.size());

		model.addAttribute("historyList", historyList);
		model.addAttribute("request", request);
		return VIEW_TEMPLATE_ADMIN_IMPORT_EXPORT_HISTORY;
	}

	/**
	 * Экспортирует вопросы в Excel-файл.
	 *
	 * @param response HTTP-ответ для записи Excel-файла
	 * @param user     текущий аутентифицированный пользователь
	 * @throws IOException если произошла ошибка при записи в поток ответа
	 */
	@GetMapping(URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS_EXPORT_EXCEL)
	@ResponseBody
	public void exportQuestionsToExcel(HttpServletResponse response, @AuthenticationPrincipal User user) throws IOException {
		log.info("Начат экспорт вопросов в Excel пользователем: {}", user.getUsername());

		String fileName = "questions_export.xlsx";
		ImportExportHistory history = historyService.createExportHistory(user, fileName);
		log.debug("Создана запись истории экспорта с id: {}", history.getId());

		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

		try {
			questionExportService.exportQuestionsToExcel(response.getOutputStream(), history);
			log.info("Экспорт вопросов в Excel успешно завершен пользователем: {}", user.getUsername());
		} catch (Exception e) {
			log.error("Ошибка при экспорте вопросов в Excel: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Импортирует вопросы из Excel-файла.
	 *
	 * @param file загруженный Excel-файл
	 * @param user текущий аутентифицированный пользователь
	 * @return строка перенаправления на страницу истории импорта/экспорта
	 */
	@PostMapping(URL_ADMIN_ROOT + URL_ADMIN_QUESTIONS_IMPORT_EXCEL)
	public String importQuestionsFromExcel(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal User user) {
		log.info("Начат импорт вопросов из Excel-файла '{}' пользователем: {}",
				file.getOriginalFilename(), user.getUsername());

		ImportExportHistory history = historyService.createImportHistory(user, file.getOriginalFilename());
		log.debug("Создана запись истории импорта с id: {}", history.getId());

		try {
			questionImportService.importQuestionsAsync(file, history);
			log.debug("Запущен асинхронный процесс импорта вопросов из файла '{}'", file.getOriginalFilename());
		} catch (Exception e) {
			log.error("Ошибка при запуске импорта вопросов из Excel-файла: {}", e.getMessage(), e);
			historyService.markAsError(history, "Ошибка при запуске импорта: " + e.getMessage());
		}

		return "redirect:" + URL_ADMIN_ROOT + URL_ADMIN_IMPORT_EXPORT_HISTORY;
	}

	/**
	 * Очищает историю импорта и экспорта вопросов.
	 *
	 * @return строка перенаправления на страницу истории импорта/экспорта
	 */
	@PostMapping(URL_ADMIN_ROOT + URL_ADMIN_IMPORT_EXPORT_HISTORY_CLEAR)
	public String clearHistory() {
		log.info("Запрос на очистку истории импорта/экспорта вопросов");

		try {
			historyService.deleteAll();
			log.info("История импорта/экспорта вопросов успешно очищена");
		} catch (Exception e) {
			log.error("Ошибка при очистке истории импорта/экспорта вопросов: {}", e.getMessage(), e);
		}

		return "redirect:" + URL_ADMIN_ROOT + URL_ADMIN_IMPORT_EXPORT_HISTORY;
	}
} 