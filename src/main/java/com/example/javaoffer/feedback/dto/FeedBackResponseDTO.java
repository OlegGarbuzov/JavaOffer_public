package com.example.javaoffer.feedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа с данными обратной связи.
 * Используется для передачи информации об обратной связи клиенту.
 * 
 * @author Garbuzov Oleg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedBackResponseDTO {
    
    /**
     * Уникальный идентификатор записи обратной связи.
     */
    private Long id;
    
    /**
     * Текст обратной связи от пользователя.
     */
    private String text;
    
    /**
     * IP-адрес пользователя, отправившего обратную связь.
     */
    private String remoteAdd;
    
    /**
     * Имя пользователя, отправившего обратную связь.
     */
    private String user;
} 