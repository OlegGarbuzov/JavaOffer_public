package com.example.javaoffer.exam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность ответа на задание.
 * <p>
 * Представляет собой вариант ответа на вопрос задания. Каждый ответ связан с конкретным заданием,
 * содержит текст варианта ответа, признак его правильности и, при необходимости, пояснение.
 * Задание может иметь несколько вариантов ответа, из которых только один правильный.
 * 
 *
 * @author Garbuzov Oleg
 */
@Data
@Entity
@Table(name = "answers")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {
    /**
     * Уникальный идентификатор ответа в базе данных
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "answer_seq_gen")
    @SequenceGenerator(name = "answer_seq_gen", sequenceName = "answer_seq", allocationSize = 1)
    private Long id;

    /**
     * Задание, к которому относится вариант ответа.
     * Обратная сторона связи @OneToMany в классе Task.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * Текст варианта ответа, который отображается пользователю.
     * Может содержать форматированный текст, примеры кода и т.д.
     */
    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Признак правильности варианта ответа.
     * True - если это правильный вариант, false - если неправильный.
     */
    @Column(name = "is_correct")
    private boolean isCorrect;

    /**
     * Пояснение к варианту ответа (почему ответ правильный или неправильный).
     * Показывается пользователю после выбора ответа для объяснения материала.
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    /**
     * Дата и время создания варианта ответа в базе данных.
     * Устанавливается автоматически при первом сохранении.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления варианта ответа.
     * Обновляется автоматически при каждом изменении записи.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
