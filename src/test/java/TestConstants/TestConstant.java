package TestConstants;

/**
 * Класс содержит константы для тестирования различных контроллеров приложения.
 * <p>
 * Включает в себя:
 * <ul>
 *   <li>Константы для тестирования QuestionFilterController</li>
 *   <li>Константы для тестирования ImportExportHistoryController</li>
 *   <li>Константы для тестирования AntiCheatController</li>
 *   <li>URL пути для различных эндпоинтов</li>
 * </ul>
 * </p>
 *
 * @author Garbuzov Oleg
 * @since 1.0
 */
public final class TestConstant {

	/**
	 * Приватный конструктор для предотвращения создания экземпляров утилитного класса.
	 */
	private TestConstant() {
		throw new AssertionError("Utility class cannot be instantiated");
	}

	// === Константы для QuestionFilterController ===

	/**
	 * Тема вопросов: базовые знания Java
	 */
	public static final String TOPIC_CORE = "CORE";

	/**
	 * Уровень сложности: средний первый уровень
	 */
	public static final String DIFFICULTY_MEDIUM1 = "MEDIUM1";

	/**
	 * Грейд пользователя: младший разработчик
	 */
	public static final String GRADE_JUNIOR = "JUNIOR";

	// === Константы для ImportExportHistoryController ===

	/**
	 * Тип операции: импорт данных
	 */
	public static final String OPERATION_TYPE_IMPORT = "IMPORT";

	/**
	 * Тип операции: экспорт данных
	 */
	public static final String OPERATION_TYPE_EXPORT = "EXPORT";

	/**
	 * Имя файла для импорта вопросов
	 */
	public static final String FILE_NAME_IMPORT = "questions_import.xlsx";

	/**
	 * Имя файла для экспорта вопросов
	 */
	public static final String FILE_NAME_EXPORT = "questions_export.xlsx";

	/**
	 * Статус операции: успешно завершена
	 */
	public static final String STATUS_SUCCESS = "SUCCESS";

	/**
	 * Статус операции: завершена с ошибкой
	 */
	public static final String STATUS_ERROR = "ERROR";

	/**
	 * Результат успешного экспорта
	 */
	public static final String RESULT_SUCCESS = "Экспортировано вопросов: 5";

	/**
	 * Результат с ошибкой
	 */
	public static final String RESULT_ERROR = "Ошибка: Некорректный формат файла";

	// === URL пути для тестирования эндпоинтов ===

	/**
	 * URL для фильтрации вопросов в админ панели
	 */
	public static final String URL_ADMIN_FILTER_QUESTIONS = "/admin/filter/questions";

	/**
	 * URL для просмотра истории импорта/экспорта
	 */
	public static final String URL_ADMIN_IMPORT_EXPORT_HISTORY = "/admin/import-export-history";

	/**
	 * URL для очистки истории импорта/экспорта
	 */
	public static final String URL_ADMIN_IMPORT_EXPORT_HISTORY_CLEAR = "/admin/import-export-history/clear";

	/**
	 * URL для проверки статуса античит системы
	 */
	public static final String URL_ANTICHEAT_STATUS = "/api/ui-feedback/status";
} 