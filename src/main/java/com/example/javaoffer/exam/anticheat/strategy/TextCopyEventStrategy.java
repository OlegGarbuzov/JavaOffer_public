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

/**
 * Стратегия обработки событий копирования текста.
 * <p>
 * Отвечает за обработку событий, когда пользователь копирует текст с экрана
 * во время прохождения экзамена. Это считается нарушением правил, так как
 * позволяет пользователю сохранять вопросы для дальнейшего использования или
 * передачи другим лицам.
 * 
 * <p>
 * В отличие от нарушений, связанных с вмешательством в работу системы,
 * копирование текста имеет собственный счетчик и лимит нарушений, при превышении
 * которого экзамен прерывается.
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextCopyEventStrategy implements AntiCheatEventStrategy {

	private final ExamSessionCacheService examSessionCacheService;
	private final AntiCheatProperties antiCheatProperties;
	private final Striped<Lock> examLocks = Striped.lock(10240);

	/**
	 * {@inheritDoc}
	 * @return тип события {@link EventType#TEXT_COPY}
	 */
	@Override
	public EventType getSupportedEvent() {
		return EventType.TEXT_COPY;
	}

	/**
	 * Обрабатывает событие копирования текста.
	 * <p>
	 * Увеличивает счетчик нарушений и проверяет, не превышен ли лимит.
	 * Если лимит превышен, экзамен прерывается.
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
			progress.setTextCopyViolationCount(progress.getTextCopyViolationCount() + 1);

			// Проверяем, не превышено ли максимальное количество нарушений
			if (progress.getTextCopyViolationCount() >= antiCheatProperties.getMaxTextCopyViolations()) {
				log.warn("examId={}: Превышено максимальное количество копирований текста ({})",
						examId, progress.getTextCopyViolationCount());

				// Помечаем экзамен как прерванный
				progress.setTerminatedByViolations(true);
				examSessionCacheService.save(examId, progress);

				return ResponseEntity.ok(new SessionIntegrityResponseDTO(true));
			}

			// Сохраняем обновленный прогресс
			examSessionCacheService.save(examId, progress);

			log.info("examId={}: Зафиксировано копирование текста ({})", examId, progress.getTextCopyViolationCount());

			return ResponseEntity.ok(new SessionIntegrityResponseDTO(false));

		} finally {
			lock.unlock();
		}
	}

}
