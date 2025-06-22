package com.example.javaoffer.feedback.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса обратной связи от пользователя.
 * 
 * @author Garbuzov Oleg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedBackRequestDTO {
    
    /**
     * Текст обратной связи от пользователя.
     * Обязательное поле для заполнения.
     */
    @NotNull
    private String text;
}
