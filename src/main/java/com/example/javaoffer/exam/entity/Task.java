package com.example.javaoffer.exam.entity;

import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.enums.TaskTopic;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность задания (вопроса) в системе.
 * <p>
 * Представляет собой вопрос, задаваемый пользователю во время экзамена.
 * Содержит текст вопроса, метаданные задания (тема, сложность, уровень подготовки),
 * а также связи с возможными вариантами ответов. Каждое задание может содержать
 * несколько вариантов ответа, из которых только один правильный.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tasks")
public class Task {

    /**
     * Уникальный идентификатор задания в базе данных
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tasks_seq_gen")
    @SequenceGenerator(name = "tasks_seq_gen", sequenceName = "tasks_seq", allocationSize = 1)
    private Long id;

    /**
     * Текст вопроса задания, который отображается пользователю.
     * Может содержать форматированный текст, примеры кода и т.д.
     */
    @NotBlank(message = "Вопрос обязателен")
    @Column(columnDefinition = "TEXT")
    private String question;

    /**
     * Тема, к которой относится задание.
     * Определяет область знаний, проверяемую вопросом (например, COLLECTIONS, GENERICS и т.д.).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private TaskTopic topic;

    /**
     * Уровень сложности задания (от EASY до EXPERT).
     * Влияет на количество баллов за правильный ответ и вероятность появления в экзамене.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private TaskDifficulty difficulty;

    /**
     * Уровень подготовки, для которого предназначено задание (JUNIOR, MIDDLE, SENIOR и т.д.).
     * Определяет целевую аудиторию вопроса.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private TaskGrade grade;

    /**
     * Дата и время создания задания в базе данных.
     * Устанавливается автоматически при первом сохранении.
     */
    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления задания.
     * Обновляется автоматически при каждом изменении записи.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Варианты ответов на задание.
     * Коллекция объектов типа Answer, связанных с данным заданием.
     */
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    /**
     * Метод, вызываемый перед сохранением объекта в базу данных.
     * Устанавливает дату и время создания и обновления.
     * Вызывается автоматически при первом сохранении сущности в базу данных.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Метод, вызываемый перед обновлением объекта в базе данных.
     * Обновляет дату и время последнего изменения.
     * Вызывается автоматически при каждом обновлении сущности в базе данных.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
