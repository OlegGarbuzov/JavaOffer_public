package com.example.javaoffer.feedback.entity;

import com.example.javaoffer.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/**
 * Сущность обратной связи от пользователей.
 * Содержит информацию о тексте обратной связи, пользователе и его IP-адресе.
 * 
 * @author Garbuzov Oleg
 */
@Entity
@Table(name = "feed_back")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedBack {
	/**
	 * Уникальный идентификатор записи обратной связи.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feed_back_seq_gen")
	@SequenceGenerator(name = "feed_back_seq_gen", sequenceName = "feed_back_seq", allocationSize = 1)
	private Long id;

	/**
	 * Текст обратной связи от пользователя.
	 * Обязательное поле для заполнения.
	 */
	@NotNull
	private String text;

	/**
	 * IP-адрес пользователя, отправившего обратную связь.
	 */
	private String remoteAdd;

	/**
	 * Пользователь, отправивший обратную связь.
	 * Связь many-to-one с сущностью User.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * Сравнивает объекты с учетом особенностей Hibernate Proxy.
	 * Реализует корректное сравнение для JPA-сущностей с ленивой загрузкой.
	 *
	 * @param o объект для сравнения
	 * @return true если объекты равны, false в противном случае
	 */
	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
		Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
		if (thisEffectiveClass != oEffectiveClass) return false;
		FeedBack feedBack = (FeedBack) o;
		return getId() != null && Objects.equals(getId(), feedBack.getId());
	}

	/**
	 * Вычисляет хеш-код объекта с учетом особенностей Hibernate Proxy.
	 * Обеспечивает корректную работу с коллекциями для JPA-сущностей.
	 *
	 * @return хеш-код объекта
	 */
	@Override
	public final int hashCode() {
		return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
	}
}
