package com.example.javaoffer.admin.service;

import com.example.javaoffer.admin.entity.ImportExportHistory;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для импорта и экспорта вопросов в формате Excel.
 * <p>
 * Предоставляет функционал для:
 * <ul>
 *   <li>Асинхронного импорта вопросов из Excel-файла</li>
 *   <li>Экспорта вопросов в Excel-файл</li>
 * </ul>
 *
 * <p>
 * Сервис отслеживает историю операций импорта/экспорта и сохраняет
 * результаты выполнения в базе данных.
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionImportExportService {
	private final TaskRepository taskRepository;
	private final AnswerRepository answerRepository;
	private final ImportExportHistoryService historyService;

	/**
	 * Асинхронно импортирует вопросы из Excel-файла.
	 * <p>
	 * Метод анализирует Excel-файл, извлекает данные о вопросах и ответах,
	 * и сохраняет их в базе данных. Поддерживается как создание новых вопросов
	 * и ответов, так и обновление существующих.
	 *
	 * <p>
	 * Метод выполняется асинхронно и обновляет запись истории импорта
	 * по завершении операции.
	 *
	 * @param file    Excel-файл с вопросами и ответами
	 * @param history запись истории импорта для отслеживания результата
	 */
	@Async
	@Transactional
	public void importQuestionsAsync(MultipartFile file, ImportExportHistory history) {
		int updatedQuestions = 0;
		int createdQuestions = 0;
		int updatedAnswers = 0;
		int createdAnswers = 0;
		int errorLines = 0;
		StringBuilder errors = new StringBuilder();

		// Для отслеживания уже созданных и обновлённых вопросов
		java.util.Set<Long> updatedQuestionIds = new java.util.HashSet<>();
		java.util.Set<String> createdQuestionTexts = new java.util.HashSet<>();
		java.util.Set<String> processedQuestions = new java.util.HashSet<>();

		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);

			// Пропускаем заголовок (первую строку)
			int startRow = 1;
			int lastRow = sheet.getLastRowNum();

			Task currentTask = null;

			for (int rowNum = startRow; rowNum <= lastRow; rowNum++) {
				Row row = sheet.getRow(rowNum);
				if (row == null) continue;

				try {
					// Извлекаем данные из ячеек
					Long questionId = getNumericCellValueAsLong(row.getCell(0));
					String questionText = getStringCellValue(row.getCell(1));
					String topic = getStringCellValue(row.getCell(2));
					String difficulty = getStringCellValue(row.getCell(3));
					String grade = getStringCellValue(row.getCell(4));
					Long answerId = getNumericCellValueAsLong(row.getCell(5));
					String answerText = getStringCellValue(row.getCell(6));
					boolean isCorrect = getBooleanCellValue(row.getCell(7));
					String explanation = getStringCellValue(row.getCell(8));

					log.info("Обрабатываем строку {}: questionId={}, questionText={}",
							rowNum + 1, questionId,
							questionText != null ? questionText.substring(0, Math.min(20, questionText.length())) + "..." : "null");

					// Проверяем, является ли эта строка дополнительным ответом к предыдущему вопросу
					boolean isAdditionalAnswer = (questionText == null || questionText.trim().isEmpty()) && currentTask != null;

					if (!isAdditionalAnswer) {
						// Это новый вопрос
						if (questionText == null || questionText.trim().isEmpty()) {
							errorLines++;
							errors.append("Строка ").append(rowNum + 1).append(": Текст вопроса не может быть пустым для нового вопроса\n");
							continue;
						}

						// Проверяем, не обрабатывали ли мы уже этот вопрос
						if (processedQuestions.contains(questionText)) {
							log.info("Вопрос с текстом '{}' уже был обработан, используем его как дополнительный ответ",
									questionText.substring(0, Math.min(20, questionText.length())) + "...");

							// Ищем вопрос по тексту
							List<Task> existingTasks = taskRepository.findByQuestion(questionText);
							if (existingTasks != null && !existingTasks.isEmpty()) {
								currentTask = existingTasks.getFirst();
								log.info("Найден существующий вопрос с ID: {}", currentTask.getId());
							}
							// Если не нашли, то продолжим использовать текущий currentTask
						} else {
							// Отмечаем вопрос как обработанный
							processedQuestions.add(questionText);

							// Проверяем, существует ли уже вопрос с таким текстом в базе
							List<Task> existingTasksByText = taskRepository.findByQuestion(questionText);
							boolean existsInDb = existingTasksByText != null && !existingTasksByText.isEmpty();

							Task task;
							boolean isNewTask;

							// Проверяем существование вопроса по ID
							if (questionId != null && questionId > 0) {
								task = taskRepository.findById(questionId).orElse(null);
								if (task == null) {
									if (existsInDb) {
										task = existingTasksByText.getFirst();
										isNewTask = false;
										log.warn("Вопрос с ID {} не найден, но найден вопрос с таким текстом (ID: {}), будет использован он",
												questionId, task.getId());
										errors.append("Строка ").append(rowNum + 1).append(": Вопрос с ID ").append(questionId)
												.append(" не найден, но найден вопрос с таким текстом (ID: ").append(task.getId())
												.append("), будет использован он\n");
									} else {
										task = new Task();
										isNewTask = true;
										log.warn("Вопрос с ID {} не найден, будет создан новый вопрос", questionId);
										errors.append("Строка ").append(rowNum + 1).append(": Вопрос с ID ").append(questionId)
												.append(" не найден, будет создан новый вопрос\n");
									}
								} else {
									isNewTask = false;
									log.info("Найден вопрос с ID {}, будет обновлен", questionId);
									// Явно увеличиваем счетчик обновленных вопросов только один раз за импорт
									if (!updatedQuestionIds.contains(task.getId())) {
										updatedQuestions++;
										updatedQuestionIds.add(task.getId());
										log.info("Обновлен вопрос с ID: {}, текущий счетчик: {}", task.getId(), updatedQuestions);
									}
								}
							} else {
								if (existsInDb) {
									task = existingTasksByText.getFirst();
									isNewTask = false;
									log.info("Найден существующий вопрос с таким текстом (ID: {}), будет обновлен", task.getId());
									if (task.getId() != null && !updatedQuestionIds.contains(task.getId())) {
										updatedQuestions++;
										updatedQuestionIds.add(task.getId());
										log.info("Обновлен вопрос с ID: {}, текущий счетчик: {}", task.getId(), updatedQuestions);
									}
								} else {
									task = new Task();
									isNewTask = true;
								}
							}

							// Обновляем данные вопроса
							task.setQuestion(questionText);
							try {
								task.setTopic(Enum.valueOf(TaskTopic.class, topic));
								task.setDifficulty(Enum.valueOf(TaskDifficulty.class, difficulty));
								task.setGrade(Enum.valueOf(TaskGrade.class, grade));
							} catch (IllegalArgumentException e) {
								errorLines++;
								errors.append("Строка ").append(rowNum + 1).append(": Неверное значение для Topic/Difficulty/Grade: ").append(e.getMessage()).append("\n");
								continue;
							}

							if (isNewTask) {
								task.setCreatedAt(LocalDateTime.now());
							}
							task.setUpdatedAt(LocalDateTime.now());

							task = taskRepository.save(task);
							currentTask = task;

							// Подсчёт созданных вопросов
							if (isNewTask) {
								if (!createdQuestionTexts.contains(task.getQuestion())) {
									createdQuestions++;
									createdQuestionTexts.add(task.getQuestion());
									log.info("Создан новый вопрос с ID: {}, текущий счетчик: {}", task.getId(), createdQuestions);
								}
							}
						}
					} else {
						// Это дополнительный ответ к предыдущему вопросу
						log.info("Дополнительный ответ к вопросу с ID: {}", currentTask.getId());
						// currentTask уже установлен
					}

					// Теперь работаем с ответами
					if (answerText == null || answerText.trim().isEmpty()) {
						log.warn("Строка {}: Пустой текст ответа, пропускаем", rowNum + 1);
						continue;
					}

					if (answerId != null && answerId > 0) {
						// Ищем существующий ответ в репозитории
						var answerOptional = answerRepository.findById(answerId);
						if (answerOptional.isPresent()) {
							var answer = answerOptional.get();
							// Проверяем, принадлежит ли ответ текущему вопросу
							if (answer.getTask() != null && answer.getTask().getId().equals(currentTask.getId())) {
								// Обновляем существующий ответ
								answer.setContent(answerText);
								answer.setCorrect(isCorrect);
								answer.setExplanation(explanation);
								answer.setUpdatedAt(LocalDateTime.now());
								answerRepository.save(answer);
								updatedAnswers++;
								log.info("Обновлен ответ с ID: {} для вопроса с ID: {}", answer.getId(), currentTask.getId());
							} else {
								// Ответ существует, но принадлежит другому вопросу
								// Создаем новый ответ для текущего вопроса
								var newAnswer = new com.example.javaoffer.exam.entity.Answer();
								newAnswer.setTask(currentTask);
								newAnswer.setContent(answerText);
								newAnswer.setCorrect(isCorrect);
								newAnswer.setExplanation(explanation);
								newAnswer.setCreatedAt(LocalDateTime.now());
								newAnswer.setUpdatedAt(LocalDateTime.now());
								answerRepository.save(newAnswer);
								createdAnswers++;
								log.info("Создан новый ответ для вопроса с ID: {}", currentTask.getId());
							}
						} else {
							// Ответ с указанным ID не найден, создаем новый
							var newAnswer = new com.example.javaoffer.exam.entity.Answer();
							newAnswer.setTask(currentTask);
							newAnswer.setContent(answerText);
							newAnswer.setCorrect(isCorrect);
							newAnswer.setExplanation(explanation);
							newAnswer.setCreatedAt(LocalDateTime.now());
							newAnswer.setUpdatedAt(LocalDateTime.now());
							answerRepository.save(newAnswer);
							createdAnswers++;
							log.info("Создан новый ответ для вопроса с ID: {} (ответ с ID {} не найден)", currentTask.getId(), answerId);
						}
					} else {
						// ID ответа не указан, создаем новый ответ
						var newAnswer = new com.example.javaoffer.exam.entity.Answer();
						newAnswer.setTask(currentTask);
						newAnswer.setContent(answerText);
						newAnswer.setCorrect(isCorrect);
						newAnswer.setExplanation(explanation);
						newAnswer.setCreatedAt(LocalDateTime.now());
						newAnswer.setUpdatedAt(LocalDateTime.now());
						answerRepository.save(newAnswer);
						createdAnswers++;
						log.info("Создан новый ответ для вопроса с ID: {}", currentTask.getId());
					}
				} catch (Exception ex) {
					errorLines++;
					errors.append("Ошибка в строке ").append(rowNum + 1).append(": ").append(ex.getMessage()).append("\n");
					log.error("Ошибка при импорте строки " + (rowNum + 1), ex);
				}
			}

			String result = "Обновлено вопросов: " + updatedQuestions +
					", создано вопросов: " + createdQuestions +
					", обновлено ответов: " + updatedAnswers +
					", создано ответов: " + createdAnswers;

			log.info("Итоговые счетчики: {}", result);

			if (errorLines > 0) {
				result += ", ошибок: " + errorLines;
			}

			if (!errors.isEmpty()) {
				// Ограничиваем длину сообщения об ошибках
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
	 * Экспортирует вопросы в Excel-файл.
	 * <p>
	 * Метод извлекает все вопросы и ответы из базы данных и записывает их
	 * в Excel-файл. Для каждого вопроса создается отдельная строка с его данными
	 * и первым ответом. Дополнительные ответы добавляются в отдельные строки.
	 *
	 * <p>
	 * Метод обновляет запись истории экспорта по завершении операции.
	 *
	 * @param outputStream выходной поток для записи Excel-файла
	 * @param history      запись истории экспорта для отслеживания результата
	 */
	@Transactional
	public void exportQuestionsToExcel(OutputStream outputStream, ImportExportHistory history) {
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Вопросы");

			// Создаем стили для заголовка
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);

			// Создаем заголовок
			Row headerRow = sheet.createRow(0);
			String[] headers = {"question_id", "question_text", "topic", "difficulty", "grade", "answer_id", "answer_text", "is_correct", "explanation"};
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			// Заполняем данными
			List<Task> tasks = taskRepository.findAll();
			int rowNum = 1;
			for (Task task : tasks) {
				List<com.example.javaoffer.exam.entity.Answer> answers = task.getAnswers();
				if (answers == null || answers.isEmpty()) {
					// Если у вопроса нет ответов, все равно добавляем строку с вопросом
					Row row = sheet.createRow(rowNum++);

					row.createCell(0).setCellValue(task.getId() != null ? task.getId() : 0);
					row.createCell(1).setCellValue(task.getQuestion() != null ? task.getQuestion() : "");
					row.createCell(2).setCellValue(task.getTopic() != null ? task.getTopic().name() : "");
					row.createCell(3).setCellValue(task.getDifficulty() != null ? task.getDifficulty().name() : "");
					row.createCell(4).setCellValue(task.getGrade() != null ? task.getGrade().name() : "");
					// Оставляем ячейки ответа пустыми
				} else {
					boolean isFirstAnswer = true;
					for (var answer : answers) {
						Row row = sheet.createRow(rowNum++);

						// ID вопроса всегда указываем
						row.createCell(0).setCellValue(task.getId() != null ? task.getId() : 0);

						if (isFirstAnswer) {
							// Для первого ответа указываем все данные вопроса
							row.createCell(1).setCellValue(task.getQuestion() != null ? task.getQuestion() : "");
							row.createCell(2).setCellValue(task.getTopic() != null ? task.getTopic().name() : "");
							row.createCell(3).setCellValue(task.getDifficulty() != null ? task.getDifficulty().name() : "");
							row.createCell(4).setCellValue(task.getGrade() != null ? task.getGrade().name() : "");
							isFirstAnswer = false;
						} else {
							// Для остальных ответов оставляем поля вопроса пустыми
							row.createCell(1).setCellValue("");
							row.createCell(2).setCellValue("");
							row.createCell(3).setCellValue("");
							row.createCell(4).setCellValue("");
						}

						// Данные ответа
						row.createCell(5).setCellValue(answer.getId() != null ? answer.getId() : 0);
						row.createCell(6).setCellValue(answer.getContent() != null ? answer.getContent() : "");
						row.createCell(7).setCellValue(answer.isCorrect());
						row.createCell(8).setCellValue(answer.getExplanation() != null ? answer.getExplanation() : "");
					}
				}
			}

			// Автоматически регулируем ширину столбцов
			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			// Записываем в выходной поток
			workbook.write(outputStream);
			outputStream.flush();

			historyService.markAsSuccess(history, "Экспортировано вопросов: " + tasks.size());
		} catch (Exception e) {
			log.error("Ошибка экспорта вопросов в Excel", e);
			historyService.markAsError(history, e.getMessage());
		}
	}

	/**
	 * Извлекает числовое значение из ячейки Excel и преобразует его в Long.
	 * <p>
	 * Метод поддерживает извлечение числовых значений как из числовых ячеек,
	 * так и из строковых ячеек, содержащих числа.
	 *
	 * @param cell ячейка Excel
	 * @return числовое значение или null, если значение не может быть преобразовано
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
	 * Извлекает строковое значение из ячейки Excel.
	 * <p>
	 * Метод поддерживает извлечение строковых значений из ячеек различных типов:
	 * строковых, числовых и логических.
	 *
	 * @param cell ячейка Excel
	 * @return строковое значение или null, если ячейка пуста или значение не может быть преобразовано
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
	 * Извлекает логическое значение из ячейки Excel.
	 * <p>
	 * Метод поддерживает извлечение логических значений из ячеек различных типов:
	 * логических, строковых (содержащих "true", "yes", "1", "да") и числовых (ненулевые значения считаются true).
	 *
	 * @param cell ячейка Excel
	 * @return логическое значение (false по умолчанию)
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
} 
