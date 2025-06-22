package com.example.javaoffer.exam.anticheat.antiocr.dto;

import lombok.Data;

/**
 * DTO для передачи настроек Anti-OCR между клиентом и сервером.
 */
@Data
public class AntiOcrConfigDTO {
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
	private CanvasMethodDTO canvasMethod = new CanvasMethodDTO();

	/**
	 * Настройки цветов текста для разных тем
	 */
	private TextColorDTO textColor = new TextColorDTO();

	/**
	 * Вложенный класс для настроек метода Canvas
	 */
	@Data
	public static class CanvasMethodDTO {
		/**
		 * Включен ли метод Canvas
		 */
		private boolean enabled = true;

		/**
		 * Интервал обновления искажения в мс
		 */
		private int refreshInterval = 1000;

		/**
		 * Максимальная амплитуда смещения по горизонтали (в пикселях)
		 */
		private double charSpacingVariation = 2.0;

		/**
		 * Максимальная амплитуда смещения по вертикали (в пикселях)
		 */
		private double charVerticalVariation = 3.0;

		/**
		 * Максимальная амплитуда вращения символов (в градусах)
		 */
		private double charRotationVariation = 10.0;

		/**
		 * Вариация размера шрифта (в пикселях)
		 */
		private double fontSizeVariation = 2.0;
	}

	/**
	 * Вложенный класс для настроек цветов текста
	 */
	@Data
	public static class TextColorDTO {
		/**
		 * Цвет текста для светлой темы
		 */
		private String lightTheme = "#000000";

		/**
		 * Цвет текста для темной темы
		 */
		private String darkTheme = "#e0e0e0";
	}
} 