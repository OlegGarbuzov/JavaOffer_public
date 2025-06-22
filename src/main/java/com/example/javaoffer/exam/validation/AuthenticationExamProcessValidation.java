package com.example.javaoffer.exam.validation;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.exam.exception.AuthenticatedValidationException;
import com.example.javaoffer.exam.exception.NoPropertyForRequestExamMode;
import com.example.javaoffer.exam.property.ModeProperties;
import com.example.javaoffer.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Валидатор аутентификации пользователя при старте экзамена.
 * <p>
 * Проверяет необходимость аутентификации для выбранного режима экзамена
 * на основе настроек свойств режима. Если режим требует аутентификации,
 * но пользователь не вошел в систему, выбрасывает исключение.
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AuthenticationExamProcessValidation implements ExamStartProcessValidation {

	private final Set<ModeProperties> properties;

	/**
	 * Валидирует аутентификацию пользователя для указанного режима экзамена.
	 * <p>
	 * Проверяет, требует ли выбранный режим экзамена аутентификации пользователя.
	 * Если требует, но пользователь не аутентифицирован, выбрасывает исключение.
	 *
	 * @param examMode выбранный режим экзамена
	 * @throws AuthenticatedValidationException если режим требует аутентификации, но пользователь не вошел в систему
	 * @throws NoPropertyForRequestExamMode     если для переданного режима экзамена не найдены настройки
	 */
	@Override
	public void validation(ExamMode examMode) {
		log.debug("Валидация аутентификации пользователя для режима: {}", examMode);

		ModeProperties modeProperties = findModeProperties(examMode);

		if (modeProperties.isNeedAuthorization()) {
			log.debug("Режим {} требует аутентификации", examMode);
			validateUserAuthentication(examMode);
		}

		log.debug("Валидация аутентификации для режима {} успешно завершена", examMode);
	}

	/**
	 * Находит настройки для указанного режима экзамена.
	 *
	 * @param examMode режим экзамена
	 * @return настройки режима
	 * @throws NoPropertyForRequestExamMode если настройки не найдены
	 */
	private ModeProperties findModeProperties(ExamMode examMode) {
		return properties.stream()
				.filter(mode -> mode.getMode().equals(examMode))
				.findFirst()
				.orElseThrow(() -> {
					log.error("Ошибка валидации. Для режима {} не найдены properties", examMode);
					return new NoPropertyForRequestExamMode();
				});
	}

	/**
	 * Проверяет аутентификацию текущего пользователя.
	 *
	 * @param examMode режим экзамена
	 * @throws AuthenticatedValidationException если пользователь не аутентифицирован
	 */
	private void validateUserAuthentication(ExamMode examMode) {
		Optional<User> currentUserOptional = ClientUtils.getCurrentUser();
		if (currentUserOptional.isEmpty()) {
			log.warn("Режим {} требует аутентификации, но пользователь не аутентифицирован. Валидация не пройдена", examMode);
			throw new AuthenticatedValidationException("Войдите в систему для продолжения");
		}
		log.trace("Пользователь аутентифицирован для режима {}", examMode);
	}
}
