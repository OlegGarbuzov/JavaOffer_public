package com.example.javaoffer.exam.service;

import com.example.javaoffer.exam.dto.AnswerDTO;
import com.example.javaoffer.exam.entity.Answer;
import com.example.javaoffer.exam.repository.AnswerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Сервис для работы с ответами на вопросы.
 * <p>
 * Предоставляет методы для получения ответов по идентификатору,
 * проверки правильности ответов и конвертации между DTO и сущностями.
 * Обеспечивает доступ к ответам, хранящимся в базе данных.
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerService {
	private final AnswerRepository answerRepository;

	/**
	 * Получает ответ по его идентификатору и конвертирует в DTO.
	 * <p>
	 * Метод ищет ответ в базе данных по указанному идентификатору
	 * и преобразует его в DTO для передачи клиенту.
	 *
	 * @param answerId идентификатор ответа
	 * @return DTO с данными ответа
	 * @throws EntityNotFoundException если ответ с указанным идентификатором не найден
	 */
	@Transactional(readOnly = true)
	public AnswerDTO getAnswerById(Long answerId) {
		log.debug("Получение ответа по идентификатору: {}", answerId);
		Optional<Answer> answer = answerRepository.findById(answerId);
		if (answer.isPresent()) {
			log.trace("Ответ найден: {}", answerId);
			return convertToDTO(answer.get());
		} else {
			log.error("Ответ не найден по идентификатору: {}", answerId);
			throw new EntityNotFoundException("Ответ не найден по идентификатору: " + answerId);
		}
	}

	/**
	 * Конвертирует сущность в DTO для передачи клиенту.
	 * <p>
	 * Преобразует объект сущности Answer в объект DTO с необходимыми
	 * для клиента данными, скрывая внутренние детали реализации.
	 *
	 * @param answer сущность ответа для конвертации
	 * @return объект DTO с данными ответа
	 */
	public AnswerDTO convertToDTO(Answer answer) {
		log.trace("Конвертация Answer с id: {} в DTO", answer.getId());
		return AnswerDTO.builder()
				.id(answer.getId())
				.content(answer.getContent())
				.isCorrect(answer.isCorrect())
				.explanation(answer.getExplanation())
				.build();
	}

	/**
	 * Конвертирует DTO в сущность для сохранения в БД.
	 * <p>
	 * Преобразует объект DTO с данными от клиента в объект сущности
	 * для последующего сохранения в базе данных.
	 *
	 * @param answerDTO DTO ответа для конвертации
	 * @return объект сущности Answer
	 */
	public Answer convertToEntity(AnswerDTO answerDTO) {
		log.trace("Конвертация answerDTO в сущность Answer с id: {}", answerDTO.getId());
		return Answer.builder()
				.id(answerDTO.getId())
				.content(answerDTO.getContent())
				.isCorrect(answerDTO.getIsCorrect())
				.explanation(answerDTO.getExplanation())
				.build();
	}
}
