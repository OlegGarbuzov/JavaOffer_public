package com.example.javaoffer.exam.anticheat.strategy;

import com.example.javaoffer.exam.anticheat.config.AntiCheatProperties;
import com.example.javaoffer.exam.anticheat.dto.SessionIntegrityResponseDTO;
import com.example.javaoffer.exam.anticheat.dto.UnifiedRequestDTO;
import com.example.javaoffer.exam.anticheat.enums.EventType;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.google.common.util.concurrent.Striped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

import static com.example.javaoffer.exam.anticheat.util.EventUtils.exceededTotalTamperingViolation;

/**
 * Стратегия обработки событий вмешательства в работу модуля Anti-OCR.
 * <p>
 * Отвечает за обработку событий, когда пользователь пытается отключить или модифицировать
 * механизм защиты от OCR-распознавания текста. Это считается серьезным нарушением правил,
 * так как позволяет пользователю обойти защиту от копирования вопросов через скриншоты
 * и последующее распознавание текста.
 * 
 * <p>
 * Вмешательство в работу Anti-OCR может включать:
 * <ul>
 *   <li>Блокирование загрузки скриптов Anti-OCR</li>
 *   <li>Модификацию функций защиты от OCR</li>
 *   <li>Блокирование получения конфигурации с сервера</li>
 *   <li>Отключение механизмов искажения текста</li>
 * </ul>
 * 
 * <p>
 * Вмешательство в Anti-OCR учитывается в общем счетчике нарушений, связанных с вмешательством
 * в работу системы, и может привести к прерыванию экзамена при превышении суммарного лимита.
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AntiOcrTamperingEventStrategy implements AntiCheatEventStrategy {

	private final ExamSessionCacheService examSessionCacheService;
	private final AntiCheatProperties antiCheatProperties;
	private final Striped<Lock> examLocks = Striped.lock(10240);

	/**
	 * {@inheritDoc}
	 * @return тип события {@link EventType#ANTI_OCR_TAMP}
	 */
	@Override
	public EventType getSupportedEvent() {
		return EventType.ANTI_OCR_TAMP;
	}

	/**
	 * Обрабатывает событие вмешательства в работу модуля Anti-OCR.
	 * <p>
	 * Увеличивает счетчик нарушений и проверяет, не превышен ли суммарный лимит
	 * нарушений, связанных с вмешательством в работу системы. Если лимит превышен,
	 * экзамен прерывается.
	 * 
	 *
	 * @param requestDTO объект запроса с данными о событии
	 * @param clientIp IP-адрес клиента
	 * @return ответ с информацией о статусе экзамена
	 */
	@Override
	public ResponseEntity<?> eventProcess(UnifiedRequestDTO requestDTO, String clientIp) {
		UUID examId = requestDTO.getExamId();

		Lock lock = examLocks.get(examId);
		lock.lock();

		try {
			// Проверяем, не прерван ли уже экзамен
			if (examSessionCacheService.isExamTerminatedByViolations(examId)) {
				log.warn("examId={}: Экзамен уже прерван из-за нарушений", examId);
				return ResponseEntity.ok(new SessionIntegrityResponseDTO(true));
			}

			// Получаем прогресс экзамена
			TemporaryExamProgress progress = examSessionCacheService.get(examId).orElse(null);
			if (progress == null) {
				log.warn("examId={}: Прогресс не найден при обработке нарушения Anti-OCR", examId);
				return ResponseEntity.ok(new SessionIntegrityResponseDTO(false));
			}

			// Инкрементируем счетчик нарушений
			progress.setAntiOcrTamperingViolationCount(progress.getAntiOcrTamperingViolationCount() + 1);

			// Проверяем общее количество нарушений, связанных с вмешательством
			if (exceededTotalTamperingViolation(progress, antiCheatProperties)) {
				log.warn("examId={}: Превышено максимальное количество вмешательств в работу системы (включая Anti-OCR).",
						examId);

				// Помечаем экзамен как прерванный
				progress.setTerminatedByViolations(true);
				examSessionCacheService.save(examId, progress);

				return ResponseEntity.ok(new SessionIntegrityResponseDTO(true));
			}

			// Сохраняем обновленный прогресс
			examSessionCacheService.save(examId, progress);

			log.info("examId={}: Зафиксировано вмешательство в работу модуля Anti-OCR ({})",
					examId, progress.getAntiOcrTamperingViolationCount());

			return ResponseEntity.ok(new SessionIntegrityResponseDTO(false));

		} finally {
			lock.unlock();
		}
	}
} 