package com.example.javaoffer.user.repository;

import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link User}.
 * Предоставляет методы для доступа к пользователям в базе данных
 * и поиска по имени пользователя или адресу электронной почты.
 * 
 * @author Garbuzov Oleg
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Находит пользователя по имени пользователя (логину)
     *
     * @param name имя пользователя для поиска
     * @return контейнер Optional с пользователем или пустой, если пользователь не найден
     */
    Optional<User> findByUsername(String name);
    
    /**
     * Находит пользователя по адресу электронной почты
     *
     * @param email адрес электронной почты для поиска
     * @return контейнер Optional с пользователем или пустой, если пользователь не найден
     */
    Optional<User> findByEmail(String email);

    /**
     * Находит всех пользователей с фильтрацией по email и имени пользователя (без учета регистра)
     *
     * @param email     email для поиска (может быть частичным)
     * @param username  имя пользователя для поиска (может быть частичным)
     * @param pageable  параметры пагинации
     * @return страница пользователей, соответствующих критериям поиска
     */
    Page<User> findAllByEmailContainingIgnoreCaseAndUsernameContainingIgnoreCase(String email, String username, Pageable pageable);

    /**
     * Находит всех пользователей с фильтрацией по email, имени пользователя и роли (без учета регистра)
     *
     * @param email     email для поиска (может быть частичным)
     * @param username  имя пользователя для поиска (может быть частичным)
     * @param role      роль пользователя для фильтрации
     * @param pageable  параметры пагинации
     * @return страница пользователей, соответствующих критериям поиска
     */
    Page<User> findAllByEmailContainingIgnoreCaseAndUsernameContainingIgnoreCaseAndRole(String email, String username, UserRole role, Pageable pageable);

    /**
     * Находит всех пользователей по идентификатору
     *
     * @param id       идентификатор пользователя
     * @param pageable параметры пагинации
     * @return страница пользователей с указанным идентификатором
     */
    Page<User> findAllById(Long id, Pageable pageable);

    /**
     * Находит всех пользователей по статусу блокировки аккаунта
     *
     * @param accountNonLocked статус блокировки (true - не заблокирован, false - заблокирован)
     * @param pageable         параметры пагинации
     * @return страница пользователей с указанным статусом блокировки
     */
    Page<User> findAllByAccountNonLocked(boolean accountNonLocked, Pageable pageable);
} 