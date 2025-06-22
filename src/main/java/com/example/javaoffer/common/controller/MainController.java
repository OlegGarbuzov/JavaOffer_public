package com.example.javaoffer.common.controller;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.exam.entity.GlobalRatingScoreHistory;
import com.example.javaoffer.exam.service.GlobalRatingScoreHistoryService;
import com.example.javaoffer.rateLimiter.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.example.javaoffer.common.constants.UrlConstant.*;
import static com.example.javaoffer.common.constants.ViewConstant.*;

/**
 * Основной контроллер приложения.
 * <p>
 * Отвечает за обработку запросов к основным страницам:
 * <ul>
 *   <li>Главная страница</li>
 *   <li>Страница обратной связи</li>
 *   <li>Страница выбора режима экзамена</li>
 *   <li>Страница глобального рейтинга</li>
 * </ul>
 * 
 */
@Controller
@Slf4j
public class MainController {
	private final GlobalRatingScoreHistoryService globalRatingScoreHistoryService;

	/**
	 * Конструктор контроллера.
	 *
	 * @param globalRatingScoreHistoryService сервис для работы с историей глобального рейтинга
	 */
	public MainController(GlobalRatingScoreHistoryService globalRatingScoreHistoryService) {
		this.globalRatingScoreHistoryService = globalRatingScoreHistoryService;
	}

	/**
	 * Обрабатывает запрос к корневому URL и отображает главную страницу.
	 * Добавляет в модель топ-5 пользователей по рейтингу.
	 *
	 * @param request HTTP-запрос
	 * @param model модель для передачи данных в представление
	 * @return имя шаблона представления главной страницы
	 */
	@GetMapping(URL_ROOT)
	@RateLimit
	public String root(HttpServletRequest request, Model model) {
		String clientIp = ClientUtils.getClientIp(request);
		model.addAttribute("request", request);
		model.addAttribute("top5", globalRatingScoreHistoryService.getTop5ByScore());
		log.info("{}:GET /: Запрос главной страницы", clientIp);
		return VIEW_TEMPLATE_INDEX;
	}

	/**
	 * Обрабатывает запрос к странице контактов.
	 *
	 * @param request HTTP-запрос
	 * @param model модель для передачи данных в представление
	 * @return имя шаблона представления страницы контактов
	 */
	@GetMapping(URL_CONTACT)
	@RateLimit
	public String contact(HttpServletRequest request, Model model) {
		String clientIp = ClientUtils.getClientIp(request);
		model.addAttribute("request", request);
		log.info("{}:GET /contact: Запрос страницы контактов", clientIp);
		return VIEW_TEMPLATE_CONTACT;
	}

	/**
	 * Обрабатывает запрос к странице выбора режима экзамена.
	 *
	 * @param request HTTP-запрос
	 * @param model модель для передачи данных в представление
	 * @return имя шаблона представления страницы выбора режима
	 */
	@GetMapping(URL_MODE_SELECT)
	@RateLimit
	public String modeSelect(HttpServletRequest request, Model model) {
		String clientIp = ClientUtils.getClientIp(request);
		model.addAttribute("request", request);
		log.info("{}:GET /modeSelect: Запрос страницы выбора режима экзамена", clientIp);
		return VIEW_TEMPLATE_MODE_SELECT;
	}

	/**
	 * Обрабатывает запрос к странице глобального рейтинга.
	 * Поддерживает пагинацию результатов.
	 *
	 * @param page номер страницы (начиная с 0)
	 * @param request HTTP-запрос
	 * @param model модель для передачи данных в представление
	 * @return имя шаблона представления страницы глобального рейтинга
	 */
	@GetMapping(URL_GLOBAL_RATING)
	@RateLimit
	public String globalRatingPage(@RequestParam(defaultValue = "0") int page, HttpServletRequest request, Model model) {
		String clientIp = ClientUtils.getClientIp(request);
		Pageable pageable = PageRequest.of(page, 100);
		Page<GlobalRatingScoreHistory> ratingPage = globalRatingScoreHistoryService.findAll(pageable);
		model.addAttribute("ratingPage", ratingPage);
		model.addAttribute("request", request);
		log.info("{}:GET /global-rating: Запрос страницы глобального рейтинга, страница {}", clientIp, page);
		return "global-rating";
	}
}
