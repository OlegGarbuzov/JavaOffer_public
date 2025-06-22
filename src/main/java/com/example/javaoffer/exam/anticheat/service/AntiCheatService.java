package com.example.javaoffer.exam.anticheat.service;

import com.example.javaoffer.exam.anticheat.dto.UnifiedRequestDTO;
import com.example.javaoffer.exam.anticheat.enums.EventType;
import com.example.javaoffer.exam.anticheat.strategy.AntiCheatEventStrategy;
import com.example.javaoffer.exam.exception.NoStrategyForEventTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для обработки событий античита.
 * <p>
 * Этот сервис является центральным компонентом системы античита, который
 * выбирает подходящую стратегию обработки для каждого типа события.
 * Использует паттерн "Стратегия" для делегирования обработки конкретным реализациям.
 * 
 */
@Service
@Slf4j
public class AntiCheatService {
	private final ConcurrentMap<EventType, AntiCheatEventStrategy> strategyMap;

	/**
	 * Конструктор сервиса.
	 * <p>
	 * Инициализирует карту стратегий, сопоставляя каждый тип события с соответствующей стратегией.
	 * 
	 *
	 * @param strategies список стратегий, которые будут использоваться для обработки событий
	 */
	public AntiCheatService(List<AntiCheatEventStrategy> strategies) {
		this.strategyMap = strategies.stream()
				.collect(Collectors.toConcurrentMap(
						AntiCheatEventStrategy::getSupportedEvent,
						Function.identity()
				));
		log.debug("AntiCheatService инициализирован с {} стратегиями: {}",
				strategies.size(),
				strategies.stream().map(s -> s.getSupportedEvent().name()).collect(Collectors.joining(", ")));
	}

	/**
	 * Обрабатывает событие античита.
	 * <p>
	 * Выбирает подходящую стратегию на основе типа события и делегирует обработку ей.
	 * 
	 *
	 * @param requestDTO объект запроса с данными о событии
	 * @param clientIp IP-адрес клиента
	 * @return ответ с результатом обработки события
	 */
	public ResponseEntity<?> process(UnifiedRequestDTO requestDTO, String clientIp) {
		log.info("Обработка события античита типа {} для экзамена {}", 
		        requestDTO.getEventType(), requestDTO.getExamId());
		AntiCheatEventStrategy strategy = getEventStrategy(requestDTO);
		return strategy.eventProcess(requestDTO, clientIp);
	}

	/**
	 * Получает стратегию для обработки события на основе его типа.
	 *
	 * @param requestDTO объект запроса с данными о событии
	 * @return стратегия для обработки события
	 * @throws NoStrategyForEventTypeException если не найдена подходящая стратегия
	 */
	private AntiCheatEventStrategy getEventStrategy(UnifiedRequestDTO requestDTO) {
		if (requestDTO.getEventType() == null) {
			log.error("Событие не указано в запросе anti cheat event для examId={}", requestDTO.getExamId());
			throw new NoStrategyForEventTypeException("Server error");
		}

		AntiCheatEventStrategy strategy = strategyMap.get(requestDTO.getEventType());
		if (strategy == null) {
			log.error("Не найдена стратегия для anti cheat event: {} (examId={})", requestDTO.getEventType(), requestDTO.getExamId());
			throw new NoStrategyForEventTypeException("Server error");
		}

		log.debug("Выбрана стратегия {} для обработки запроса anti cheat event (examId={})",
				strategy.getClass().getSimpleName(), requestDTO.getExamId());
		return strategy;
	}
}
