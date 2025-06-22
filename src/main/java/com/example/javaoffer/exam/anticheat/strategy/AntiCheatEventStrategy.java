package com.example.javaoffer.exam.anticheat.strategy;

import com.example.javaoffer.exam.anticheat.dto.UnifiedRequestDTO;
import com.example.javaoffer.exam.anticheat.enums.EventType;
import org.springframework.http.ResponseEntity;

/**
 * Интерфейс для стратегий обработки событий античита.
 * <p>
 * Определяет контракт для различных стратегий обработки событий, связанных с системой античита.
 * Каждая стратегия отвечает за обработку определенного типа события, такого как переключение вкладки,
 * копирование текста, открытие инструментов разработчика и т.д.
 * 
 */
public interface AntiCheatEventStrategy {
	
	/**
	 * Возвращает тип события, который поддерживает данная стратегия.
	 *
	 * @return тип события из перечисления {@link EventType}
	 */
	EventType getSupportedEvent();

	/**
	 * Обрабатывает событие античита.
	 *
	 * @param requestDTO объект запроса, содержащий данные о событии
	 * @param clientIp   IP-адрес клиента, отправившего событие
	 * @return ответ сервера с результатом обработки события
	 */
	ResponseEntity<?> eventProcess(UnifiedRequestDTO requestDTO, String clientIp);
}
