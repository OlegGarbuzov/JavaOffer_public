package com.example.javaoffer.common.constants;

/**
 * Константы имен представлений (templates).
 * <p>
 * Содержит названия всех Thymeleaf шаблонов, используемых в приложении,
 * сгруппированные по функциональным областям:
 * <ul>
 *   <li>Основные шаблоны</li>
 *   <li>Шаблоны авторизации</li>
 *   <li>Шаблоны экзаменов</li>
 *   <li>Шаблоны административной панели</li>
 *   <li>Шаблоны фрагментов</li>
 * </ul>
 * 
 */
public class ViewConstant {
	
	// ======== Основные шаблоны ========
	
	/**
	 * Шаблон главной страницы
	 */
	public static final String VIEW_TEMPLATE_INDEX = "index";
	
	/**
	 * Шаблон страницы контактов
	 */
	public static final String VIEW_TEMPLATE_CONTACT = "contact";
	
	// ======== Шаблоны авторизации ========
	
	/**
	 * Шаблон страницы входа
	 */
	public static final String VIEW_TEMPLATE_LOGIN = "login";

	/**
	 * Шаблон страницы регистрации
	 */
	public static final String VIEW_TEMPLATE_REGISTER = "register";
	
	// ======== Шаблоны экзаменов ========
	
	/**
	 * Шаблон страницы выбора режима экзамена
	 */
	public static final String VIEW_TEMPLATE_MODE_SELECT = "modeSelect";
	
	/**
	 * Шаблон страницы свободного экзамена
	 */
	public static final String VIEW_TEMPLATE_EXAM = "testing";
	
	/**
	 * Шаблон блока с результатами экзамена
	 */
	public static final String VIEW_TEMPLATE_EXAM_RESULT_BLOCK = "fragments/resultExam :: resultExamBlock";
	
	// ======== Шаблоны административной панели ========
	
	/**
	 * Шаблон страницы с вопросами в админке
	 */
	public static final String VIEW_TEMPLATE_ADMIN_QUESTIONS = "admin/questions";

	/**
	 * Путь к шаблону страницы с вопросами в админке (с начальным слешем)
	 */
	public static final String VIEW_TEMPLATE_ADMIN_QUESTIONS_WITH_SLASH = "/admin/questions";

	/**
	 * Шаблон страницы с пользователями в админке
	 */
	public static final String VIEW_TEMPLATE_ADMIN_USERS = "admin/users";

	/**
	 * Шаблон страницы с обратной связью от пользователей в админке
	 */
	public static final String VIEW_TEMPLATE_ADMIN_FEEDBACK = "admin/contact";

	/**
	 * Шаблон страницы отладки кэша экзаменов
	 */
	public static final String VIEW_TEMPLATE_ADMIN_CACHE_DEBUG = "admin/cache-debug";
	
	/**
	 * Шаблон страницы настроек интерфейса в админке
	 */
	public static final String VIEW_TEMPLATE_ADMIN_ANTI_OCR_SETTINGS = "admin/antiOcrSettings";

	/**
	 * Шаблон страницы истории экзаменов в админке
	 */
	public static final String VIEW_TEMPLATE_ADMIN_EXAM_HISTORY = "admin/exam-history";

	/**
	 * Шаблон страницы с детальной информацией об истории экзамена
	 */
	public static final String VIEW_TEMPLATE_ADMIN_EXAM_HISTORY_DETAILS = "admin/exam-history-details";

	/**
	 * Шаблон страницы истории импорта/экспорта
	 */
	public static final String VIEW_TEMPLATE_ADMIN_IMPORT_EXPORT_HISTORY = "admin/import-export-history";
	
	// ======== Шаблоны фрагментов ========
	
	/**
	 * Шаблон фрагмента с блоком вопроса
	 */
	public static final String VIEW_TEMPLATE_QUESTION_BLOCK = "fragments/question :: questionBlock";

	/**
	 * Шаблон фрагмента с строками таблицы вопросов
	 */
	public static final String VIEW_TEMPLATE_ADMIN_QUESTIONS_TBODY = "admin/fragments/questions-tbody :: rows";
}
