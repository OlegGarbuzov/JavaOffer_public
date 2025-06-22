package com.example.javaoffer.exam.property;

import com.example.javaoffer.exam.enums.ExamMode;

/**
 * Интерфейс для конфигурационных свойств режимов экзамена.
 * <p>
 * Реализуется классами, которые предоставляют параметры для различных режимов экзамена (например, свободный, рейтинговый).
 * Позволяет получать режим, лимиты ошибок/успехов и требование авторизации.
 * 
 */
public interface ModeProperties {
	/**
	 * Возвращает режим экзамена, к которому относятся свойства.
	 *
	 * @return режим экзамена
	 */
	ExamMode getMode();

	/**
	 * Количество неправильных ответов для проигрыша или понижения сложности.
	 *
	 * @return лимит ошибок
	 */
	int getFailAnswersCount();

	int getFailAnswersCountAbsoluteLimit();

	/**
	 * Количество правильных ответов для повышения сложности.
	 *
	 * @return лимит успехов
	 */
	int getSuccessAnswersCount();

	/**
	 * Требуется ли авторизация для прохождения экзамена.
	 *
	 * @return true, если нужна авторизация
	 */
	boolean isNeedAuthorization();
}
