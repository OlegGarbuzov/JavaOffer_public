package com.example.javaoffer.exam.anticheat.service;

import com.example.javaoffer.exam.anticheat.config.AntiCheatProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * Сервис для генерации и проверки токенов сессии экзамена.
 * <p>
 * Этот сервис обеспечивает безопасную аутентификацию heartbeat-запросов от клиента
 * путем генерации и проверки криптографически защищенных токенов. Токены используются
 * для подтверждения подлинности запросов и предотвращения подделки heartbeat-сигналов.
 * 
 * <p>
 * Токены генерируются на основе идентификатора экзамена, идентификатора текущего вопроса,
 * текущего времени и секретного ключа, что обеспечивает их уникальность и защищенность.
 * Для генерации токена используется алгоритм SHA-256 с последующим кодированием в Base64.
 * 
 * <p>
 * Проверка токена осуществляется путем сравнения полученного токена с сохраненным
 * на сервере, что позволяет убедиться, что запрос отправлен легитимным клиентом.
 * 
 */
@Service
@RequiredArgsConstructor
public class HeartBeatTokenService {

	private final AntiCheatProperties antiCheatProperties;

	/**
	 * Генерирует токен для проверки подлинности heartbeat-запросов.
	 * <p>
	 * Токен создается путем объединения идентификатора экзамена, идентификатора вопроса,
	 * текущего времени в миллисекундах и секретного ключа. Полученная строка хешируется
	 * с использованием алгоритма SHA-256, а результат кодируется в Base64 для удобства передачи.
	 * 
	 * <p>
	 * Такой подход обеспечивает:
	 * <ul>
	 *   <li>Уникальность токена для каждого запроса</li>
	 *   <li>Невозможность предсказать следующий токен</li>
	 *   <li>Защиту от подделки heartbeat-запросов</li>
	 *   <li>Дополнительную защиту с использованием секретного ключа</li>
	 * </ul>
	 * 
	 *
	 * @param examId     идентификатор экзамена
	 * @param questionId ID текущего вопроса
	 * @return зашифрованный токен в формате Base64
	 * @throws RuntimeException если произошла ошибка при генерации хеша
	 */
	public String generateToken(UUID examId, Long questionId) {
		try {
			String tokenData = examId.toString() + ":" +
					questionId + ":" +
					System.currentTimeMillis() + ":" +
					antiCheatProperties.getTokenSecret();

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(tokenData.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Ошибка при генерации токена: алгоритм SHA-256 не найден", e);
		}
	}

	/**
	 * Проверяет валидность токена путем сравнения с сохраненным токеном.
	 * <p>
	 * Метод выполняет простое сравнение полученного токена с сохраненным ранее.
	 * Если токены совпадают, запрос считается подлинным. Если токены не совпадают
	 * или один из них равен null, запрос считается недействительным.
	 * 
	 * <p>
	 * В отличие от более сложных систем аутентификации, этот метод не выполняет
	 * проверку срока действия токена, так как эта функциональность реализована
	 * в HeartbeatService, который отслеживает время между запросами.
	 * 
	 *
	 * @param token       токен для проверки, полученный от клиента
	 * @param storedToken сохраненный токен, с которым выполняется сравнение
	 * @return true, если токен валиден (совпадает с сохраненным), false в противном случае
	 */
	public boolean validateToken(String token, String storedToken) {
		if (token == null || storedToken == null) {
			return false;
		}
		return token.equals(storedToken);
	}
} 