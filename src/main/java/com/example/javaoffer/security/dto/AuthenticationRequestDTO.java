package com.example.javaoffer.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса аутентификации пользователя.
 * <p>
 * Содержит учетные данные пользователя, необходимые для входа в систему:
 * имя пользователя и пароль. Используется при обработке запросов на вход
 * через REST API или форму входа.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequestDTO {
    
    /**
     * Имя пользователя для аутентификации.
     */
    private String userName;
    
    /**
     * Пароль пользователя для аутентификации.
     */
    private String password;
} 