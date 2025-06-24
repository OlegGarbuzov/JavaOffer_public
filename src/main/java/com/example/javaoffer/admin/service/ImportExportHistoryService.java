package com.example.javaoffer.admin.service;

import com.example.javaoffer.admin.dto.ImportExportHistoryDTO;
import com.example.javaoffer.admin.entity.ImportExportHistory;
import com.example.javaoffer.admin.repository.ImportExportHistoryRepository;
import com.example.javaoffer.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для работы с историей импорта и экспорта вопросов.
 * <p>
 * Предоставляет методы для:
 * <ul>
 *   <li>Получения списка всех записей истории</li>
 *   <li>Сохранения новых записей</li>
 *   <li>Поиска записей по идентификатору</li>
 *   <li>Удаления всех записей</li>
 *   <li>Фильтрации записей по различным критериям</li>
 * </ul>
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportExportHistoryService {
	private final ImportExportHistoryRepository repository;

	/**
	 * Получает список всех записей истории импорта/экспорта.
	 * <p>
	 * Преобразует сущности в DTO для передачи в слой представления.
	 * Записи сортируются по дате начала операции от новых к старым.
	 *
	 * @return список DTO с информацией об истории импорта/экспорта, отсортированный по дате (сначала новые)
	 */
	@Transactional(readOnly = true)
	public List<ImportExportHistoryDTO> findAll() {
		log.debug("Запрос на получение всех записей истории импорта/экспорта");

		// Сортировка по дате начала операции от новых к старым
		Sort sort = Sort.by(Sort.Direction.DESC, "startedAt");

		List<ImportExportHistoryDTO> result = repository.findAll(sort).stream()
				.map(this::convertToDTO)
				.toList();
		log.trace("Получено {} записей истории импорта/экспорта", result.size());
		return result;
	}

	/**
	 * Сохраняет запись истории импорта/экспорта.
	 *
	 * @param history запись истории для сохранения
	 * @return сохраненная запись с заполненным идентификатором
	 */
	@Transactional
	public ImportExportHistory save(ImportExportHistory history) {
		log.debug("Сохранение записи истории импорта/экспорта: операция={}, файл={}, статус={}",
				history.getOperationType(), history.getFileName(), history.getStatus());
		ImportExportHistory saved = repository.save(history);
		log.debug("Запись истории импорта/экспорта сохранена с id={}", saved.getId());
		return saved;
	}

	/**
	 * Создает новую запись истории импорта.
	 *
	 * @param user     пользователь, выполняющий операцию
	 * @param fileName имя файла
	 * @return созданная запись истории
	 */
	@Transactional
	public ImportExportHistory createImportHistory(User user, String fileName) {
		log.debug("Создание записи истории импорта для пользователя {} и файла {}",
				user.getUsername(), fileName);
		ImportExportHistory history = ImportExportHistory.builder()
				.user(user)
				.operationType(ImportExportHistory.OPERATION_TYPE_IMPORT)
				.fileName(fileName)
				.status(ImportExportHistory.STATUS_IN_PROGRESS)
				.startedAt(LocalDateTime.now())
				.build();
		return save(history);
	}

	/**
	 * Создает новую запись истории экспорта.
	 *
	 * @param user     пользователь, выполняющий операцию
	 * @param fileName имя файла
	 * @return созданная запись истории
	 */
	@Transactional
	public ImportExportHistory createExportHistory(User user, String fileName) {
		log.debug("Создание записи истории экспорта для пользователя {} и файла {}",
				user.getUsername(), fileName);
		ImportExportHistory history = ImportExportHistory.builder()
				.user(user)
				.operationType(ImportExportHistory.OPERATION_TYPE_EXPORT)
				.fileName(fileName)
				.status(ImportExportHistory.STATUS_IN_PROGRESS)
				.startedAt(LocalDateTime.now())
				.build();
		return save(history);
	}

	/**
	 * Обновляет статус записи истории на успешное завершение.
	 *
	 * @param history запись истории
	 * @param result  результат операции
	 */
	@Transactional
	public void markAsSuccess(ImportExportHistory history, String result) {
		log.debug("Обновление статуса записи истории с id={} на SUCCESS", history.getId());
		history.setStatus(ImportExportHistory.STATUS_SUCCESS);
		history.setFinishedAt(LocalDateTime.now());
		history.setResult(result);
		save(history);
	}

	/**
	 * Обновляет статус записи истории на завершение с ошибкой.
	 *
	 * @param history запись истории
	 * @param error   сообщение об ошибке
	 */
	@Transactional
	public void markAsError(ImportExportHistory history, String error) {
		log.debug("Обновление статуса записи истории с id={} на ERROR", history.getId());
		history.setStatus(ImportExportHistory.STATUS_ERROR);
		history.setFinishedAt(LocalDateTime.now());

		// Ограничиваем длину сообщения об ошибке
		if (error != null && error.length() > 500) {
			error = error.substring(0, 500) + "...";
		}

		history.setResult("Ошибка: " + error);
		save(history);
	}

	/**
	 * Удаляет все записи истории импорта/экспорта.
	 */
	@Transactional
	public void deleteAll() {
		log.info("Удаление всех записей истории импорта/экспорта");
		long count = repository.count();
		repository.deleteAll();
		log.info("Удалено {} записей истории импорта/экспорта", count);
	}

	/**
	 * Преобразует сущность ImportExportHistory в DTO.
	 *
	 * @param history сущность для преобразования
	 * @return объект DTO
	 */
	private ImportExportHistoryDTO convertToDTO(ImportExportHistory history) {
		return ImportExportHistoryDTO.builder()
				.id(history.getId())
				.username(history.getUser() != null ? history.getUser().getUsername() : null)
				.operationType(history.getOperationType())
				.fileName(history.getFileName())
				.status(history.getStatus())
				.startedAt(history.getStartedAt())
				.finishedAt(history.getFinishedAt())
				.result(history.getResult())
				.build();
	}
} 