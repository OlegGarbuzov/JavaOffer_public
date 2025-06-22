package com.example.javaoffer.exam.dto;

import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.enums.TaskTopic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO для передачи данных о вопросе (задаче) экзамена.
 * <p>
 * Включает текст вопроса, его тему, сложность, уровень,
 * а также список возможных вариантов ответа. Используется для отображения
 * вопроса пользователю в процессе прохождения экзамена, а также в административной
 * панели для управления вопросами.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class TaskDTO {
    /**
     * Уникальный идентификатор вопроса в базе данных
     */
    private Long id;

    /**
     * Текст вопроса, который будет показан пользователю.
     * Может содержать форматирование, примеры кода и т.д.
     */
    @NotBlank(message = "Вопрос обязателен")
    private String question;

    /**
     * Тема вопроса (например, COLLECTIONS, GENERICS, MULTITHREADING и т.д.).
     * Используется для категоризации вопросов и формирования тематических экзаменов.
     */
    @NotNull(message = "Тема обязательна")
    private TaskTopic topic;

    /**
     * Сложность вопроса (от EASY до EXPERT).
     * Определяет количество баллов за правильный ответ и вероятность появления вопроса
     * на разных уровнях сложности экзамена.
     */
    @NotNull(message = "Сложность обязательна")
    private TaskDifficulty difficulty;

    /**
     * Уровень подготовки, на который рассчитан вопрос (JUNIOR, MIDDLE, SENIOR и т.д.).
     * Используется для формирования экзаменов соответствующего уровня сложности.
     */
    @NotNull(message = "Уровень обязателен")
    private TaskGrade grade;

    /**
     * Список вариантов ответа на вопрос.
     * Обычно содержит один правильный и несколько неправильных вариантов.
     */
    @Builder.Default
    private List<AnswerDTO> answers = new ArrayList<>();

    /**
     * Создает копию текущего объекта TaskDTO с глубоким копированием списка ответов.
     * <p>
     * Этот метод используется, когда необходимо создать копию объекта
     * для дальнейшего изменения без воздействия на оригинал.
     * 
     *
     * @return новый объект TaskDTO с теми же значениями полей
     */
    public TaskDTO copy() {
        return new TaskDTO(
                id,
                question,
                topic,
                difficulty,
                grade,
                answers.stream()
                        .map(answer -> new AnswerDTO(
                                answer.getId(),
                                answer.getContent(),
                                answer.getIsCorrect(),
                                answer.getExplanation()))
                        .collect(Collectors.toList())
        );
    }
}
