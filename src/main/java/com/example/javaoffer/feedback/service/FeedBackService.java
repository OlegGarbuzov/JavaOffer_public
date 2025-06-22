package com.example.javaoffer.feedback.service;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.feedback.dto.FeedBackRequestDTO;
import com.example.javaoffer.feedback.dto.FeedBackResponseDTO;
import com.example.javaoffer.feedback.entity.FeedBack;
import com.example.javaoffer.feedback.repository.FeedBackRepository;
import com.example.javaoffer.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для работы с обратной связью пользователей.
 * Обеспечивает создание, получение и удаление записей обратной связи.
 * 
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedBackService {
    
    private final FeedBackRepository feedBackRepository;
    
    /**
     * Добавляет новую запись обратной связи от пользователя.
     * 
     * @param feedBackRequestDTO DTO с данными обратной связи
     * @param httpServletRequest HTTP-запрос для получения IP-адреса
     * @return true если обратная связь успешно сохранена, false в случае ошибки
     */
    @Transactional
    public boolean addFeedBack(FeedBackRequestDTO feedBackRequestDTO, HttpServletRequest httpServletRequest) {
        log.debug("Начало сохранения обратной связи: {}", feedBackRequestDTO.getText());
        
        try {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String remoteAddr = ClientUtils.getClientIp(httpServletRequest);
            
            log.trace("Извлечен пользователь: {} и IP-адрес: {}", user.getUsername(), remoteAddr);

            FeedBack feedBack = FeedBack.builder()
                    .user(user)
                    .remoteAdd(remoteAddr)
                    .text(feedBackRequestDTO.getText())
                    .build();

            log.trace("Создан объект FeedBack для сохранения");
            
            feedBackRepository.save(feedBack);
            
            log.info("Обратная связь успешно сохранена для пользователя: {}", user.getUsername());
            return true;
        } catch (Exception e) {
            log.error("Ошибка при сохранении обратной связи: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Получает все записи обратной связи с пагинацией.
     * 
     * @param pageable параметры пагинации
     * @return страница с DTO обратной связи
     */
    @Transactional(readOnly = true)
    public Page<FeedBackResponseDTO> getAllFeedBacks(Pageable pageable) {
        log.debug("Получение всех записей обратной связи с пагинацией: page={}, size={}", 
                 pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<FeedBackResponseDTO> result = feedBackRepository.findAll(pageable).map(fb -> {
                log.trace("Маппинг FeedBack с ID: {} в DTO", fb.getId());
                return FeedBackResponseDTO.builder()
                        .id(fb.getId())
                        .text(fb.getText())
                        .remoteAdd(fb.getRemoteAdd())
                        .user(fb.getUser().getUsername())
                        .build();
            });
            
            log.info("Получено {} записей обратной связи", result.getTotalElements());
            return result;
        } catch (Exception e) {
            log.error("Ошибка при получении записей обратной связи: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Удаляет запись обратной связи по идентификатору.
     * 
     * @param id идентификатор записи для удаления
     */
    @Transactional
    public void deleteFeedBack(Long id) {
        log.debug("Удаление записи обратной связи с ID: {}", id);
        
        try {
            if (!feedBackRepository.existsById(id)) {
                log.warn("Попытка удаления несуществующей записи обратной связи с ID: {}", id);
                return;
            }
            
            feedBackRepository.deleteById(id);
            log.info("Запись обратной связи с ID: {} успешно удалена", id);
        } catch (Exception e) {
            log.error("Ошибка при удалении записи обратной связи с ID: {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
