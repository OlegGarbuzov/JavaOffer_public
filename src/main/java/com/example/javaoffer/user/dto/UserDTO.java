package com.example.javaoffer.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для передачи данных пользователя.
 * <p>
 * Содержит полную информацию о пользователе, включая системные поля
 * и статистические данные. Используется для отображения пользователей
 * в административной панели и других местах, где требуется детальная
 * информация о пользователе.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    /**
     * Уникальный идентификатор пользователя
     */
    private Long id;
    
    /**
     * Email-адрес пользователя
     */
    private String email;
    
    /**
     * Имя пользователя (логин)
     */
    private String username;
    
    /**
     * Пароль пользователя (не используется при передаче данных)
     */
    private String password;
    
    /**
     * Роль пользователя в системе
     */
    private String role;
    
    /**
     * Флаг блокировки аккаунта: true - не заблокирован, false - заблокирован
     */
    private boolean accountNonLocked;
    
    /**
     * Количество записей в истории результатов пользователя
     */
    private int userScoreHistoriesCount;
} 