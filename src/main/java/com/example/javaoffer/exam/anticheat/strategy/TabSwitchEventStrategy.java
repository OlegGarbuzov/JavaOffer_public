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
 * Стратегия обработки событий переключения вкладки.
 * <p>
 * Отвечает за обработку событий, когда пользователь переключается на другую вкладку
 * браузера во время прохождения экзамена. Это считается нарушением правил и
 * может привести к прерыванию экзамена при превышении допустимого количества нарушений.
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TabSwitchEventStrategy implements AntiCheatEventStrategy {

	private final ExamSessionCacheService examSessionCacheService;
	private final AntiCheatProperties antiCheatProperties;
	private final Striped<Lock> examLocks = Striped.lock(10240);

	/**
	 * {@inheritDoc}
	 * @return тип события {@link EventType#TAB_SWITCH}
	 */
	@Override
	public EventType getSupportedEvent() {
		return EventType.TAB_SWITCH;
	}

	/**
	 * Обрабатывает событие переключения вкладки.
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

			progress.setTabSwitchViolationCount(progress.getTabSwitchViolationCount() + 1);

			// Проверяем, не превышено ли максимальное количество нарушений
			if (progress.getTabSwitchViolationCount() >= antiCheatProperties.getMaxTabSwitchViolations()) {
				log.warn("examId={}: Превышено максимальное количество переключений вкладки ({})",
						examId, progress.getTabSwitchViolationCount());

				// Помечаем экзамен как прерванный
				progress.setTerminatedByViolations(true);
				examSessionCacheService.save(examId, progress);

				return ResponseEntity.ok(new SessionIntegrityResponseDTO(true));
			}

			examSessionCacheService.save(examId, progress);

			log.info("examId={}: Зафиксировано переключение вкладки ({})", examId, progress.getTabSwitchViolationCount());

			return ResponseEntity.ok(new SessionIntegrityResponseDTO(false));

		} finally {
			lock.unlock();
		}
	}

}
