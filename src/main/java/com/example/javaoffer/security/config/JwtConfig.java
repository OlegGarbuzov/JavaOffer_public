package com.example.javaoffer.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация JWT-токенов.
 * <p>
 * Класс загружает настройки JWT из файла конфигурации приложения,
 * используя префикс "jwt". Содержит информацию о секретном ключе
 * для подписи токенов и времени жизни как для access, так и для
 * refresh токенов.
 * 
 *
 * @author Garbuzov Oleg
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {
    /**
     * Секретный ключ для подписи JWT токенов.
     * Используется для генерации и валидации JWT.
     */
    private String secret;
    
    /**
     * Время жизни access токена в миллисекундах.
     * Access токен используется для доступа к защищенным ресурсам.
     */
    private long accessTokenExpiration;
    
    /**
     * Время жизни refresh токена в миллисекундах.
     * Refresh токен используется для получения нового access токена
     * после истечения срока действия предыдущего.
     */
    private long refreshTokenExpiration;
} 