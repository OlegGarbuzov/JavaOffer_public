package com.example.javaoffer.exam.property;

import com.example.javaoffer.exam.enums.ExamMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационные свойства для рейтингового режима экзамена.
 * <p>
 * Этот класс загружает и хранит параметры конфигурации для рейтингового режима экзамена
 * из файла настроек приложения с префиксом "exam.rating". Эти параметры определяют
 * поведение рейтингового режима: условия успеха, неудачи и требования авторизации.
 * 
 *
 * @author Garbuzov Oleg
 */
@Component
@Data
@ConfigurationProperties("exam.rating")
public class RatingModeProperties implements ModeProperties {
	/**
	 * Режим экзамена.
	 * <p>
	 * Всегда установлен в {@link ExamMode#RATING} для данной реализации.
	 * 
	 */
	private ExamMode mode;

	/**
	 * Количество неправильных ответов для понижения сложности.
	 * <p>
	 * Определяет, сколько неправильных ответов подряд должен дать пользователь,
	 * чтобы система понизила сложность следующих вопросов.
	 * 
	 */
	private int failAnswersCount;

	/**
	 * Абсолютный лимит неправильных ответов для прерывания экзамена.
	 * <p>
	 * Определяет максимальное количество неправильных ответов, после которого
	 * экзамен будет автоматически прерван с соответствующим статусом.
	 * 
	 */
	private int failAnswersCountAbsoluteLimit;

	/**
	 * Количество правильных ответов для повышения сложности.
	 * <p>
	 * Определяет, сколько правильных ответов подряд должен дать пользователь,
	 * чтобы система повысила сложность следующих вопросов.
	 * 
	 */
	private int successAnswersCount;

	/**
	 * Флаг, указывающий, требуется ли авторизация для прохождения экзамена.
	 * <p>
	 * Для рейтингового режима обычно установлен в true, так как результаты
	 * должны быть привязаны к конкретному пользователю.
	 * 
	 */
	private boolean needAuthorization;
}
