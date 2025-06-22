package com.example.javaoffer.rateLimiter;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.rateLimiter.annotation.RateLimit;
import com.example.javaoffer.rateLimiter.exception.RateLimitException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Аспект для реализации системы ограничения частоты запросов (Rate Limiting).
 * <p>
 * Перехватывает вызовы методов, аннотированных {@link RateLimit}, и применяет
 * пользовательские и глобальные ограничения на частоту вызовов.
 * Использует библиотеку Bucket4j с алгоритмом "Token Bucket" для эффективного
 * контроля частоты запросов.
 *
 * <p>
 * Поддерживает два типа ограничений:
 * <ul>
 *   <li><b>Пользовательские лимиты</b> - ограничения по аутентифицированному пользователю или IP-адресу</li>
 *   <li><b>Глобальные лимиты</b> - ограничения на общее количество вызовов метода всеми пользователями</li>
 * </ul>
 *
 * <p>
 * Исключен из тестового профиля для избежания влияния на производительность тестов.
 *
 * @author Garbuzov Oleg
 */
@Aspect
@Profile("!test")
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

	private final HttpServletRequest request;

	/**
	 * Кэш bucket'ов для пользовательских ограничений.
	 * Ключ: endpoint:username, Значение: Bucket с настроенными лимитами.
	 */
	private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

	/**
	 * Кэш bucket'ов для глобальных ограничений.
	 * Ключ: endpoint, Значение: Bucket с настроенными глобальными лимитами.
	 */
	private final Map<String, Bucket> endpointBuckets = new ConcurrentHashMap<>();

	/**
	 * Основной метод аспекта для ограничения частоты вызовов методов.
	 * <p>
	 * Применяет пользовательские и глобальные лимиты к методам, аннотированным {@link RateLimit}.
	 * Логирует все попытки доступа, успешные и неуспешные прохождения лимитов.
	 *
	 * <p>
	 * Порядок проверок:
	 * <ol>
	 *   <li>Определение пользователя (по имени или IP)</li>
	 *   <li>Проверка пользовательского лимита</li>
	 *   <li>Проверка глобального лимита (если включен)</li>
	 *   <li>Выполнение целевого метода</li>
	 * </ol>
	 *
	 * <p>
	 * <strong>Внимание:</strong> Этот метод автоматически вызывается Spring AOP
	 * для всех методов, аннотированных {@code @RateLimit}. Не вызывайте его напрямую.
	 *
	 * @param joinPoint точка соединения AOP для перехвата метода
	 * @param rateLimit параметры ограничений из аннотации
	 * @return результат выполнения целевого метода
	 * @throws RateLimitException если превышен пользовательский или глобальный лимит
	 * @throws Throwable          исключения из целевого метода
	 */
	@Around("@annotation(rateLimit)")
	@SuppressWarnings("unused")
	public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
		String endpointKey = request.getRequestURI();
		String ip = ClientUtils.getClientIp(request);
		String username = getCurrentUsernameOrIp(ip);
		String userKey = endpointKey + ":" + username;

		log.trace("RateLimitAspect: начало проверки rate limit для endpoint={}, user={}, userLimit={}, globalLimit={}",
				endpointKey, username, rateLimit.userLimit(), rateLimit.globalLimit());

		try {
			checkUserRateLimit(userKey, username, endpointKey, rateLimit);
			checkGlobalRateLimit(endpointKey, rateLimit);

			log.debug("RateLimitAspect: запрос разрешён, выполняем метод. Endpoint={}, user={}", endpointKey, username);
			Object result = joinPoint.proceed();

			log.trace("RateLimitAspect: метод успешно выполнен для endpoint={}, user={}", endpointKey, username);
			return result;

		} catch (RateLimitException e) {
			log.info("RateLimitAspect: запрос отклонён из-за превышения лимита. Endpoint={}, user={}, причина={}",
					endpointKey, username, e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("RateLimitAspect: непредвиденная ошибка при проверке rate limit. Endpoint={}, user={}",
					endpointKey, username, e);
			throw e;
		}
	}

	/**
	 * Проверяет пользовательский лимит запросов.
	 * <p>
	 * Создаёт или получает существующий bucket для пользователя и пытается
	 * получить токен. Если токенов нет, выбрасывает исключение.
	 *
	 * @param userKey     уникальный ключ пользователя для endpoint'а
	 * @param username    имя пользователя для логирования
	 * @param endpointKey путь endpoint'а
	 * @param rateLimit   настройки лимитов
	 * @throws RateLimitException если превышен пользовательский лимит
	 */
	private void checkUserRateLimit(String userKey, String username, String endpointKey, RateLimit rateLimit) {
		log.trace("RateLimitAspect: проверка пользовательского лимита. UserKey={}, лимит={}, период={}сек",
				userKey, rateLimit.userLimit(), rateLimit.userDurationSeconds());

		Bucket userBucket = userBuckets.computeIfAbsent(userKey, _ -> {
			log.debug("RateLimitAspect: создание нового bucket для пользователя. UserKey={}", userKey);
			return Bucket.builder()
					.addLimit(Bandwidth.classic(
							rateLimit.userLimit(),
							Refill.greedy(rateLimit.userLimit(), Duration.ofSeconds(rateLimit.userDurationSeconds()))
					))
					.build();
		});

		if (!userBucket.tryConsume(1)) {
			log.warn("RateLimitAspect: превышен пользовательский лимит. User={}, endpoint={}, лимит={}/{}сек",
					username, endpointKey, rateLimit.userLimit(), rateLimit.userDurationSeconds());
			throw new RateLimitException("Превышен пользовательский лимит запросов. Попробуйте отправить запрос снова чуть позже");
		}

		log.trace("RateLimitAspect: пользовательский лимит пройден. User={}", username);
	}

	/**
	 * Проверяет глобальный лимит запросов, если он включен.
	 * <p>
	 * Создаёт или получает существующий глобальный bucket для endpoint'а.
	 * Проверка выполняется только если globalLimit > 0.
	 *
	 * @param endpointKey путь endpoint'а
	 * @param rateLimit   настройки лимитов
	 * @throws RateLimitException если превышен глобальный лимит
	 */
	private void checkGlobalRateLimit(String endpointKey, RateLimit rateLimit) {
		if (rateLimit.globalLimit() <= 0) {
			log.trace("RateLimitAspect: глобальный лимит отключён для endpoint={}", endpointKey);
			return;
		}

		log.trace("RateLimitAspect: проверка глобального лимита. Endpoint={}, лимит={}, период={}сек",
				endpointKey, rateLimit.globalLimit(), rateLimit.globalDurationSeconds());

		Bucket globalBucket = endpointBuckets.computeIfAbsent(endpointKey, _ -> {
			log.debug("RateLimitAspect: создание нового глобального bucket. Endpoint={}", endpointKey);
			return Bucket.builder()
					.addLimit(Bandwidth.classic(
							rateLimit.globalLimit(),
							Refill.greedy(rateLimit.globalLimit(), Duration.ofSeconds(rateLimit.globalDurationSeconds()))
					))
					.build();
		});

		if (!globalBucket.tryConsume(1)) {
			log.warn("RateLimitAspect: превышен глобальный лимит. Endpoint={}, лимит={}/{}сек",
					endpointKey, rateLimit.globalLimit(), rateLimit.globalDurationSeconds());
			throw new RateLimitException("Превышен глобальный лимит запросов. Попробуйте отправить запрос снова чуть позже");
		}

		log.trace("RateLimitAspect: глобальный лимит пройден. Endpoint={}", endpointKey);
	}

	/**
	 * Определяет текущего пользователя для применения rate limiting.
	 * <p>
	 * Для аутентифицированных пользователей возвращает username,
	 * для неаутентифицированных - IP-адрес.
	 *
	 * @param ip IP-адрес клиента
	 * @return username аутентифицированного пользователя или IP-адрес
	 */
	private String getCurrentUsernameOrIp(String ip) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String identifier = (auth != null && auth.isAuthenticated()) ? auth.getName() : ip;

		log.trace("RateLimitAspect: определён идентификатор пользователя: {}, аутентифицирован: {}",
				identifier, auth != null && auth.isAuthenticated());

		return identifier;
	}
}
