package com.example.javaoffer.exam.anticheat.enums;

import com.example.javaoffer.exam.anticheat.exception.NoMatchFieldAntiCheatEventTypeException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Перечисление, представляющее типы событий и нарушений при прохождении экзамена.
 * <p>
 * Особенностью реализации является использование двух наборов имен для каждого типа события:
 * 
 * <ul>
 *     <li>Внутреннее имя (имя enum) - понятное серверное название, используемое в коде сервера
 *     для логичной и ясной обработки событий (например, TAB_SWITCH, TEXT_COPY, DEVTOOLS).</li>
 *     <li>Клиентское имя (jsonName) - намеренно неочевидное название, используемое при передаче
 *     данных между клиентом и сервером (например, UI_PERFORMANCE_CHECK вместо TAB_SWITCH).</li>
 * </ul>
 * <p>
 * Такой подход обеспечивает дополнительный уровень безопасности - затрудняет понимание
 * механизма античита для потенциальных нарушителей, анализирующих сетевой трафик или
 * исследующих JavaScript код клиента. Пользователь, пытающийся обойти защиту, не сможет
 * легко определить, какие именно действия отслеживаются и как они обрабатываются.
 * 
 * <p>
 * Для преобразования клиентских имен во внутренние используется статическая карта MAP
 * и метод fromJson, который вызывается при десериализации JSON.
 * 
 */
@Getter
@RequiredArgsConstructor
public enum EventType {

	/**
	 * Событие проверки активности (heartbeat).
	 * Клиентское название намеренно маскирует его как проверку меню UI.
	 */
	HEART_BEAT("UI_MENU_HB_CHECK"),
	
	/**
	 * Событие переключения вкладки браузера.
	 * Клиентское название маскирует его как проверку производительности UI.
	 */
	TAB_SWITCH("UI_PERFORMANCE_CHECK"),
	
	/**
	 * Событие копирования текста.
	 * Клиентское название маскирует его как обработку текста UI.
	 */
	TEXT_COPY("UI_TEXT_PROCESSING"),
	
	/**
	 * Событие открытия инструментов разработчика (DevTools).
	 * Клиентское название маскирует его как отрисовку кадра UI.
	 */
	DEVTOOLS("UI_PAINT_FRAME"),
	
	/**
	 * Событие вмешательства в DOM структуру страницы.
	 * Клиентское название маскирует его как рендеринг макета UI.
	 */
	DOM_TAMPERING("UI_LAYOUT_RENDER"),
	
	/**
	 * Событие модификации JavaScript функций.
	 * Клиентское название маскирует его как обертку функций UI.
	 */
	FUNCTION_TAMPERING("UI_FUNCTION_WRAPPER"),
	
	/**
	 * Событие вмешательства в модули JavaScript.
	 * Клиентское название маскирует его как обработчик модулей UI.
	 */
	MODULE_TAMPERING("UI_MODULE_HANDLER"),
	
	/**
	 * Событие закрытия страницы.
	 * Клиентское название маскирует его как жизненный цикл страницы UI.
	 */
	PAGE_CLOSE("UI_PAGE_LIFECYCLE"),
	
	/**
	 * Событие загрузки внешнего контента.
	 * Клиентское название маскирует его как проверку безопасности контента UI.
	 */
	EXTERNAL_CONTENT("UI_CONTENT_SECURITY"),
	
	/**
	 * Событие вмешательства в механизм Anti-OCR.
	 * Клиентское название маскирует его как проверку меню.
	 */
	ANTI_OCR_TAMP("UI_MENU_OC_TAM");

	/**
	 * Клиентское название события, используемое при передаче данных в JSON.
	 * Намеренно неинформативное для затруднения анализа.
	 */
	private final String jsonName;

	/**
	 * Карта соответствия клиентских имен (из JSON) внутренним типам событий.
	 * Используется для быстрого поиска типа события по его клиентскому имени.
	 */
	private static final Map<String, EventType> MAP;

	static {
		Map<String, EventType> map = new HashMap<>();
		for (EventType type : values()) {
			if (map.put(type.jsonName, type) != null) {
				throw new IllegalStateException("Duplicate jsonName: " + type.jsonName);
			}
		}
		MAP = Collections.unmodifiableMap(map);
	}

	/**
	 * Преобразует клиентское имя события из JSON в соответствующий тип EventType.
	 * <p>
	 * Метод используется Jackson при десериализации JSON и позволяет преобразовать
	 * неочевидные клиентские имена во внутренние типы событий.
	 * 
	 *
	 * @param jsonValue клиентское имя события из JSON
	 * @return соответствующий тип события EventType
	 * @throws NoMatchFieldAntiCheatEventTypeException если клиентское имя не соответствует ни одному типу
	 */
	@JsonCreator
	public static EventType fromJson(String jsonValue) {
		EventType result = MAP.get(jsonValue);
		if (result == null) {
			throw new NoMatchFieldAntiCheatEventTypeException(jsonValue);
		}
		return result;
	}
}