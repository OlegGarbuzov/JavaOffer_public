package com.example.javaoffer.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса регистрации нового пользователя.
 * <p>
 * Содержит все необходимые данные для создания нового пользователя в системе:
 * имя пользователя, email, пароль и подтверждение пароля. Включает валидацию
 * для проверки корректности введенных данных.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequestDTO {

    /**
     * Имя пользователя.
     * <p>
     * Должно быть уникальным в системе, содержать от 4 до 20 символов
     * и не быть пустым.
     * 
     */
    @NotBlank(message = "Имя пользователя не может быть пустым")
    @Size(min = 4, max = 20, message = "Имя должно быть от 4 до 20 символов")
    private String username;

    /**
     * Email пользователя.
     * <p>
     * Должен быть уникальным в системе и соответствовать стандартному
     * формату email адреса.
     * 
     */
    @NotBlank
    @Pattern(
            regexp = "^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$",
            message = "Неверный формат email"
    )
    private String userEmail;

    /**
     * Пароль пользователя.
     * <p>
     * Должен содержать минимум 6 символов и не быть пустым.
     * 
     */
    @NotBlank
    @Size(min = 6, message = "Пароль должен быть минимум 6 символов")
    private String password;

    /**
     * Подтверждение пароля.
     * <p>
     * Должно совпадать с основным паролем. Проверка совпадения
     * осуществляется на уровне контроллера.
     * 
     */
    private String passwordConfirm;
} 