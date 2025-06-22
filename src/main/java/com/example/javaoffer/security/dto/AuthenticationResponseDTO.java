package com.example.javaoffer.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа на запрос аутентификации.
 * <p>
 * Содержит JWT-токены, возвращаемые клиенту после успешной аутентификации.
 * Включает access токен для доступа к защищенным ресурсам и refresh токен
 * для обновления access токена после истечения его срока действия.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponseDTO {
    
    /**
     * Access токен для доступа к защищенным ресурсам.
     */
    private String accessToken;
    
    /**
     * Refresh токен для обновления access токена.
     */
    private String refreshToken;
} 