package com.example.javaoffer.exam.service;

import com.example.javaoffer.exam.dto.UserAnswerDTO;
import com.example.javaoffer.exam.entity.UserAnswer;
import com.example.javaoffer.exam.entity.UserScoreHistory;
import com.example.javaoffer.exam.repository.UserAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для работы с ответами пользователей на задания в экзаменах.
 * <p>
 * Обеспечивает операции создания, чтения и сохранения ответов пользователей,
 * а также конвертацию между DTO и сущностями. Используется для отслеживания
 * и анализа ответов пользователей на задания в процессе прохождения экзаменов.
 * 
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAnswerService {
	private final TaskService taskService;
	private final AnswerService answerService;
	private final UserAnswerRepository userAnswerRepository;

	/**
	 * Сохраняет список ответов пользователя в базу данных.
	 * <p>
	 * Метод используется для массового сохранения ответов пользователя
	 * после завершения экзамена или его части.
	 * 
	 *
	 * @param userAnswers список ответов пользователя для сохранения
	 */
	@Transactional
	public void saveAll(List<UserAnswer> userAnswers) {
		log.debug("Сохранение {} ответов пользователя", userAnswers.size());
		userAnswerRepository.saveAll(userAnswers);
		log.trace("Ответы пользователя успешно сохранены");
	}

	/**
	 * Конвертирует сущность UserAnswer в DTO для передачи клиенту.
	 * <p>
	 * Преобразует объект сущности UserAnswer в объект DTO с необходимыми
	 * для клиента данными, включая связанные объекты задания и ответа.
	 * 
	 *
	 * @param userAnswer сущность ответа пользователя для конвертации
	 * @return объект DTO с данными ответа пользователя
	 */
	public UserAnswerDTO convertToDTO(UserAnswer userAnswer) {
		log.trace("Конвертация ответа пользователя с id: {} в DTO", userAnswer.getId());
		return UserAnswerDTO.builder()
				.id(userAnswer.getId())
				.taskDTO(taskService.convertToDTO(userAnswer.getTask()))
				.answerDTO(answerService.convertToDTO(userAnswer.getAnswer()))
				.isCorrect(userAnswer.isCorrect())
				.timeTakenSeconds(userAnswer.getTimeTakenSeconds())
				.build();
	}

	/**
	 * Конвертирует DTO в сущность UserAnswer для сохранения в БД.
	 * <p>
	 * Преобразует объект DTO с данными от клиента в объект сущности
	 * для последующего сохранения в базе данных. Связывает ответ
	 * с историей прохождения экзамена пользователем.
	 * 
	 *
	 * @param userAnswerDTO DTO ответа пользователя для конвертации
	 * @param userScoreHistory история прохождения экзамена, с которой связан ответ
	 * @return объект сущности UserAnswer
	 */
	public UserAnswer convertToEntity(UserAnswerDTO userAnswerDTO, UserScoreHistory userScoreHistory) {
		log.trace("Конвертация ответа пользователя с id: {} в сущность", userAnswerDTO.getId());
		return UserAnswer.builder()
				.id(userAnswerDTO.getId())
				.userScoreHistory(userScoreHistory)
				.task(taskService.convertToEntity(userAnswerDTO.getTaskDTO()))
				.answer(answerService.convertToEntity(userAnswerDTO.getAnswerDTO()))
				.isCorrect(userAnswerDTO.isCorrect())
				.timeTakenSeconds(userAnswerDTO.getTimeTakenSeconds())
				.build();
	}
}
