package com.example.javaoffer.exam.anticheat.antiocr.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Конфигурационные свойства для механизма Anti-OCR.
 * <p>
 * Этот класс загружает и хранит параметры конфигурации для механизма защиты от OCR распознавания
 * из файла настроек приложения с префиксом "exam.antiocr".
 * 
 */
@Component
@Data
@ConfigurationProperties("exam.antiocr")
public class AntiOcrProperties {
	/**
	 * Включен ли модуль Anti-OCR в целом
	 */
	private boolean enabled = true;

	/**
	 * Применять ли защиту к тексту вопроса
	 */
	private boolean enabledForQuestionText = true;

	/**
	 * Применять ли защиту к блокам кода
	 */
	private boolean enabledForCodeBlocks = true;

	/**
	 * Настройки метода Canvas для искажения текста
	 */
	private CanvasMethod canvasMethod = new CanvasMethod();

	/**
	 * Настройки цветов текста для разных тем
	 */
	private TextColor textColor = new TextColor();

	/**
	 * Вложенный класс для настроек метода Canvas
	 */
	@Data
	public static class CanvasMethod {
		/**
		 * Включен ли метод Canvas
		 */
		private boolean enabled = true;

		/**
		 * Интервал обновления искажения в мс
		 */
		private int refreshInterval = 5000;

		/**
		 * Максимальная амплитуда смещения по горизонтали (в пикселях)
		 */
		private double charSpacingVariation = 1.5;

		/**
		 * Максимальная амплитуда смещения по вертикали (в пикселях)
		 */
		private double charVerticalVariation = 10.0;

		/**
		 * Максимальная амплитуда вращения символов (в градусах)
		 */
		private double charRotationVariation = 25.0;

		/**
		 * Вариация размера шрифта (в пикселях)
		 */
		private double fontSizeVariation = 8.0;
	}

	/**
	 * Вложенный класс для настроек цветов текста
	 */
	@Data
	public static class TextColor {
		/**
		 * Цвет текста для светлой темы
		 */
		private String lightTheme = "#cfcece";

		/**
		 * Цвет текста для темной темы
		 */
		private String darkTheme = "#e0e0e0";
	}
}