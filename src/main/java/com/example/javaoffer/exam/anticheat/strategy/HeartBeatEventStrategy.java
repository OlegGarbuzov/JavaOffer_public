package com.example.javaoffer.exam.anticheat.strategy;

import com.example.javaoffer.exam.anticheat.dto.SessionStatusResponseDTO;
import com.example.javaoffer.exam.anticheat.dto.UnifiedRequestDTO;
import com.example.javaoffer.exam.anticheat.enums.EventType;
import com.example.javaoffer.exam.anticheat.service.HeartbeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.example.javaoffer.common.constants.UrlConstant.URL_ANTICHEAT_STATUS_FULL;

/**
 * Стратегия обработки heartbeat-событий.
 * <p>
 * Отвечает за обработку периодических запросов от клиента (heartbeat),
 * которые подтверждают активность пользователя и целостность сессии.
 * Эти запросы используются для обнаружения ситуаций, когда пользователь
 * может пытаться обойти систему античита.
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartBeatEventStrategy implements AntiCheatEventStrategy {
	private final HeartbeatService heartbeatService;

	/**
	 * {@inheritDoc}
	 * @return тип события {@link EventType#HEART_BEAT}
	 */
	@Override
	public EventType getSupportedEvent() {
		return EventType.HEART_BEAT;
	}

	/**
	 * Обрабатывает heartbeat-запрос от клиента.
	 * <p>
	 * Извлекает данные из запроса, логирует информацию о времени клиента
	 * и передает запрос сервису для проверки и обработки.
	 * 
	 *
	 * @param requestDTO объект запроса с данными heartbeat
	 * @param clientIp IP-адрес клиента
	 * @return ответ с новым токеном и статусом сессии
	 */
	@Override
	public ResponseEntity<?> eventProcess(UnifiedRequestDTO requestDTO, String clientIp) {
		UUID examId = requestDTO.getExamId();
		Long questionId = requestDTO.getQuestionId();
		String token = requestDTO.getToken();

		// Получаем информацию о времени клиента
		Integer timezoneOffset = requestDTO.getTimezoneOffset();
		String timezoneString = requestDTO.getTimezoneString();
		Long clientTime = requestDTO.getClientTime();

		if (timezoneOffset != null && clientTime != null) {
			long serverClientTimeDiff = Math.abs(System.currentTimeMillis() - clientTime);
			log.debug("{}:POST {}: Часовой пояс клиента: offset={}, timezone={}, разница времени сервер/клиент={}мс",
					clientIp, URL_ANTICHEAT_STATUS_FULL, timezoneOffset, timezoneString, serverClientTimeDiff);
		}

		log.debug("{}:POST {}: Получен heartbeat для examId={}, questionId={}, токен={}",
				clientIp, URL_ANTICHEAT_STATUS_FULL, examId, questionId,
				token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "null");

		// Передаем данные для обработки heartbeat
		SessionStatusResponseDTO response = heartbeatService.processHeartbeat(
				examId,
				token,
				questionId
		);

		log.debug("{}:POST {}: examId={}, результат обработки: nextToken={}",
				clientIp, URL_ANTICHEAT_STATUS_FULL, examId,
				response.getNextToken() != null ? response.getNextToken().substring(0, Math.min(10, response.getNextToken().length())) + "..." : "null");

		return ResponseEntity.ok(response);
	}

}
