package com.example.javaoffer.exam.anticheat.controller;

import com.example.javaoffer.common.constants.UrlConstant;
import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.exam.anticheat.dto.SessionIntegrityResponseDTO;
import com.example.javaoffer.exam.anticheat.dto.SessionStatusResponseDTO;
import com.example.javaoffer.exam.anticheat.dto.UnifiedRequestDTO;
import com.example.javaoffer.exam.anticheat.enums.EventType;
import com.example.javaoffer.exam.anticheat.service.AntiCheatService;
import com.example.javaoffer.exam.cache.exception.NoEntryInCacheException;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.rateLimiter.annotation.RateLimit;
import com.google.common.util.concurrent.Striped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

import static com.example.javaoffer.common.constants.UrlConstant.URL_ANTICHEAT_STATUS;
import static com.example.javaoffer.common.constants.UrlConstant.URL_ANTICHEAT_STATUS_FULL;

/**
 * Унифицированный контроллер для обработки событий мониторинга сессии при прохождении экзамена.
 * <p>
 * Объединяет функциональность обработки heartbeat и событий нарушений через единый эндпоинт.
 * Контроллер принимает запросы от клиента, проверяет режим экзамена и делегирует обработку
 * соответствующим стратегиям в зависимости от типа события.
 * 
 * <p>
 * Используется потокобезопасная обработка запросов с помощью Striped Locks
 * для предотвращения одновременного доступа к одним и тем же данным сессии.
 * 
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@RestController
@RequestMapping(UrlConstant.URL_ANTICHEAT_API_ROOT)
@RequiredArgsConstructor
public class AntiCheatController {

	private final AntiCheatService antiCheatService;
	private final ExamSessionCacheService examSessionCacheService;
	private final Striped<Lock> locks = Striped.lock(10240);

	/**
	 * Унифицированный эндпоинт для обработки как heartbeat, так и событий нарушений.
	 * <p>
	 * Тип запроса определяется по полю eventType в запросе. Обработка выполняется только
	 * для экзаменов в рейтинговом режиме. Для других режимов возвращается стандартный ответ
	 * без обработки события.
	 * 
	 * <p>
	 * Метод использует блокировку для обеспечения потокобезопасной обработки запросов
	 * от одного пользователя.
	 * 
	 *
	 * @param requestDTO DTO с данными запроса, содержит идентификатор экзамена и тип события
	 * @param request    HTTP-запрос для получения IP клиента и CSRF токена
	 * @return ResponseEntity с результатом обработки запроса (SessionStatusResponseDTO или
	 *         SessionIntegrityResponseDTO в зависимости от типа события)
	 * @throws NoEntryInCacheException если сессия экзамена не найдена в кэше
	 */
	@PostMapping(URL_ANTICHEAT_STATUS)
	@ResponseBody
	@RateLimit
	public ResponseEntity<?> processUnifiedRequest(
			@RequestBody @Valid UnifiedRequestDTO requestDTO,
			HttpServletRequest request) {

		String clientIp = ClientUtils.getClientIp(request);
		UUID examId = requestDTO.getExamId();
		EventType eventType = requestDTO.getEventType();

		log.debug("{}:POST {}: Получен унифицированный запрос для examId={}, тип={}",
				clientIp, URL_ANTICHEAT_STATUS_FULL, examId, eventType);

		// Получаем объект, по которому будем лочить
		Object lockKey = ClientUtils.getLockKeyByXsrfOrIp(request);
		Lock lock = locks.get(lockKey);
		lock.lock();

		try {
			// Проверяем, существует ли экзамен и в рейтинговом ли он режиме
			ExamMode examMode = examSessionCacheService.getExamMode(examId).orElseThrow(() ->
					new NoEntryInCacheException(
							"Ошибка. Сессия не найдена. Вероятно вас долго не было. Начните сначала"
					));

			// Обрабатываем запрос только для рейтингового режима
			if (examMode != ExamMode.RATING) {
				log.debug("{}:POST {}: Запрос проигнорирован, режим не рейтинговый: {}",
						clientIp, URL_ANTICHEAT_STATUS_FULL, examMode);

				// Возвращаем соответствующий тип ответа в зависимости от типа события
				if (eventType == EventType.HEART_BEAT) {
					return ResponseEntity.ok(new SessionStatusResponseDTO(null, false, null));
				} else {
					return ResponseEntity.ok(new SessionIntegrityResponseDTO(false));
				}
			}

			// В зависимости от типа события выбирается соответствующая стратегия
			ResponseEntity<?> response = antiCheatService.process(requestDTO, clientIp);
			log.trace("{}:POST {}: Обработан запрос для examId={}, тип={}, ответ получен",
					clientIp, URL_ANTICHEAT_STATUS_FULL, examId, eventType);
			
			return response;
		} finally {
			lock.unlock();
		}
	}
} 