package com.example.javaoffer.exam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * DTO для передачи данных об ответе на вопрос экзамена.
 * <p>
 * Содержит текст варианта ответа, признак его правильности и пояснение.
 * Используется как часть TaskDTO для отображения вариантов ответа пользователю
 * и в административной панели для управления вопросами.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class AnswerDTO {
    /**
     * Уникальный идентификатор варианта ответа в базе данных
     */
    private Long id;

    /**
     * Текст варианта ответа, который отображается пользователю.
     * Может содержать форматирование, примеры кода и т.д.
     */
    @NotBlank(message = "Текст ответа обязателен")
    private String content;

    /**
     * Признак правильности варианта ответа.
     * True - если это правильный вариант, false - если неправильный.
     */
    @Builder.Default
    private Boolean isCorrect = false;

    /**
     * Пояснение к варианту ответа, которое отображается после того,
     * как пользователь сделал выбор. Объясняет, почему ответ правильный
     * или неправильный, предоставляет дополнительную информацию.
     */
    private String explanation;
}
