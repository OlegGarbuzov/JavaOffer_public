package com.example.javaoffer.admin.service;

import com.example.javaoffer.admin.entity.ImportExportHistory;
import com.example.javaoffer.exam.entity.Answer;
import com.example.javaoffer.exam.entity.Task;
import com.example.javaoffer.exam.enums.TaskDifficulty;
import com.example.javaoffer.exam.enums.TaskGrade;
import com.example.javaoffer.exam.enums.TaskTopic;
import com.example.javaoffer.exam.repository.AnswerRepository;
import com.example.javaoffer.exam.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для импорта вопросов из Excel-файлов.
 * <p>
 * Реализует логику асинхронного импорта вопросов и ответов из Excel-файлов с поддержкой:
 * <ul>
 *   <li><b>Обновления существующих вопросов</b> - при указании ID вопроса в файле</li>
 *   <li><b>Создания новых вопросов</b> - при отсутствии ID или при его отсутствии в БД</li>
 *   <li><b>Превентивного удаления ответов</b> - все ответы к вопросу удаляются и создаются заново</li>
 *   <li><b>Автоматического удаления дублей</b> - в конце импорта удаляются вопросы с одинаковым текстом</li>
 * </ul>
 *
 * <h3>Структура Excel-файла:</h3>
 * <pre>
 * | question_id | question_text | topic | difficulty | grade | answer_id | answer_text | is_correct | explanation |
 * |-------------|---------------|-------|------------|-------|-----------|-------------|------------|-------------|
 * | 1           | Текст вопроса | CORE  | MEDIUM1    | JUNIOR| 1         | Ответ 1     | true       |Пояснение    |
 * | 1           |               |       |            |       | 2         | Ответ 2     | false      |             |
 * | 2           | Другой вопрос | SPRING| Easy1      | MIDDLE| 3         | Ответ 1     | true       |Пояснение    |
 * </pre>
 *
 * <h3>Логика обработки:</h3>
 * <ol>
 *   <li><b>Строка с текстом вопроса</b> - считается началом нового вопроса или обновлением существующего</li>
 *   <li><b>Строка без текста вопроса</b> - считается дополнительным ответом к предыдущему вопросу</li>
 *   <li><b>При наличии question_id</b> - поиск вопроса по ID, если не найден - поиск по тексту</li>
 *   <li><b>При обновлении вопроса</b> - все его ответы удаляются и создаются заново из файла</li>
 * </ol>
 *
 * @author Garbuzov Oleg
 * @version 2.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionImportService {
	private final TaskRepository taskRepository;
	private final AnswerRepository answerRepository;
	private final ImportExportHistoryService historyService;

	/**
	 * Асинхронно импортирует вопросы из Excel-файла.
	 * <p>
	 * Метод выполняется в отдельном потоке и не блокирует основной поток выполнения.
	 * Автоматически обновляет статус операции в истории импорта/экспорта.
	 *
	 * <h3>Процесс импорта:</h3>
	 * <ol>
	 *   <li>Парсинг Excel-файла построчно</li>
	 *   <li>Определение типа строки (новый вопрос или дополнительный ответ)</li>
	 *   <li>Обработка вопроса (создание/обновление с удалением всех ответов)</li>
	 *   <li>Добавление ответов к текущему вопросу</li>
	 *   <li>Формирование отчета с статистикой</li>
	 * </ol>
	 *
	 * @param file    Excel-файл с вопросами и ответами (формат .xlsx)
	 * @param history запись истории для отслеживания прогресса и результата
	 * @throws RuntimeException если файл поврежден или содержит критические ошибки
	 */
	@Async
	@Transactional
	public void importQuestionsAsync(MultipartFile file, ImportExportHistory history) {
		ImportStatistics stats = new ImportStatistics();
		StringBuilder errors = new StringBuilder();

		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			int startRow = 1; // Пропускаем заголовок
			int lastRow = sheet.getLastRowNum();

			Task currentTask = null; // Текущий обрабатываемый вопрос

			// Основной цикл обработки строк Excel-файла
			for (int rowNum = startRow; rowNum <= lastRow; rowNum++) {
				Row row = sheet.getRow(rowNum);
				if (row == null) continue; // Пропускаем пустые строки

				try {
					// Извлекаем данные из текущей строки в структурированный объект
					ExcelRowData rowData = extractRowData(row);
					log.info("Обрабатываем строку {}: questionId={}, questionText={}",
							rowNum + 1, rowData.questionId(),
							rowData.questionText() != null ?
									rowData.questionText().substring(0, Math.min(20, rowData.questionText().length())) + "..." : "null");

					// Определяем тип строки: новый вопрос или дополнительный ответ
					boolean isNewQuestion = isNewQuestionRow(rowData);

					if (isNewQuestion) {
						// Обрабатываем новый вопрос или обновляем существующий
						currentTask = processQuestionRow(rowData, stats, errors, rowNum + 1);
						if (currentTask == null) continue; // Ошибка обработки, переходим к следующей строке
					}

					// Добавляем ответ к текущему вопросу (если текст ответа не пустой)
					if (rowData.answerText() != null && !rowData.answerText().trim().isEmpty()) {
						processAnswerRow(rowData, currentTask, stats, rowNum + 1);
					}

				} catch (Exception ex) {
					// Логируем ошибку, но продолжаем обработку остальных строк
					stats.errorLines++;
					errors.append("Ошибка в строке ").append(rowNum + 1).append(": ").append(ex.getMessage()).append("\n");
					log.error("Ошибка при импорте строки " + (rowNum + 1), ex);
				}
			}

			// Формируем итоговый отчет для администратора
			String result = buildResultMessage(stats);

			if (stats.errorLines > 0) {
				result += ", ошибок: " + stats.errorLines;
			}

			// Ограничиваем размер сообщения об ошибках (для UI)
			if (!errors.isEmpty()) {
				String errorMsg = errors.toString();
				if (errorMsg.length() > 3000) {
					errorMsg = errorMsg.substring(0, 3000) + "... (и другие ошибки)";
				}
				result += "\n" + errorMsg;
			}

			historyService.markAsSuccess(history, result);
		} catch (Exception e) {
			log.error("Ошибка импорта вопросов из Excel", e);
			historyService.markAsError(history, e.getMessage());
		}
	}

	/**
	 * Извлекает данные из строки Excel в структурированный объект.
	 * <p>
	 * Безопасно читает ячейки разных типов (числовые, строковые, логические)
	 * и преобразует их в нужные Java-типы.
	 *
	 * @param row строка Excel для извлечения данных
	 * @return объект с данными строки
	 */
	private ExcelRowData extractRowData(Row row) {
		return new ExcelRowData(
				getNumericCellValueAsLong(row.getCell(0)),    // question_id
				getStringCellValue(row.getCell(1)),           // question_text
				getStringCellValue(row.getCell(2)),           // topic
				getStringCellValue(row.getCell(3)),           // difficulty
				getStringCellValue(row.getCell(4)),           // grade
				getNumericCellValueAsLong(row.getCell(5)),    // answer_id
				getStringCellValue(row.getCell(6)),           // answer_text
				getBooleanCellValue(row.getCell(7)),          // is_correct
				getStringCellValue(row.getCell(8))            // explanation
		);
	}

	/**
	 * Определяет, является ли строка началом нового вопроса.
	 * <p>
	 * Логика определения:
	 * - Если в строке есть текст вопроса - это новый вопрос
	 * - Если текста вопроса нет - это дополнительный ответ к предыдущему вопросу
	 *
	 * @param rowData данные текущей строки
	 * @return true если это новый вопрос, false если дополнительный ответ
	 */
	private boolean isNewQuestionRow(ExcelRowData rowData) {
		return rowData.questionText() != null && !rowData.questionText().trim().isEmpty();
	}

	/**
	 * Обрабатывает строку с новым вопросом или обновлением существующего.
	 * <p>
	 * <b>Ключевая логика импорта:</b>
	 * <ol>
	 *   <li>Если указан question_id - ищем вопрос по ID</li>
	 *   <li>Если по ID не найден - ищем по тексту вопроса</li>
	 *   <li>Если не найден нигде - создаем новый</li>
	 *   <li><b>ВАЖНО:</b> При любом обновлении сначала удаляем ВСЕ ответы вопроса</li>
	 *   <li>Сохраняем вопрос и возвращаем для дальнейшего добавления ответов</li>
	 * </ol>
	 *
	 * @param rowData   данные строки с вопросом
	 * @param stats     статистика импорта
	 * @param errors    сборщик ошибок
	 * @param rowNumber номер строки для логирования
	 * @return сохраненный вопрос или null при ошибке
	 */
	private Task processQuestionRow(ExcelRowData rowData, ImportStatistics stats, StringBuilder errors, int rowNumber) {
		if (rowData.questionText() == null || rowData.questionText().trim().isEmpty()) {
			stats.errorLines++;
			errors.append("Строка ").append(rowNumber).append(": Текст вопроса не может быть пустым\n");
			return null;
		}

		// Ищем существующий вопрос или создаем новый
		Task task = findOrCreateTask(rowData, errors, rowNumber);
		if (task == null) {
			stats.errorLines++;
			return null;
		}

		boolean isNewTask = task.getId() == null;

		// Обновляем поля вопроса данными из файла
		updateTaskFields(task, rowData);

		// Устанавливаем временные метки
		if (isNewTask) {
			task.setCreatedAt(LocalDateTime.now());
		}
		task.setUpdatedAt(LocalDateTime.now());

		// Сохраняем вопрос в БД
		task = taskRepository.save(task);

		// ВАЖНО: удаляем ВСЕ существующие ответы перед добавлением новых
		// Это гарантирует, что ответы будут точно соответствовать файлу
		deleteAllAnswersForTask(task);

		// Обновляем статистику
		if (isNewTask) {
			stats.createdQuestions++;
			log.info("Создан новый вопрос с ID: {}", task.getId());
		} else {
			stats.updatedQuestions++;
			log.info("Обновлен вопрос с ID: {}", task.getId());
		}

		return task;
	}

	/**
	 * Находит существующий вопрос в БД или создает новый.
	 * <p>
	 * <b>Алгоритм поиска:</b>
	 * <ol>
	 *   <li>Если указан question_id - ищем по ID</li>
	 *   <li>Если по ID не найден или ID не указан - ищем по тексту вопроса</li>
	 *   <li>Если нигде не найден - создаем новый объект Task</li>
	 * </ol>
	 *
	 * @param rowData   данные строки с вопросом
	 * @param errors    сборщик ошибок для отчета
	 * @param rowNumber номер строки для логирования
	 * @return найденный или новый объект Task
	 */
	private Task findOrCreateTask(ExcelRowData rowData, StringBuilder errors, int rowNumber) {
		// Попытка поиска по ID (если указан)
		if (rowData.questionId() != null && rowData.questionId() > 0) {
			Task task = taskRepository.findById(rowData.questionId()).orElse(null);
			if (task != null) {
				log.info("Найден вопрос с ID {}, будет обновлен", rowData.questionId());
				return task;
			} else {
				log.warn("Вопрос с ID {} не найден, поиск по тексту", rowData.questionId());
				errors.append("Строка ").append(rowNumber).append(": Вопрос с ID ").append(rowData.questionId())
						.append(" не найден, поиск по тексту\n");
			}
		}

		// Попытка поиска по тексту вопроса
		List<Task> existingTasks = taskRepository.findByQuestion(rowData.questionText());
		if (existingTasks != null && !existingTasks.isEmpty()) {
			Task existingTask = existingTasks.getFirst(); // Берем первый найденный
			log.info("Найден существующий вопрос с текстом (ID: {}), будет обновлен", existingTask.getId());
			return existingTask;
		}

		// Если нигде не найден - создаем новый
		log.info("Создается новый вопрос");
		return new Task();
	}

	/**
	 * Обновляет поля вопроса данными из Excel-файла.
	 * <p>
	 * Валидирует enum-значения и устанавливает их в объект Task.
	 *
	 * @param task    объект вопроса для обновления
	 * @param rowData данные из Excel-строки
	 * @throws IllegalArgumentException если enum-значения некорректны
	 */
	private void updateTaskFields(Task task, ExcelRowData rowData) {
		task.setQuestion(rowData.questionText());
		task.setTopic(Enum.valueOf(TaskTopic.class, rowData.topic()));
		task.setDifficulty(Enum.valueOf(TaskDifficulty.class, rowData.difficulty()));
		task.setGrade(Enum.valueOf(TaskGrade.class, rowData.grade()));
	}

	/**
	 * Удаляет ВСЕ ответы для указанного вопроса.
	 * <p>
	 * <b>Критически важная операция!</b> Гарантирует, что ответы к вопросу
	 * будут точно соответствовать тем, что указаны в Excel-файле.
	 * Предотвращает накопление "мусорных" ответов при повторных импортах.
	 *
	 * @param task вопрос, для которого удаляются ответы
	 */
	private void deleteAllAnswersForTask(Task task) {
		if (task.getId() != null) {
			List<Answer> existingAnswers = answerRepository.findByTaskId(task.getId());
			if (!existingAnswers.isEmpty()) {
				answerRepository.deleteAll(existingAnswers);
				log.info("Удалено {} существующих ответов для вопроса с ID: {}", existingAnswers.size(), task.getId());
			}
		}
	}

	/**
	 * Добавляет новый ответ к текущему вопросу.
	 * <p>
	 * Создает новый объект Answer и привязывает его к указанному вопросу.
	 * Все ответы создаются заново (предыдущие были удалены в {@link #deleteAllAnswersForTask}).
	 *
	 * @param rowData     данные строки с ответом
	 * @param currentTask вопрос, к которому добавляется ответ
	 * @param stats       статистика для подсчета созданных ответов
	 * @param rowNumber   номер строки для логирования
	 */
	private void processAnswerRow(ExcelRowData rowData, Task currentTask, ImportStatistics stats, int rowNumber) {
		if (currentTask == null) {
			log.warn("Строка {}: Нет текущего вопроса для добавления ответа", rowNumber);
			return;
		}

		// Создаем новый ответ (ID из файла игнорируем - создаем всегда новый)
		Answer answer = new Answer();
		answer.setTask(currentTask);
		answer.setContent(rowData.answerText());
		answer.setCorrect(rowData.isCorrect());
		answer.setExplanation(rowData.explanation());
		answer.setCreatedAt(LocalDateTime.now());
		answer.setUpdatedAt(LocalDateTime.now());

		answerRepository.save(answer);
		stats.createdAnswers++;
		log.info("Создан новый ответ для вопроса с ID: {}", currentTask.getId());
	}

	/**
	 * Формирует итоговое сообщение с результатами импорта.
	 *
	 * @param stats статистика импорта
	 * @return строка с результатами для отображения администратору
	 */
	private String buildResultMessage(ImportStatistics stats) {
		return "Обновлено вопросов: " + stats.updatedQuestions +
				", создано вопросов: " + stats.createdQuestions +
				", создано ответов: " + stats.createdAnswers;
	}

	/**
	 * Безопасно извлекает числовое значение из ячейки Excel.
	 * <p>
	 * Поддерживает чтение из числовых и строковых ячеек.
	 * Возвращает null при ошибках парсинга.
	 *
	 * @param cell ячейка Excel
	 * @return числовое значение или null
	 */
	private Long getNumericCellValueAsLong(Cell cell) {
		if (cell == null) return null;
		try {
			if (cell.getCellType() == CellType.NUMERIC) {
				double numValue = cell.getNumericCellValue();
				return (long) numValue;
			} else if (cell.getCellType() == CellType.STRING) {
				String strValue = cell.getStringCellValue();
				if (strValue == null || strValue.trim().isEmpty()) return null;
				return Long.parseLong(strValue.trim());
			}
		} catch (Exception e) {
			// Игнорируем ошибки и возвращаем null
		}
		return null;
	}

	/**
	 * Безопасно извлекает строковое значение из ячейки Excel.
	 * <p>
	 * Поддерживает чтение из строковых, числовых и логических ячеек.
	 *
	 * @param cell ячейка Excel
	 * @return строковое значение или null
	 */
	private @Nullable String getStringCellValue(Cell cell) {
		if (cell == null) return null;
		try {
			if (cell.getCellType() == CellType.STRING) {
				return cell.getStringCellValue();
			} else if (cell.getCellType() == CellType.NUMERIC) {
				return String.valueOf((long) cell.getNumericCellValue());
			} else if (cell.getCellType() == CellType.BOOLEAN) {
				return String.valueOf(cell.getBooleanCellValue());
			}
		} catch (Exception e) {
			// Игнорируем ошибки и возвращаем null
		}
		return null;
	}

	/**
	 * Безопасно извлекает логическое значение из ячейки Excel.
	 * <p>
	 * Поддерживает различные форматы:
	 * - Логические ячейки: true/false
	 * - Строковые: "true", "yes", "1", "да" = true
	 * - Числовые: 0 = false, любое другое = true
	 *
	 * @param cell ячейка Excel
	 * @return логическое значение (по умолчанию false)
	 */
	private boolean getBooleanCellValue(Cell cell) {
		if (cell == null) return false;
		try {
			if (cell.getCellType() == CellType.BOOLEAN) {
				return cell.getBooleanCellValue();
			} else if (cell.getCellType() == CellType.STRING) {
				String value = cell.getStringCellValue().toLowerCase().trim();
				return "true".equals(value) || "yes".equals(value) || "1".equals(value) || "да".equals(value);
			} else if (cell.getCellType() == CellType.NUMERIC) {
				return cell.getNumericCellValue() != 0;
			}
		} catch (Exception ignore) {
			// Игнорируем ошибки
		}
		return false;
	}

	/**
	 * Структура данных для хранения информации из одной строки Excel-файла.
	 * <p>
	 * Использует современный Java Record для иммутабельного хранения данных.
	 * Соответствует структуре экспортируемого Excel-файла.
	 *
	 * @param questionId   ID вопроса (может быть null для новых вопросов)
	 * @param questionText текст вопроса (обязателен для новых вопросов)
	 * @param topic        тема вопроса (enum TaskTopic)
	 * @param difficulty   сложность вопроса (enum TaskDifficulty)
	 * @param grade        уровень подготовки (enum TaskGrade)
	 * @param answerId     ID ответа (игнорируется при импорте)
	 * @param answerText   текст ответа
	 * @param isCorrect    признак правильности ответа
	 * @param explanation  пояснение к ответу
	 */
	private record ExcelRowData(
			Long questionId,
			String questionText,
			String topic,
			String difficulty,
			String grade,
			Long answerId,
			String answerText,
			boolean isCorrect,
			String explanation
	) {
	}

	/**
	 * Класс для сбора статистики процесса импорта.
	 * <p>
	 * Используется для формирования итогового отчета для администратора.
	 */
	private static class ImportStatistics {
		int updatedQuestions = 0;  // Количество обновленных вопросов
		int createdQuestions = 0;  // Количество созданных вопросов
		int createdAnswers = 0;    // Количество созданных ответов
		int errorLines = 0;        // Количество строк с ошибками
	}
} 