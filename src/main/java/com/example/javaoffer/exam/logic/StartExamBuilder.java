package com.example.javaoffer.exam.logic;

import com.example.javaoffer.common.utils.ClientUtils;
import com.example.javaoffer.exam.cache.TemporaryExamProgress;
import com.example.javaoffer.exam.cache.service.ExamSessionCacheService;
import com.example.javaoffer.exam.dto.ExamNextQuestionRequestDTO;
import com.example.javaoffer.exam.enums.ExamDifficulty;
import com.example.javaoffer.exam.enums.ExamMode;
import com.example.javaoffer.user.entity.User;
import com.example.javaoffer.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Компонент для создания запросов на новый экзамен и управления состоянием экзамена.
 * <p>
 * Проверяет наличие у пользователя незавершенных экзаменов и
 * возвращает настройки существующего экзамена, если он есть.
 * В противном случае создает новый экзамен с заданными параметрами.
 * 
 *
 * @author Garbuzov Oleg
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StartExamBuilder {

	private final ExamSessionCacheService examSessionCacheService;
	private final UserService userService;

	/**
	 * Создает запрос на начало экзамена с заданными параметрами.
	 * <p>
	 * Метод проверяет наличие незавершенного экзамена у пользователя
	 * и возвращает либо существующий прогресс, либо создает новый запрос.
	 * 
	 *
	 * @param examMode       режим экзамена (свободный, рейтинговый)
	 * @param examDifficulty уровень сложности экзамена
	 * @return объект запроса на следующий вопрос
	 */
	public ExamNextQuestionRequestDTO buildAndGet(ExamMode examMode, ExamDifficulty examDifficulty) {
		log.info("Запрос на старт экзамена с режимом: {}", examMode);
		return checkPresentProgressAndBuildExamRequestDTO(examMode, examDifficulty);
	}

	/**
	 * Проверяет наличие незавершенного экзамена и формирует соответствующий запрос.
	 * <p>
	 * Метод проверяет, есть ли у пользователя незавершенный экзамен в кэше,
	 * и если есть, то возвращает запрос на его продолжение. В противном случае
	 * создает запрос на новый экзамен.
	 * 
	 *
	 * @param examMode       режим экзамена
	 * @param examDifficulty уровень сложности экзамена
	 * @return объект запроса на следующий вопрос
	 */
	private ExamNextQuestionRequestDTO checkPresentProgressAndBuildExamRequestDTO(ExamMode examMode, ExamDifficulty examDifficulty) {
		log.debug("Проверка наличия незавершённого экзамена для режима: {}", examMode);
		Optional<User> currentUserOptional = ClientUtils.getCurrentUser();
		if (currentUserOptional.isEmpty()) {
			log.trace("Пользователь не найден или анонимен, создаю дефолтный ExamNextQuestionRequestDTO");
			return buildAndGetDefaultExamNextQuestionRequestDTO(examMode, examDifficulty);
		}

		User currentUser = currentUserOptional.get();
		UUID unfinishedExam = currentUser.getUnfinishedExamId();
		if (unfinishedExam == null) {
			log.trace("У пользователя нет незавершённого экзамена, создаем дефолтный ExamNextQuestionRequestDTO");
			return buildAndGetDefaultExamNextQuestionRequestDTO(examMode, examDifficulty, currentUser);
		}
		Optional<TemporaryExamProgress> temporaryExamProgressOptional = examSessionCacheService.get(unfinishedExam);
		if (temporaryExamProgressOptional.isEmpty()) {
			log.trace("Не найден прогресс по незавершённому экзамену {}, создаю дефолтный ExamNextQuestionRequestDTO", unfinishedExam);
			return buildAndGetDefaultExamNextQuestionRequestDTO(examMode, examDifficulty, currentUser);
		}
		TemporaryExamProgress temporaryExamProgress = temporaryExamProgressOptional.get();
		if (!Objects.equals(temporaryExamProgress.getExamMode(), examMode)) {
			log.trace("Режим текущего прогресса {} не совпадает с запрошенным {}, создаю дефолтный ExamNextQuestionRequestDTO", temporaryExamProgress.getExamMode(), examMode);
			return buildAndGetDefaultExamNextQuestionRequestDTO(examMode, examDifficulty, currentUser);
		}

		UUID usedRequestId = temporaryExamProgress.getNextQuestionRequestId() != null ? temporaryExamProgress.getNextQuestionRequestId() : temporaryExamProgress.getLastQuestionRequestId();
		log.debug("Возвращаю ExamNextQuestionRequestDTO на основе существующего прогресса: examId={}, mode={}", unfinishedExam, temporaryExamProgress.getExamMode());
		return buildAndGetExamNextQuestionRequestDTO(
				unfinishedExam,
				examMode,
				usedRequestId);
	}

	/**
	 * Создает дефолтный запрос на экзамен для анонимного пользователя.
	 * <p>
	 * Метод генерирует новый идентификатор экзамена, создает запрос
	 * и инициализирует прогресс в кэше без привязки к пользователю.
	 * 
	 *
	 * @param examMode       режим экзамена
	 * @param examDifficulty уровень сложности экзамена
	 * @return объект запроса на следующий вопрос
	 */
	private ExamNextQuestionRequestDTO buildAndGetDefaultExamNextQuestionRequestDTO(ExamMode examMode, ExamDifficulty examDifficulty) {
		log.debug("Создание дефолтного ExamNextQuestionRequestDTO для режима: {}", examMode);
		UUID newExamId = UUID.randomUUID();
		ExamNextQuestionRequestDTO newExamRequestDTO = buildAndGetExamNextQuestionRequestDTO(
				newExamId,
				examMode,
				UUID.randomUUID()
		);
		log.debug("Создание дефолтного прогресса в кэше: {}", examMode);
		examSessionCacheService.createNewProgress(newExamRequestDTO, examDifficulty, examMode);
		return newExamRequestDTO;
	}

	/**
	 * Создает дефолтный запрос на экзамен для авторизованного пользователя.
	 * <p>
	 * Метод генерирует новый идентификатор экзамена, создает запрос,
	 * инициализирует прогресс в кэше и связывает его с пользователем.
	 * 
	 *
	 * @param examMode       режим экзамена
	 * @param examDifficulty уровень сложности экзамена
	 * @param user           пользователь, для которого создается экзамен
	 * @return объект запроса на следующий вопрос
	 */
	private ExamNextQuestionRequestDTO buildAndGetDefaultExamNextQuestionRequestDTO(ExamMode examMode, ExamDifficulty examDifficulty, User user) {
		log.debug("Создание дефолтного ExamRequestDTO для режима: {}", examMode);

		UUID newExamId = UUID.randomUUID();
		ExamNextQuestionRequestDTO newExamRequestDTO = buildAndGetExamNextQuestionRequestDTO(
				newExamId,
				examMode,
				UUID.randomUUID()
		);

		log.debug("Создание дефолтного прогресса в кэше: {}", examMode);
		examSessionCacheService.createNewProgress(newExamRequestDTO, examDifficulty, examMode);

		log.debug("Присваиваем пользователю:{} идентификатор не завершенного экзамена: {}", user.getUsername(), examMode);
		userService.setUnfinishedExam(user, newExamId);

		return newExamRequestDTO;
	}

	/**
	 * Создает объект запроса на следующий вопрос с заданными параметрами.
	 * <p>
	 * Вспомогательный метод для создания объекта запроса с указанными
	 * идентификаторами экзамена и запроса.
	 * 
	 *
	 * @param examId   идентификатор экзамена
	 * @param examMode режим экзамена
	 * @param requestId идентификатор запроса
	 * @return объект запроса на следующий вопрос
	 */
	private static ExamNextQuestionRequestDTO buildAndGetExamNextQuestionRequestDTO(
			UUID examId,
			ExamMode examMode,
			UUID requestId) {
		log.trace("Сборка ExamRequestDTO: examId={}, examMode={}, requestId={}", examId, examMode, requestId);
		return ExamNextQuestionRequestDTO.builder()
				.examId(examId)
				.requestId(requestId)
				.build();
	}
} 