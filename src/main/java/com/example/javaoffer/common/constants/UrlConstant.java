package com.example.javaoffer.common.constants;

/**
 * Константы URL-путей приложения.
 * <p>
 * Содержит все URL-адреса, используемые в приложении, сгруппированные по функциональным областям:
 * <ul>
 *   <li>Основные URL-пути</li>
 *   <li>URL для авторизации и аутентификации</li>
 *   <li>URL для экзаменов</li>
 *   <li>URL для административной панели</li>
 *   <li>URL для API</li>
 *   <li>URL для античита</li>
 * </ul>
 * 
 */
public class UrlConstant {

	// ======== Основные URL-пути ========

	/**
	 * Корневой URL приложения
	 */
	public static final String URL_ROOT = "/";

	/**
	 * URL для страницы контактов
	 */
	public static final String URL_CONTACT = "/contact";

	/**
	 * URL для обратной связи приложения
	 */
	public static final String URL_FEEDBACK = "/feedBack";

	/**
	 * URL для страницы глобального рейтинга
	 */
	public static final String URL_GLOBAL_RATING = "/global-rating";

	// ======== URL для авторизации и аутентификации ========

	/**
	 * URL для страницы входа
	 */
	public static final String URL_LOGIN = "/login";

	/**
	 * URL для страницы регистрации
	 */
	public static final String URL_REGISTER = "/register";

	/**
	 * URL для выхода из системы
	 */
	public static final String URL_LOGOUT = "/logout";

	// ======== URL для экзаменов ========

	/**
	 * URL для выбора режима экзамена
	 */
	public static final String URL_MODE_SELECT = "/modeSelect";

	/**
	 * Корневой URL экзамена
	 */
	public static final String URL_EXAM_ROOT = "/testing";

	/**
	 * URL для начала экзамена
	 */
	public static final String URL_START_EXAM = "/process";

	/**
	 * URL для получения следующего вопроса в экзамене
	 */
	public static final String URL_NEXT_QUESTION = "/nextQuestion";

	/**
	 * URL для валидации ответа пользователя
	 */
	public static final String URL_ANSWER_CHECK = "/answerCheck";

	/**
	 * URL для прерывания экзамена
	 */
	public static final String URL_ABORT_EXAM = "/abort";

	// ======== URL для административной панели ========

	/**
	 * Корневой URL админки приложения
	 */
	public static final String URL_ADMIN_ROOT = "/admin";

	/**
	 * URL для страницы с вопросами в админке
	 */
	public static final String URL_ADMIN_QUESTIONS = "/questions";

	/**
	 * URL для удаления вопроса по ID в админке
	 */
	public static final String URL_ADMIN_QUESTIONS_ID_DELETE = "/admin/questions/{id}/delete";

	/**
	 * URL для получения данных вопроса по ID в админке
	 */
	public static final String URL_ADMIN_QUESTIONS_ID_DATA = "/admin/questions/{id}/data";

	/**
	 * URL для страницы с обратной связью от пользователей в админке
	 */
	public static final String URL_ADMIN_FEEDBACK = "/contact";

	/**
	 * URL для страницы с пользователями в админке
	 */
	public static final String URL_ADMIN_USERS = "/users";

	/**
	 * Корневой URL для фильтрации в админке
	 */
	public static final String URL_ADMIN_FILTER_ROOT = "/admin/filter";

	/**
	 * URL для фильтрации вопросов в админке
	 */
	public static final String URL_ADMIN_FILTER_QUESTIONS = "/questions";

	/**
	 * URL для страницы истории импорта/экспорта
	 */
	public static final String URL_ADMIN_IMPORT_EXPORT_HISTORY = "/import-export-history";

	/**
	 * URL для экспорта вопросов в Excel
	 */
	public static final String URL_ADMIN_QUESTIONS_EXPORT_EXCEL = "/questions/export-excel";

	/**
	 * URL для импорта вопросов из Excel
	 */
	public static final String URL_ADMIN_QUESTIONS_IMPORT_EXCEL = "/questions/import-excel";

	/**
	 * URL для очистки истории импорта/экспорта
	 */
	public static final String URL_ADMIN_IMPORT_EXPORT_HISTORY_CLEAR = "/import-export-history/clear";

	/**
	 * Корневой URL для отладки кэша
	 */
	public static final String URL_ADMIN_DEBUG_CACHE_ROOT = "/debug/cache";

	/**
	 * Полный URL для отладки кэша
	 */
	public static final String URL_ADMIN_DEBUG_CACHE_FULL = URL_ADMIN_ROOT + URL_ADMIN_DEBUG_CACHE_ROOT;

	/**
	 * URL для просмотра прогресса экзаменов в кэше
	 */
	public static final String URL_ADMIN_DEBUG_CACHE_PROGRESS = "/progress";

	/**
	 * URL для страницы настроек Anti-OCR
	 */
	public static final String URL_ADMIN_OCR_SETTINGS = "/config";

	/**
	 * URL для страницы настроек Anti-OCR в админке
	 */
	public static final String URL_ADMIN_OCR = "/antiOcrSettings";

	/**
	 * Корневой URL для истории экзаменов в админке
	 */
	public static final String URL_ADMIN_EXAM_HISTORY_ROOT = "/exam-history";

	/**
	 * URL для просмотра деталей истории экзамена
	 */
	public static final String URL_ADMIN_EXAM_HISTORY_DETAILS = "/details";

	/**
	 * URL для удаления истории экзамена
	 */
	public static final String URL_ADMIN_EXAM_HISTORY_DELETE = "/delete";

	// ======== URL для API ========

	/**
	 * URL для API интерфейса
	 */
	public static final String API_V1_PREFIX = "/api/v1";

	/**
	 * REST: Получение списка пользователей
	 */
	public static final String URL_ADMIN_API_USERS = "/api/users";

	/**
	 * REST: Получение одного пользователя
	 */
	public static final String URL_ADMIN_API_USER_BY_ID = "/api/users/{id}";

	/**
	 * REST: Истории пользователя
	 */
	public static final String URL_ADMIN_API_USER_SCORE_HISTORIES = "/api/users/{id}/score-histories";

	/**
	 * REST: История пользователя по id
	 */
	public static final String URL_ADMIN_API_USER_SCORE_HISTORY_BY_ID = "/api/user-score-history/{historyId}";

	/**
	 * REST: Вопрос по id
	 */
	public static final String URL_ADMIN_QUESTION_BY_ID = "/questions/{id}";

	/**
	 * REST: Удаление вопроса
	 */
	public static final String URL_ADMIN_QUESTION_DELETE = "/questions/{id}/delete";

	/**
	 * REST: Данные вопроса
	 */
	public static final String URL_ADMIN_QUESTION_DATA = URL_ADMIN_QUESTIONS + "/{id}/data";

	/**
	 * REST: Получение обращений обратной связи
	 */
	public static final String URL_ADMIN_API_FEEDBACK = "/api/feedback";

	/**
	 * REST: Удаление обращения обратной связи
	 */
	public static final String URL_ADMIN_API_FEEDBACK_BY_ID = "/api/feedback/{id}";

	/**
	 * URL для клиентской конфигурации интерфейса
	 */
	public static final String URL_CLIENT_OCR_CONFIG = API_V1_PREFIX + "/ui/client-settings";

	// ======== URL для античита ========

	/**
	 * Корневой URL для UI интерфейса Anti-OCR
	 */
	public static final String URL_ANTI_OCR_ROOT = "/antiocr";

	/**
	 * Корневой URL для API античита
	 */
	public static final String URL_ANTICHEAT_API_ROOT = "/api/ui-feedback";

	/**
	 * URL для проверки статуса и отправки heartbeat
	 */
	public static final String URL_ANTICHEAT_STATUS = "/status";

	/**
	 * Полный URL для проверки статуса и отправки heartbeat.
	 * Также используется как унифицированный URL для всех запросов античита
	 */
	public static final String URL_ANTICHEAT_STATUS_FULL = URL_ANTICHEAT_API_ROOT + URL_ANTICHEAT_STATUS;
}

