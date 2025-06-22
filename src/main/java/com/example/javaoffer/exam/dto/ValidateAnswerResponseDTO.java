package com.example.javaoffer.exam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO для ответа на запрос проверки ответа пользователя.
 * <p>
 * Содержит информацию о правильном ответе, включая его содержание и пояснение,
 * а также флаг, указывающий, был ли ответ пользователя правильным.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateAnswerResponseDTO {
    /**
     * Идентификатор правильного ответа.
     * Соответствует полю id в AnswerDTO.
     */
    private Long id;

    /**
     * Текст правильного ответа, который отображается пользователю
     * после завершения проверки.
     */
    @NotBlank(message = "Текст ответа обязателен")
    private String content;

    /**
     * Подробное объяснение ответа или дополнительная информация,
     * которая помогает пользователю лучше понять правильный ответ.
     */
    private String explanation;

    /**
     * Флаг, указывающий, выбрал ли пользователь правильный ответ.
     * True - если ответ пользователя был правильным, false - если ответ был неправильным.
     */
    private Boolean userChooseIsCorrect;

    /**
     * Уникальный идентификатор запроса для сопоставления запросов и ответов.
     * Соответствует идентификатору запроса из ExamCheckAnswerRequestDTO.
     */
    private UUID requestId;
}
