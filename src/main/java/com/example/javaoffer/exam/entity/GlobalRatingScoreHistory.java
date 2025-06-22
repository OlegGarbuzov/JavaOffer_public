package com.example.javaoffer.exam.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/**
 * Сущность глобальной истории результатов для рейтинга.
 * <p>
 * Хранит ссылки на лучшие результаты пользователей, пройденных в соревновательном режиме.
 * Используется для формирования и отображения глобального рейтинга лучших результатов
 * всех пользователей системы. Каждая запись представляет один результат экзамена,
 * который попал в глобальный рейтинг.
 * 
 *
 * @author Garbuzov Oleg
 */
@Getter
@Setter
@Entity
@Table(name = "global_rating_score_history")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalRatingScoreHistory {
    /**
     * Уникальный идентификатор записи в глобальном рейтинге
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_rating_score_history_seq_gen")
    @SequenceGenerator(name = "global_rating_score_history_seq_gen", sequenceName = "global_rating_score_history_seq", allocationSize = 1)
    private Long id;

    /**
     * Ссылка на результат экзамена пользователя, который входит в глобальный рейтинг.
     * При удалении результата из UserScoreHistory, запись также удаляется из глобального рейтинга.
     */
    @OneToOne
    @JoinColumn(name = "user_score_history_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserScoreHistory bestUserScoreHistory;

    /**
     * Сравнивает объекты с учетом особенностей Hibernate Proxy.
     * <p>
     * Метод переопределен для корректной работы с Hibernate-прокси объектами.
     * Проверяет только на равенство идентификаторов, если они установлены.
     * 
     *
     * @param o объект для сравнения
     * @return true если объекты представляют одну и ту же сущность
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        GlobalRatingScoreHistory userScoreHistory = (GlobalRatingScoreHistory) o;
        return getId() != null && Objects.equals(getId(), userScoreHistory.getId());
    }

    /**
     * Вычисляет хеш-код объекта с учетом особенностей Hibernate Proxy.
     * <p>
     * Метод переопределен для корректной работы с Hibernate-прокси объектами.
     * Использует класс объекта для вычисления хеш-кода.
     * 
     *
     * @return хеш-код объекта
     */
    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
