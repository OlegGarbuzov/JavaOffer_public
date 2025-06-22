package com.example.javaoffer.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа с JWT-токенами и информацией о сроке их действия.
 * <p>
 * Расширенная версия ответа на аутентификацию, которая включает не только
 * сами токены, но и информацию о времени их истечения. Используется для
 * внутренней передачи данных между сервисами и для установки правильного
 * времени жизни куки.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponseDTO {
    
    /**
     * Access токен для доступа к защищенным ресурсам.
     */
    private String accessToken;
    
    /**
     * Refresh токен для обновления access токена.
     */
    private String refreshToken;
    
    /**
     * Время истечения access токена в миллисекундах с эпохи Unix.
     */
    private long accessTokenExpiration;
    
    /**
     * Время истечения refresh токена в миллисекундах с эпохи Unix.
     */
    private long refreshTokenExpiration;
}