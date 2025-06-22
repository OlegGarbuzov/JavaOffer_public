package com.example.javaoffer.feedback.repository;

import com.example.javaoffer.feedback.entity.FeedBack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностями обратной связи.
 * Предоставляет стандартные CRUD-операции для сущности FeedBack.
 * 
 * @author Garbuzov Oleg
 */
@Repository
public interface FeedBackRepository extends JpaRepository<FeedBack, Long> {
}
