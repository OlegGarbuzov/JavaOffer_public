package com.example.javaoffer.common.exception;

import com.example.javaoffer.common.exception.dto.ErrorResponse;
import com.example.javaoffer.exam.anticheat.exception.NoMatchFieldAntiCheatEventTypeException;
import com.example.javaoffer.exam.cache.exception.NoEntryInCacheException;
import com.example.javaoffer.exam.exception.*;
import com.example.javaoffer.rateLimiter.exception.RateLimitException;
import com.example.javaoffer.user.exception.PasswordConfirmException;
import com.example.javaoffer.user.exception.UnauthorizedUserException;
import com.example.javaoffer.user.exception.UserAlreadyExistsException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Optional;

/**
 * Глобальный обработчик исключений для всего приложения.
 * <p>
 * Этот класс централизованно перехватывает все исключения, возникающие в приложении,
 * и обрабатывает их соответствующим образом, возвращая пользователю информативное
 * сообщение об ошибке и соответствующий HTTP-статус.
 * 
 * <p>
 * Для каждого типа исключения определен свой метод-обработчик, который:
 * <ul>
 *   <li>Устанавливает соответствующий HTTP-статус ответа</li>
 *   <li>Логирует информацию об ошибке</li>
 *   <li>Подготавливает модель с информацией об ошибке</li>
 *   <li>Возвращает соответствующий шаблон представления или JSON-ответ</li>
 * </ul>
 * 
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	/**
	 * Обрабатывает исключение, возникающее при попытке аутентификации с несуществующим именем пользователя
	 *
	 * @param exception исключение UsernameNotFoundException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 401
	 */
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<ErrorResponse> userNotFound(UsernameNotFoundException exception, HttpServletRequest request) {
		log.error("Ошибка аутентификации: пользователь не найден - {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	/**
	 * Обрабатывает исключение, возникающее при попытке регистрации пользователя с уже существующим логином или email
	 *
	 * @param exception исключение UserAlreadyExistsException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 409
	 */
	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> userAlreadyExists(UserAlreadyExistsException exception, HttpServletRequest request) {
		log.warn("Попытка регистрации существующего пользователя: {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	/**
	 * Обрабатывает исключение, возникающее при несовпадении пароля и его подтверждения
	 *
	 * @param exception исключение PasswordConfirmException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 400
	 */
	@ExceptionHandler(PasswordConfirmException.class)
	public ResponseEntity<ErrorResponse> passwordRegistrationConfirmFailed(PasswordConfirmException exception, HttpServletRequest request) {
		log.warn("Ошибка подтверждения пароля: {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Обрабатывает исключение, возникающее при отсутствии стратегии для выбранного режима экзамена
	 *
	 * @param exception исключение NoStrategyForSelectModeException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 501
	 */
	@ExceptionHandler(NoStrategyForSelectModeException.class)
	public ResponseEntity<ErrorResponse> noStrategyForSelectModeException(NoStrategyForSelectModeException exception, HttpServletRequest request) {
		log.error("Не найдена стратегия для выбранного режима: {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.NOT_IMPLEMENTED.value(),
				HttpStatus.NOT_IMPLEMENTED.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
	}

	/**
	 * Обрабатывает исключение, возникающее при отсутствии стратегии для типа события
	 *
	 * @param exception исключение NoStrategyForEventTypeException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 501
	 */
	@ExceptionHandler(NoStrategyForEventTypeException.class)
	public ResponseEntity<ErrorResponse> noStrategyForEventTypeException(NoStrategyForEventTypeException exception, HttpServletRequest request) {
		log.error("Не найдена стратегия для типа события: {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.NOT_IMPLEMENTED.value(),
				HttpStatus.NOT_IMPLEMENTED.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
	}

	/**
	 * Обрабатывает исключение, возникающее при отсутствии вопросов для выбранных параметров экзамена
	 *
	 * @param exception исключение NoQuestionsException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 409
	 */
	@ExceptionHandler(NoQuestionsException.class)
	public ResponseEntity<ErrorResponse> noQuestionsException(NoQuestionsException exception, HttpServletRequest request) {
		log.warn("Не найдены вопросы для экзамена: {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	/**
	 * Обрабатывает исключение, возникающее при отсутствии выбранного ответа в запросе
	 *
	 * @param exception исключение NoSelectedAnswerInRequestException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 400
	 */
	@ExceptionHandler(NoSelectedAnswerInRequestException.class)
	public ResponseEntity<ErrorResponse> noSelectedAnswerInRequestException(NoSelectedAnswerInRequestException exception, HttpServletRequest request) {
		log.warn("Отсутствует выбранный ответ в запросе: {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Обрабатывает исключение отсутствия прогресса экзамена.
	 *
	 * @param ex      исключение NoEntryInCacheException
	 * @param request текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 410
	 */
	@ExceptionHandler(NoEntryInCacheException.class)
	public ResponseEntity<ErrorResponse> noEntryInCacheException(NoEntryInCacheException ex, HttpServletRequest request) {
		log.warn("Не найден прогресс экзамена в кэше: {}", ex.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.GONE.value(),
				HttpStatus.GONE.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.GONE).body(error);
	}

	/**
	 * Обрабатывает исключение, возникающее если не аутентифицированный пользователь пытается начать экзамен с режимом, доступный только для залогиненых пользователей.
	 *
	 * @param ex      исключение AuthenticatedValidationException
	 * @param request текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 401
	 */
	@ExceptionHandler(AuthenticatedValidationException.class)
	public ResponseEntity<ErrorResponse> startExamAuthenticatedValidationException(AuthenticatedValidationException ex, HttpServletRequest request) {
		log.warn("Попытка доступа к защищенному режиму без аутентификации: {}", ex.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				ex.getLocalizedMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	/**
	 * Обрабатывает исключение, выбрасываемое, когда не удается найти проперти, соответствующие переданному режиму экзамена.
	 *
	 * @param ex      исключение NoPropertyForRequestExamMode
	 * @param request текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 400
	 */
	@ExceptionHandler(NoPropertyForRequestExamMode.class)
	public ResponseEntity<ErrorResponse> noPropertyForRequestExamMode(NoPropertyForRequestExamMode ex, HttpServletRequest request) {
		log.warn("Не найдены настройки для запрошенного режима экзамена: {}", ex.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Обрабатывает исключение, возникающее при неизвестном типе события античита
	 *
	 * @param ex      исключение noMatchFieldAntiCheatEventTypeException
	 * @param request текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 400
	 */
	@ExceptionHandler(NoMatchFieldAntiCheatEventTypeException.class)
	public ResponseEntity<ErrorResponse> noMatchFieldAntiCheatEventTypeException(NoMatchFieldAntiCheatEventTypeException ex, HttpServletRequest request) {
		log.warn("Неизвестный тип нарушения: {}", ex.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Обрабатывает исключения валидации параметров методов (@Valid + @ModelAttribute/@RequestBody)
	 *
	 * @param exception исключение MethodArgumentNotValidException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 400
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpServletRequest request) {
		String errorMessage = Optional.of(exception.getFieldError())
				.map(FieldError::getDefaultMessage)
				.orElse("Ошибка валидации данных");
		log.warn("Ошибка валидации данных: {}", errorMessage);
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				errorMessage,
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Обрабатывает исключение превышения лимита запросов.
	 *
	 * @param ex      исключение RateLimitException
	 * @param request текущий HTTP-запрос
	 * @param model   модель для передачи данных в представление
	 * @return страница с ошибкой 429 (Too Many Requests) если запрос GET, иначе ResponseEntity с ErrorResponse и статусом 429
	 */
	@ExceptionHandler(RateLimitException.class)
	public Object handleRateLimitException(RateLimitException ex, HttpServletRequest request, Model model) {
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			log.warn("Превышен лимит запросов: {}", ex.getMessage());
			request.setAttribute("errorMessage", ex.getMessage());
			model.addAttribute("request", request);
			return "error/429";
		}
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.TOO_MANY_REQUESTS.value(),
				HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);
		log.warn("Превышен лимит запросов: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
	}

	/**
	 * Обрабатывает исключения валидации ограничений (например, @Size, @NotNull)
	 *
	 * @param ex      исключение ConstraintViolationException
	 * @param request текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 400
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> constraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
		String errorMessage = ex.getConstraintViolations().stream()
				.map(ConstraintViolation::getMessage)
				.findFirst()
				.orElse("Ошибка валидации данных");
		log.warn("Ошибка валидации ограничений: {}", errorMessage);
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Обрабатывает исключение неавторизованного доступа пользователя.
	 *
	 * @param ex      исключение UnauthorizedUserException
	 * @param request текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 403
	 */
	@ExceptionHandler(UnauthorizedUserException.class)
	public ResponseEntity<ErrorResponse> unauthorizedUserException(
			UnauthorizedUserException ex,
			HttpServletRequest request
	) {
		log.warn("Неавторизованный доступ: {}", ex.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.FORBIDDEN.value(),
				HttpStatus.FORBIDDEN.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	/**
	 * Обрабатывает исключение отсутствия правильного ответа для задачи.
	 *
	 * @param exception исключение NoCorrectAnswerByTaskException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 500
	 */
	@ExceptionHandler(NoCorrectAnswerByTaskException.class)
	public Object noCorrectAnswerByTaskException(
			NoCorrectAnswerByTaskException exception,
			HttpServletRequest request) {
		log.error("Отсутствует правильный ответ для задачи: {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	/**
	 * Обрабатывает исключение неверного идентификатора запроса.
	 *
	 * @param exception исключение InvalidRequestIdException
	 * @param request   текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 400
	 */
	@ExceptionHandler(InvalidRequestIdException.class)
	public Object invalidRequestIdException(
			InvalidRequestIdException exception,
			HttpServletRequest request) {
		log.warn("Неверный идентификатор запроса: {}", exception.getMessage());
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Обрабатывает исключение неверного формата данных (например, при десериализации enum).
	 *
	 * @param ex      исключение InvalidFormatException
	 * @param request текущий HTTP-запрос
	 * @return ResponseEntity с ErrorResponse и статусом 400
	 */
	@ExceptionHandler(InvalidFormatException.class)
	public Object handleInvalidEnum(InvalidFormatException ex,
									HttpServletRequest request) {
		String message = String.format(
				"Неверное значение '%s' для типа %s. Допустимые значения: %s",
				ex.getValue(),
				ex.getTargetType().getSimpleName(),
				ex.getTargetType().isEnum() ? java.util.Arrays.toString(ex.getTargetType().getEnumConstants()) : "неизвестно"
		);
		log.warn("Ошибка формата данных: {}", message);
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Обрабатывает все остальные исключения, которые не были перехвачены другими обработчиками.
	 *
	 * @param exception исключение Exception
	 * @param request   текущий HTTP-запрос
	 * @param model     модель для передачи данных в представление
	 * @return страница с ошибкой 500 (Internal Server Error) или ResponseEntity с ErrorResponse и статусом 500
	 */
	@ExceptionHandler(Exception.class)
	public Object global(Exception exception, HttpServletRequest request, Model model) {
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			log.error("Необработанное исключение: {}", exception.getMessage(), exception);
			request.setAttribute("errorMessage", exception.getMessage());
			model.addAttribute("request", request);
			return "error/500";
		}
		ErrorResponse error = ErrorResponse.of(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				exception.getMessage(),
				request.getRequestURI()
		);
		log.error("Необработанное исключение: {}", exception.getMessage(), exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}
