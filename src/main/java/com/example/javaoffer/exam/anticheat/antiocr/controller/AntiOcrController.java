package com.example.javaoffer.exam.anticheat.antiocr.controller;

import com.example.javaoffer.exam.anticheat.antiocr.config.AntiOcrProperties;
import com.example.javaoffer.exam.anticheat.antiocr.dto.AntiOcrConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.example.javaoffer.common.constants.UrlConstant.*;

/**
 * Контроллер для управления настройками Anti-OCR.
 * <p>
 * Предоставляет API для получения и обновления настроек механизма защиты от OCR (оптического распознавания символов).
 * Система Anti-OCR применяет различные техники искажения текста для предотвращения его автоматического
 * копирования и распознавания.
 * 
 * <p>
 * Контроллер предоставляет два типа эндпоинтов:
 * <ul>
 *   <li>Административные - для настройки параметров Anti-OCR (требуют роль ADMIN)</li>
 *   <li>Клиентские - для получения текущих настроек клиентской частью приложения (публичные)</li>
 * </ul>
 * 
 * <p>
 * Настройки применяются в режиме реального времени, без необходимости перезапуска приложения.
 * 
 *
 * @author Garbuzov Oleg
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class AntiOcrController {

	private final AntiOcrProperties antiOcrProperties;

	/**
	 * Получение текущих настроек Anti-OCR для административной панели.
	 * <p>
	 * Метод возвращает полную конфигурацию системы Anti-OCR, включая все параметры
	 * для каждого метода защиты от OCR.
	 * 
	 * <p>
	 * Требует наличия роли ADMIN.
	 * 
	 *
	 * @return ResponseEntity с DTO, содержащим все настройки Anti-OCR
	 */
	@GetMapping(API_V1_PREFIX + URL_ANTI_OCR_ROOT + URL_ADMIN_OCR_SETTINGS)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<AntiOcrConfigDTO> getConfig() {
		log.debug("Запрос на получение административных настроек Anti-OCR");
		
		AntiOcrConfigDTO configDTO = mapPropertiesToDTO();
		
		log.debug("Возвращаем настройки Anti-OCR: enabled={}, enabledForQuestionText={}, enabledForCodeBlocks={}",
				configDTO.isEnabled(), configDTO.isEnabledForQuestionText(), configDTO.isEnabledForCodeBlocks());
		
		return ResponseEntity.ok(configDTO);
	}

	/**
	 * Обновление настроек Anti-OCR.
	 * <p>
	 * Метод принимает новую конфигурацию системы Anti-OCR и обновляет соответствующие свойства.
	 * После обновления возвращает актуальную конфигурацию.
	 * 
	 * <p>
	 * Требует наличия роли ADMIN.
	 * 
	 *
	 * @param configDTO DTO с новыми настройками Anti-OCR
	 * @return ResponseEntity с обновленными настройками Anti-OCR
	 */
	@PutMapping(API_V1_PREFIX + URL_ANTI_OCR_ROOT + URL_ADMIN_OCR_SETTINGS)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<AntiOcrConfigDTO> updateConfig(@RequestBody AntiOcrConfigDTO configDTO) {
		log.info("Обновление настроек Anti-OCR: enabled={}, enabledForQuestionText={}, enabledForCodeBlocks={}",
				configDTO.isEnabled(), configDTO.isEnabledForQuestionText(), configDTO.isEnabledForCodeBlocks());
		
		// Обновляем основные настройки
		updateMainProperties(configDTO);

		// Обновляем настройки Canvas
		updateCanvasMethodProperties(configDTO);

		// Обновляем настройки цветов
		updateTextColorProperties(configDTO);

		log.info("Настройки Anti-OCR успешно обновлены");
		return ResponseEntity.ok(configDTO);
	}

	/**
	 * Получение клиентских настроек Anti-OCR.
	 * <p>
	 * Этот метод доступен без аутентификации и возвращает настройки для клиентской части.
	 * Клиент использует эти настройки для применения защиты от OCR на страницах с вопросами экзамена.
	 * 
	 * <p>
	 * Возвращает те же данные, что и административный эндпоинт, но доступен всем пользователям.
	 * 
	 *
	 * @return ResponseEntity с DTO, содержащим настройки Anti-OCR для клиента
	 */
	@GetMapping(URL_CLIENT_OCR_CONFIG)
	public ResponseEntity<AntiOcrConfigDTO> getClientConfig() {
		log.debug("Запрос на получение клиентских настроек Anti-OCR");
		
		AntiOcrConfigDTO configDTO = mapPropertiesToDTO();
		
		log.trace("Возвращаем клиентские настройки Anti-OCR: enabled={}, canvasEnabled={}, " +
				"enabledForQuestionText={}, enabledForCodeBlocks={}",
				configDTO.isEnabled(), configDTO.getCanvasMethod().isEnabled(),
				configDTO.isEnabledForQuestionText(), configDTO.isEnabledForCodeBlocks());
		
		return ResponseEntity.ok(configDTO);
	}
	
	/**
	 * Преобразует свойства из AntiOcrProperties в DTO для передачи клиенту.
	 * <p>
	 * Метод копирует все настройки из объекта свойств в новый DTO объект,
	 * включая вложенные объекты для метода Canvas и настроек цветов.
	 * 
	 *
	 * @return заполненный объект AntiOcrConfigDTO
	 */
	private AntiOcrConfigDTO mapPropertiesToDTO() {
		AntiOcrConfigDTO configDTO = new AntiOcrConfigDTO();
		configDTO.setEnabled(antiOcrProperties.isEnabled());
		configDTO.setEnabledForQuestionText(antiOcrProperties.isEnabledForQuestionText());
		configDTO.setEnabledForCodeBlocks(antiOcrProperties.isEnabledForCodeBlocks());

		AntiOcrConfigDTO.CanvasMethodDTO canvasMethodDTO = new AntiOcrConfigDTO.CanvasMethodDTO();
		canvasMethodDTO.setEnabled(antiOcrProperties.getCanvasMethod().isEnabled());
		canvasMethodDTO.setRefreshInterval(antiOcrProperties.getCanvasMethod().getRefreshInterval());
		canvasMethodDTO.setCharRotationVariation(antiOcrProperties.getCanvasMethod().getCharRotationVariation());
		canvasMethodDTO.setCharSpacingVariation(antiOcrProperties.getCanvasMethod().getCharSpacingVariation());
		canvasMethodDTO.setCharVerticalVariation(antiOcrProperties.getCanvasMethod().getCharVerticalVariation());
		canvasMethodDTO.setFontSizeVariation(antiOcrProperties.getCanvasMethod().getFontSizeVariation());
		configDTO.setCanvasMethod(canvasMethodDTO);

		AntiOcrConfigDTO.TextColorDTO textColorDTO = new AntiOcrConfigDTO.TextColorDTO();
		textColorDTO.setLightTheme(antiOcrProperties.getTextColor().getLightTheme());
		textColorDTO.setDarkTheme(antiOcrProperties.getTextColor().getDarkTheme());
		configDTO.setTextColor(textColorDTO);
		
		return configDTO;
	}
	
	/**
	 * Обновляет основные свойства Anti-OCR.
	 * <p>
	 * Копирует базовые настройки из DTO в объект свойств приложения.
	 * 
	 *
	 * @param configDTO DTO с новыми настройками
	 */
	private void updateMainProperties(AntiOcrConfigDTO configDTO) {
		antiOcrProperties.setEnabled(configDTO.isEnabled());
		antiOcrProperties.setEnabledForQuestionText(configDTO.isEnabledForQuestionText());
		antiOcrProperties.setEnabledForCodeBlocks(configDTO.isEnabledForCodeBlocks());
		
		log.debug("Обновлены основные настройки Anti-OCR: enabled={}, enabledForQuestionText={}, enabledForCodeBlocks={}",
				antiOcrProperties.isEnabled(), antiOcrProperties.isEnabledForQuestionText(), antiOcrProperties.isEnabledForCodeBlocks());
	}
	
	/**
	 * Обновляет свойства метода Canvas для Anti-OCR.
	 * <p>
	 * Копирует настройки Canvas-метода из DTO в объект свойств приложения.
	 * 
	 *
	 * @param configDTO DTO с новыми настройками
	 */
	private void updateCanvasMethodProperties(AntiOcrConfigDTO configDTO) {
		if (configDTO.getCanvasMethod() != null) {
			log.debug("Обновление настроек Canvas: enabled={}, refreshInterval={}",
					configDTO.getCanvasMethod().isEnabled(), configDTO.getCanvasMethod().getRefreshInterval());
			
			antiOcrProperties.getCanvasMethod().setEnabled(configDTO.getCanvasMethod().isEnabled());
			antiOcrProperties.getCanvasMethod().setRefreshInterval(configDTO.getCanvasMethod().getRefreshInterval());
			antiOcrProperties.getCanvasMethod().setCharRotationVariation(configDTO.getCanvasMethod().getCharRotationVariation());
			antiOcrProperties.getCanvasMethod().setCharSpacingVariation(configDTO.getCanvasMethod().getCharSpacingVariation());
			antiOcrProperties.getCanvasMethod().setCharVerticalVariation(configDTO.getCanvasMethod().getCharVerticalVariation());
			antiOcrProperties.getCanvasMethod().setFontSizeVariation(configDTO.getCanvasMethod().getFontSizeVariation());
			
			log.debug("Обновлены параметры вариаций Canvas: rotation={}, spacing={}, vertical={}, fontSize={}",
					antiOcrProperties.getCanvasMethod().getCharRotationVariation(),
					antiOcrProperties.getCanvasMethod().getCharSpacingVariation(),
					antiOcrProperties.getCanvasMethod().getCharVerticalVariation(),
					antiOcrProperties.getCanvasMethod().getFontSizeVariation());
		}
	}
	
	/**
	 * Обновляет свойства цветов текста для Anti-OCR.
	 * <p>
	 * Копирует настройки цветов из DTO в объект свойств приложения.
	 * 
	 *
	 * @param configDTO DTO с новыми настройками
	 */
	private void updateTextColorProperties(AntiOcrConfigDTO configDTO) {
		if (configDTO.getTextColor() != null) {
			log.debug("Обновление настроек цветов: lightTheme={}, darkTheme={}",
					configDTO.getTextColor().getLightTheme(), configDTO.getTextColor().getDarkTheme());
			
			antiOcrProperties.getTextColor().setLightTheme(configDTO.getTextColor().getLightTheme());
			antiOcrProperties.getTextColor().setDarkTheme(configDTO.getTextColor().getDarkTheme());
		}
	}
} 