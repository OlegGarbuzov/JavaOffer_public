package com.example.javaoffer.exam.repository;

import com.example.javaoffer.exam.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для работы с сущностью {@link UserAnswer}.
 * Предоставляет методы для доступа к ответам пользователей, полученным при прохождении экзамена
 */
@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
}
