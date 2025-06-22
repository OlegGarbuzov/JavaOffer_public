package com.example.javaoffer.exam.cache.controller;

import com.example.javaoffer.common.constants.UrlConstant;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.dto.CacheDebugProgressDTO;
import com.example.javaoffer.exam.enums.ExamMode;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.javaoffer.common.constants.UrlConstant.URL_ADMIN_DEBUG_CACHE_FULL;
import static com.example.javaoffer.common.constants.ViewConstant.VIEW_TEMPLATE_ADMIN_CACHE_DEBUG;

/**
 * Контроллер для отладки и мониторинга кэша прогресса экзаменов.
 * <p>
 * Предоставляет администраторам доступ к информации о текущих экзаменационных сессиях,
 * хранящихся в кэше. Позволяет отслеживать активные сессии, их статус, время жизни,
 * статистику нарушений и heartbeat-сообщений.
 *
 * <p>
 * Используется для диагностики и отладки проблем с экзаменационными сессиями,
 * а также для мониторинга нагрузки на систему.
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@Controller
@RequestMapping(URL_ADMIN_DEBUG_CACHE_FULL)
public class CacheDebugController {

	private final Cache<UUID, TemporaryExamProgress> temporaryExamProgressCache;

	/**
	 * Конструктор для внедрения кэша прогресса экзаменов.
	 *
	 * @param temporaryExamProgressCache кэш с данными о прогрессе экзаменов
	 */
	public CacheDebugController(Cache<UUID, TemporaryExamProgress> temporaryExamProgressCache) {
		this.temporaryExamProgressCache = temporaryExamProgressCache;
		log.info("Инициализирован контроллер отладки кэша");
	}

	/**
	 * Обрабатывает GET-запрос для просмотра содержимого кэша прогресса экзаменов.
	 * <p>
	 * Извлекает информацию о всех активных экзаменационных сессиях из кэша,
	 * преобразует её в DTO для отображения и добавляет в модель.
	 * Результаты сортируются по режиму экзамена (сначала RATING, затем FREE)
	 * и по длительности экзамена (более новые в начале).
	 *
	 * <p>
	 * Также формирует статистику по количеству сессий каждого типа.
	 *
	 * @param model   модель для передачи данных в представление
	 * @param request HTTP-запрос
	 * @return имя представления для отображения содержимого кэша
	 */
	@GetMapping(UrlConstant.URL_ADMIN_DEBUG_CACHE_PROGRESS)
	public String getCacheContents(Model model, HttpServletRequest request) {
		log.debug("Получен запрос на отображение содержимого кэша прогресса экзаменов");
		List<CacheDebugProgressDTO> processedData = new ArrayList<>();
		Map<UUID, TemporaryExamProgress> cacheEntries = temporaryExamProgressCache.asMap();

		// Текущее время для расчета продолжительности экзамена и времени с последнего heartbeat
		Instant now = Instant.now();

		for (Map.Entry<UUID, TemporaryExamProgress> entry : cacheEntries.entrySet()) {
			UUID examId = entry.getKey();
			TemporaryExamProgress progress = entry.getValue();

			CacheDebugProgressDTO examData = createProgressDTO(examId, progress, now);
			processedData.add(examData);

			log.trace("Обработана запись кэша: examId={}, режим={}, сложность={}, создана={}",
					examId, progress.getExamMode(), progress.getCurrentDifficulty(), progress.getProgressCreateAt());
		}

		// Сортировка по режиму и длительности (сначала RATING, потом FREE)
		List<CacheDebugProgressDTO> sortedExams = processedData.stream()
				.sorted((a, b) -> {
					// Сначала сортируем по режиму (RATING перед FREE)
					ExamMode modeA = a.getExamMode();
					ExamMode modeB = b.getExamMode();

					if (modeA != null && modeB != null) {
						if (modeA == ExamMode.RATING && modeB != ExamMode.RATING) {
							return -1;
						} else if (modeA != ExamMode.RATING && modeB == ExamMode.RATING) {
							return 1;
						}
					}

					// Затем сортируем по времени (более новые сначала)
					Long durationA = a.getDurationMinutes();
					Long durationB = b.getDurationMinutes();

					if (durationA != null && durationB != null) {
						return durationB.compareTo(durationA);
					}

					return 0;
				})
				.collect(Collectors.toList());

		// Статистика по типам экзаменов
		Map<ExamMode, Long> examModeStats = processedData.stream()
				.filter(data -> data.getExamMode() != null)
				.collect(Collectors.groupingBy(
						CacheDebugProgressDTO::getExamMode,
						Collectors.counting()
				));

		// Заполняем модель данными
		model.addAttribute("timestamp", now.toString());
		model.addAttribute("totalEntries", cacheEntries.size());
		model.addAttribute("entries", sortedExams);
		model.addAttribute("request", request);
		model.addAttribute("ratingCount", examModeStats.getOrDefault(ExamMode.RATING, 0L));
		model.addAttribute("freeCount", examModeStats.getOrDefault(ExamMode.FREE, 0L));

		log.info("Отображение информации о кэше: всего сессий={}, рейтинговых={}, свободных={}",
				cacheEntries.size(),
				examModeStats.getOrDefault(ExamMode.RATING, 0L),
				examModeStats.getOrDefault(ExamMode.FREE, 0L));

		return VIEW_TEMPLATE_ADMIN_CACHE_DEBUG;
	}

	/**
	 * Создает DTO объект для отображения информации о прогрессе экзамена.
	 * <p>
	 * Преобразует данные из доменной модели кэша в DTO,
	 * добавляя вычисляемые поля, такие как продолжительность экзамена
	 * и время с последнего heartbeat-сообщения.
	 *
	 * @param examId   идентификатор экзамена
	 * @param progress объект с данными о прогрессе экзамена
	 * @param now      текущее время
	 * @return DTO объект с данными для отображения
	 */
	private CacheDebugProgressDTO createProgressDTO(UUID examId, TemporaryExamProgress progress, Instant now) {
		CacheDebugProgressDTO examData = new CacheDebugProgressDTO();
		examData.setExamId(examId);
		examData.setExamMode(progress.getExamMode());
		examData.setCurrentDifficulty(progress.getCurrentDifficulty());
		examData.setProgressCreateAt(progress.getProgressCreateAt());

		// Продолжительность экзамена
		if (progress.getProgressCreateAt() != null) {
			Instant startTime = progress.getProgressCreateAt().toInstant(java.time.ZoneOffset.UTC);
			Duration examDuration = Duration.between(startTime, now);
			examData.setDurationMinutes((long) Math.round(examDuration.toMinutes()));
		}

		// Статистика ответов
		examData.setSuccessAnswersCountAbsolute(progress.getSuccessAnswersCountAbsolute());
		examData.setFailAnswersCountAbsolute(progress.getFailAnswersCountAbsolute());
		examData.setCurrentBasePoint(progress.getCurrentBasePoint());

		// Статистика нарушений
		examData.setTabSwitchViolationCount(progress.getTabSwitchViolationCount());
		examData.setTextCopyViolationCount(progress.getTextCopyViolationCount());
		examData.setHeartbeatMissedCount(progress.getHeartbeatMissedCount());
		examData.setDevToolsViolationCount(progress.getDevToolsViolationCount());
		examData.setDomTamperingViolationCount(progress.getDomTamperingViolationCount());
		examData.setFunctionTamperingViolationCount(progress.getFunctionTamperingViolationCount());
		examData.setModuleTamperingViolationCount(progress.getModuleTamperingViolationCount());
		examData.setPageCloseViolationCount(progress.getPageCloseViolationCount());
		examData.setExternalContentViolationCount(progress.getExternalContentViolationCount());
		examData.setAntiOcrTamperingViolations(progress.getAntiOcrTamperingViolationCount());
		examData.setTerminatedByViolations(progress.isTerminatedByViolations());

		// Статистика heartbeat
		if (progress.getLastHeartbeatTime() != null) {
			long secondsSinceLastHeartbeat = ChronoUnit.SECONDS.between(progress.getLastHeartbeatTime(), now);
			examData.setLastHeartbeatSecondsAgo(secondsSinceLastHeartbeat);
			examData.setHeartbeatStatus((secondsSinceLastHeartbeat > 30) ? "ПРОБЛЕМА" : "OK");
		} else {
			examData.setLastHeartbeatSecondsAgo(null);
			examData.setHeartbeatStatus("Нет данных");
		}

		return examData;
	}
}
