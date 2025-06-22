package com.example.javaoffer.admin.entity;

import com.example.javaoffer.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сущность для хранения истории импорта и экспорта вопросов.
 * <p>
 * Хранит информацию о проведенных операциях импорта/экспорта вопросов,
 * включая пользователя, выполнившего операцию, тип операции, статус выполнения,
 * время начала и завершения, а также результат операции.
 *
 * @author Garbuzov Oleg
 */
@Entity
@Table(name = "import_export_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportExportHistory {
	/**
	 * Тип операции: Импорт
	 */
	public static final String OPERATION_TYPE_IMPORT = "IMPORT";

	/**
	 * Тип операции: Экспорт
	 */
	public static final String OPERATION_TYPE_EXPORT = "EXPORT";

	/**
	 * Статус операции: В процессе
	 */
	public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

	/**
	 * Статус операции: Успешно завершена
	 */
	public static final String STATUS_SUCCESS = "SUCCESS";

	/**
	 * Статус операции: Завершена с ошибкой
	 */
	public static final String STATUS_ERROR = "ERROR";

	/**
	 * Уникальный идентификатор записи истории
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "import_export_history_seq_gen")
	@SequenceGenerator(name = "import_export_history_seq_gen", sequenceName = "import_export_history_seq", allocationSize = 1)
	private Long id;

	/**
	 * Пользователь, выполнивший операцию
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * Тип операции (IMPORT или EXPORT)
	 *
	 * @see #OPERATION_TYPE_IMPORT
	 * @see #OPERATION_TYPE_EXPORT
	 */
	@Column(name = "operation_type", nullable = false)
	private String operationType;

	/**
	 * Имя файла, участвующего в операции
	 */
	@Column(name = "file_name", nullable = false)
	private String fileName;

	/**
	 * Статус операции (IN_PROGRESS, SUCCESS, ERROR)
	 *
	 * @see #STATUS_IN_PROGRESS
	 * @see #STATUS_SUCCESS
	 * @see #STATUS_ERROR
	 */
	@Column(name = "status", nullable = false)
	private String status;

	/**
	 * Дата и время начала операции
	 */
	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt;

	/**
	 * Дата и время завершения операции
	 */
	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	/**
	 * Результат операции (сообщение об успехе или ошибке)
	 */
	@Column(name = "result")
	private String result;
} 