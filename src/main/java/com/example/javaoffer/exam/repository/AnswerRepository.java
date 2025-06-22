package com.example.javaoffer.exam.repository;

import com.example.javaoffer.exam.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью {@link Answer}.
 * Предоставляет методы для доступа к вариантам ответов на задания в базе данных.
 * Наследует базовую функциональность JpaRepository без добавления специфичных методов.
 */
@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

}
