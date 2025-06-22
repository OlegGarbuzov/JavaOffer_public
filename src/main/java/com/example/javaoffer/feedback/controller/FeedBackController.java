package com.example.javaoffer.feedback.controller;

import com.example.javaoffer.feedback.dto.FeedBackRequestDTO;
import com.example.javaoffer.feedback.service.FeedBackService;
import com.example.javaoffer.rateLimiter.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static com.example.javaoffer.common.constants.UrlConstant.URL_FEEDBACK;

/**
 * Контроллер для обработки обратной связи от пользователей.
 * Предоставляет REST API для отправки обратной связи с ограничением частоты запросов.
 * 
 * @author Garbuzov Oleg
 * @since 1.0
 */
@Controller
@Slf4j
public class FeedBackController {
    
    private final FeedBackService feedBackService;

    /**
     * Конструктор контроллера обратной связи.
     * 
     * @param feedBackService сервис для работы с обратной связью
     */
    public FeedBackController(FeedBackService feedBackService) {
        this.feedBackService = feedBackService;
    }

    /**
     * Обрабатывает запрос на отправку обратной связи от аутентифицированного пользователя.
     * Применяет ограничение частоты запросов для предотвращения спама.
     *
     * @param feedBackRequestDTO DTO с данными обратной связи, должен пройти валидацию
     * @param httpServletRequest HTTP-запрос для извлечения метаданных пользователя
     * @return ResponseEntity с результатом обработки: 200 OK при успехе, 500 при ошибке
     */
    @PostMapping(URL_FEEDBACK)
    @RateLimit
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> suggestCorrection(@RequestBody @Valid FeedBackRequestDTO feedBackRequestDTO, 
                                                 HttpServletRequest httpServletRequest) {
        String clientIp = httpServletRequest.getRemoteAddr();
        
        log.info("{}:POST {}: Получен запрос обратной связи от пользователя", clientIp, URL_FEEDBACK);
        log.debug("{}:POST {}: Данные запроса: {}", clientIp, URL_FEEDBACK, feedBackRequestDTO);
        
        try {
            boolean result = feedBackService.addFeedBack(feedBackRequestDTO, httpServletRequest);
            
            if (result) {
                log.info("{}:POST {}: Обратная связь успешно сохранена", clientIp, URL_FEEDBACK);
                return ResponseEntity.ok().build();
            } else {
                log.error("{}:POST {}: Ошибка при сохранении обратной связи", clientIp, URL_FEEDBACK);
                return ResponseEntity.internalServerError().build();
            }
        } catch (Exception e) {
            log.error("{}:POST {}: Неожиданная ошибка при обработке обратной связи: {}", 
                     clientIp, URL_FEEDBACK, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
