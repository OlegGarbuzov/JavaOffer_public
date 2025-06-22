package com.example.javaoffer.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * DTO для передачи информации об истории импорта и экспорта вопросов.
 * <p>
 * Используется для передачи данных между слоями приложения и для отображения
 * в административном интерфейсе истории операций импорта/экспорта.
 * 
 * 
 * @author Garbuzov Oleg
 */
@Getter
@AllArgsConstructor
@Builder
@ToString
public class ImportExportHistoryDTO {
    /**
     * Уникальный идентификатор записи истории
     */
    private final Long id;
    
    /**
     * Имя пользователя, выполнившего операцию
     */
    private final String username;
    
    /**
     * Тип операции (IMPORT или EXPORT)
     */
    private final String operationType;
    
    /**
     * Имя файла, участвующего в операции
     */
    private final String fileName;
    
    /**
     * Статус операции (IN_PROGRESS, COMPLETED, FAILED)
     */
    private final String status;
    
    /**
     * Дата и время начала операции
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime startedAt;
    
    /**
     * Дата и время завершения операции
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime finishedAt;
    
    /**
     * Результат операции (сообщение об успехе или ошибке)
     */
    private final String result;
} 