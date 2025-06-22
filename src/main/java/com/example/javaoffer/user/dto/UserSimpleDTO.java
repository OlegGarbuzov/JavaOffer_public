package com.example.javaoffer.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Упрощенный DTO для пользователя, содержащий только базовые поля
 * для отображения в истории экзаменов и других местах, где не нужна
 * полная информация о пользователе.
 * 
 * @author Garbuzov Oleg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleDTO {
    /**
     * Идентификатор пользователя
     */
    private Long id;

    /**
     * Имя пользователя (логин)
     */
    private String username;
} 