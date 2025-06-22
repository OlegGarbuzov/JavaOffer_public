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
 * Стратегия обработки событий вмешательства в DOM структуру страницы.
 * <p>
 * Отвечает за обработку событий, когда пользователь модифицирует DOM структуру
 * страницы во время прохождения экзамена. Это считается серьезным нарушением правил,
 * так как позволяет пользователю изменять внешний вид и функциональность страницы,
 * потенциально обходя защитные механизмы.
 * 
 * <p>
 * Вмешательство в DOM учитывается в общем счетчике нарушений, связанных с вмешательством
 * в работу системы, и может привести к прерыванию экзамена при превышении суммарного лимита.
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomTamperingEventStrategy implements AntiCheatEventStrategy {

	private final ExamSessionCacheService examSessionCacheService;
	private final AntiCheatProperties antiCheatProperties;
	private final Striped<Lock> examLocks = Striped.lock(10240);

	/**
	 * {@inheritDoc}
	 * @return тип события {@link EventType#DOM_TAMPERING}
	 */
	@Override
	public EventType getSupportedEvent() {
		return EventType.DOM_TAMPERING;
	}

	/**
	 * Обрабатывает событие вмешательства в DOM структуру страницы.
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
				log.warn("examId={}: Прогресс не найден при обработке нарушения", examId);
				return ResponseEntity.ok(new SessionIntegrityResponseDTO(false));
			}

			// Инкрементируем счетчик нарушений
			progress.setDomTamperingViolationCount(progress.getDomTamperingViolationCount() + 1);

			// Проверяем общее количество нарушений, связанных с вмешательством
			if (exceededTotalTamperingViolation(progress, antiCheatProperties)) {
				log.warn("examId={}: Превышено максимальное количество вмешательств в работу системы.",
						examId);

				// Помечаем экзамен как прерванный
				progress.setTerminatedByViolations(true);
				examSessionCacheService.save(examId, progress);

				return ResponseEntity.ok(new SessionIntegrityResponseDTO(true));
			}

			// Сохраняем обновленный прогресс
			examSessionCacheService.save(examId, progress);

			log.info("examId={}: Зафиксировано вмешательство в DOM ({})",
					examId, progress.getDomTamperingViolationCount());

			return ResponseEntity.ok(new SessionIntegrityResponseDTO(false));

		} finally {
			lock.unlock();
		}
	}

}
