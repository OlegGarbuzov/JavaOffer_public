package com.example.javaoffer.security.service;

import com.example.javaoffer.security.config.JwtConfig;
import com.example.javaoffer.security.dto.TokenResponseDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Сервис для работы с JWT (JSON Web Tokens).
 * <p>
 * Предоставляет методы для:
 * <ul>
 *   <li>Генерации токенов доступа и обновления</li>
 *   <li>Извлечения информации из токенов</li>
 *   <li>Проверки валидности токенов</li>
 * </ul>
 * 
 *
 * @author Garbuzov Oleg
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {
	private final JwtConfig jwtConfig;

	/**
	 * Извлекает имя пользователя (subject) из JWT-токена.
	 *
	 * @param token JWT-токен
	 * @return имя пользователя
	 */
	public String extractUsername(String token) {
		String username = extractClaim(token, Claims::getSubject);
		log.trace("Извлечено имя пользователя из токена: {}", username);
		return username;
	}

	/**
	 * Извлекает определенное утверждение (claim) из JWT-токена.
	 *
	 * @param token          JWT-токен
	 * @param claimsResolver функция для извлечения нужного утверждения из объекта Claims
	 * @param <T>            тип возвращаемого значения
	 * @return значение утверждения, извлеченное из токена
	 */
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	/**
	 * Генерирует пару токенов (access и refresh) для пользователя.
	 *
	 * @param userDetails данные пользователя
	 * @return объект TokenResponseDTO, содержащий оба токена и время их истечения
	 */
	public TokenResponseDTO generateTokens(UserDetails userDetails) {
		log.debug("Генерация токенов для пользователя: {}", userDetails.getUsername());

		String accessToken = generateToken(new HashMap<>(), userDetails, jwtConfig.getAccessTokenExpiration());
		String refreshToken = generateToken(new HashMap<>(), userDetails, jwtConfig.getRefreshTokenExpiration());

		long accessExpiration = System.currentTimeMillis() + jwtConfig.getAccessTokenExpiration();
		long refreshExpiration = System.currentTimeMillis() + jwtConfig.getRefreshTokenExpiration();

		log.debug("Токены сгенерированы успешно для {}, accessToken истекает через {} мс, refreshToken через {} мс",
				userDetails.getUsername(), jwtConfig.getAccessTokenExpiration(), jwtConfig.getRefreshTokenExpiration());

		return TokenResponseDTO.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.accessTokenExpiration(accessExpiration)
				.refreshTokenExpiration(refreshExpiration)
				.build();
	}

	/**
	 * Проверяет, валиден ли токен для указанного пользователя.
	 * Токен считается валидным, если:
	 * <ul>
	 *   <li>Имя пользователя в токене совпадает с именем пользователя в UserDetails</li>
	 *   <li>Срок действия токена не истек</li>
	 * </ul>
	 *
	 * @param token       JWT-токен
	 * @param userDetails данные пользователя
	 * @return true, если токен валиден для указанного пользователя
	 */
	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

		if (!isValid) {
			if (!username.equals(userDetails.getUsername())) {
				log.warn("Несоответствие имени пользователя в токене: {} и в UserDetails: {}",
						username, userDetails.getUsername());
			} else if (isTokenExpired(token)) {
				log.warn("Токен истек для пользователя: {}", username);
			}
		} else {
			log.trace("Токен валиден для пользователя: {}", username);
		}

		return isValid;
	}

	/**
	 * Проверяет, истек ли срок действия токена.
	 *
	 * @param token JWT-токен
	 * @return true, если срок действия токена истек
	 */
	private boolean isTokenExpired(String token) {
		Date expiration = extractExpiration(token);
		boolean isExpired = expiration.before(new Date());

		if (isExpired) {
			log.trace("Токен истек: срок истечения был {}", expiration);
		}

		return isExpired;
	}

	/**
	 * Извлекает дату истечения срока действия из токена.
	 *
	 * @param token JWT-токен
	 * @return дата истечения срока действия токена
	 */
	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	/**
	 * Извлекает все утверждения (claims) из JWT-токена.
	 * <p>
	 * Метод пытается распарсить токен и вернуть его содержимое. В случае,
	 * если токен истек, но структурно валиден, возвращает утверждения из исключения.
	 * В случае других ошибок возвращает null.
	 *
	 * @param token JWT-токен
	 * @return объект Claims, содержащий все утверждения из токена, или null в случае ошибки
	 */
	private Claims extractAllClaims(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(getSigningKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			log.trace("Успешно извлечены claims из токена");
			return claims;
		} catch (ExpiredJwtException e) {
			log.debug("Токен истек, но структурно валиден. Извлекаем claims из исключения");
			return e.getClaims();
		} catch (Exception e) {
			log.error("Ошибка при извлечении claims из токена: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Генерирует JWT-токен для пользователя с указанным временем жизни.
	 *
	 * @param extraClaims дополнительные утверждения для включения в токен
	 * @param userDetails данные пользователя
	 * @param expiration  время жизни токена в миллисекундах
	 * @return строка JWT-токена
	 */
	private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
		Date issuedAt = new Date(System.currentTimeMillis());
		Date expirationDate = new Date(System.currentTimeMillis() + expiration);

		log.trace("Генерация токена для пользователя: {}, действителен с {} до {}",
				userDetails.getUsername(), issuedAt, expirationDate);

		return Jwts.builder()
				.claims(extraClaims)
				.subject(userDetails.getUsername())
				.issuedAt(issuedAt)
				.expiration(expirationDate)
				.signWith(getSigningKey())
				.compact();
	}

	/**
	 * Получает ключ для подписи JWT-токенов.
	 *
	 * @return ключ для подписи, основанный на секрете из конфигурации
	 */
	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtConfig.getSecret().getBytes();
		return Keys.hmacShaKeyFor(keyBytes);
	}
} 