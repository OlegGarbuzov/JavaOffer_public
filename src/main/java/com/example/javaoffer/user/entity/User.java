package com.example.javaoffer.user.entity;

import com.example.javaoffer.exam.entity.UserScoreHistory;
import com.example.javaoffer.feedback.entity.FeedBack;
import com.example.javaoffer.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * Сущность пользователя в системе, которая реализует интерфейс {@link UserDetails} Spring Security.
 * Хранит информацию об учетной записи, аутентификации и авторизации пользователя.
 * Включает базовые данные пользователя, роль, связь с незавершенными экзаменами
 * и ответами пользователя.
 *
 * @author Garbuzov Oleg
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Builder
@Table(name = "users")
@AllArgsConstructor
public class User implements UserDetails {
	/**
	 * Уникальный идентификатор пользователя
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq_gen")
	@SequenceGenerator(name = "user_seq_gen", sequenceName = "users_seq", allocationSize = 1)
	private Long id;

	/**
	 * Email-адрес пользователя
	 */
	private String email;

	/**
	 * Имя пользователя (логин)
	 */
	private String username;

	/**
	 * Хешированный пароль пользователя
	 */
	private String password;

	/**
	 * Роль пользователя в системе (определяет уровень доступа)
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	/**
	 * Незавершенный экзамен пользователя (если есть)
	 */
	private UUID unfinishedExamId;

	/**
	 * Обратная связь от пользователя
	 */
	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserScoreHistory> userScoreHistories = new ArrayList<>();

	/**
	 * Обратная связь от пользователя
	 */
	@Builder.Default
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<FeedBack> feedBacks = new ArrayList<>();

	/**
	 * Флаг: true - не заблокирован, false - заблокирован
	 */
	@Setter
	@Builder.Default
	@Column(nullable = false)
	private boolean accountNonLocked = true;

	/**
	 * Возвращает права доступа пользователя на основе его роли
	 *
	 * @return коллекция прав доступа пользователя
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.name()));
	}

	/**
	 * Указывает, не истек ли срок действия учетной записи пользователя
	 *
	 * @return true если учетная запись действительна
	 */
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	/**
	 * Указывает, не заблокирована ли учетная запись пользователя
	 *
	 * @return true если учетная запись не заблокирована
	 */
	@Override
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}

	/**
	 * Указывает, не истек ли срок действия учетных данных пользователя
	 *
	 * @return true если учетные данные действительны
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	/**
	 * Указывает, активирована ли учетная запись пользователя
	 *
	 * @return true если учетная запись активирована
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}

	/**
	 * Сравнивает объекты User с учетом особенностей Hibernate Proxy
	 *
	 * @param o объект для сравнения
	 * @return true если объекты равны
	 */
	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
		Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
		if (thisEffectiveClass != oEffectiveClass) return false;
		User user = (User) o;
		return getId() != null && Objects.equals(getId(), user.getId());
	}

	/**
	 * Вычисляет хеш-код объекта с учетом особенностей Hibernate Proxy
	 *
	 * @return хеш-код объекта
	 */
	@Override
	public final int hashCode() {
		return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
	}

}