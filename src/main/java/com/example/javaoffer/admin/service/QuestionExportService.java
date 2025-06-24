package com.example.javaoffer.admin.service;

import com.example.javaoffer.admin.entity.ImportExportHistory;
import com.example.javaoffer.exam.entity.Answer;
import com.example.javaoffer.exam.entity.Task;
import com.example.javaoffer.exam.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

/**
 * Сервис для экспорта вопросов в формате Excel.
 * <p>
 * Предоставляет функционал для экспорта вопросов в Excel-файл.
 *
 * @author Garbuzov Oleg
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionExportService {
	private final TaskRepository taskRepository;
	private final ImportExportHistoryService historyService;

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
				List<Answer> answers = task.getAnswers();
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
} 