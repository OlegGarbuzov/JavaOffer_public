package com.example.javaoffer;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.Charset;

/**
 * Основной тестовый класс для проверки загрузки Spring Boot приложения JavaOffer.
 * <p>
 * Класс выполняет базовые интеграционные тесты:
 * <ul>
 *   <li>Проверка успешной загрузки контекста приложения</li>
 *   <li>Настройка кодировки для корректной работы с русскими символами</li>
 *   <li>Активация тестового профиля для изоляции тестовой среды</li>
 * </ul>
 * </p>
 *
 * @author Garbuzov Oleg
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JavaOfferApplicationTests {

	/**
	 * Выполняет первоначальную настройку окружения для тестов.
	 * <p>
	 * Устанавливает кодировку UTF-8 для корректной работы с кириллическими символами
	 * в тестах и выводе в консоль.
	 * </p>
	 */
	@BeforeAll
	static void setUpEncoding() {
		log.info("Настройка кодировки UTF-8 для тестового окружения");

		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("sun.jnu.encoding", "UTF-8");

		log.debug("Установлена кодировка file.encoding: {}", Charset.defaultCharset().displayName());
		log.debug("Установлена кодировка sun.jnu.encoding: {}", System.getProperty("sun.jnu.encoding"));
	}

	/**
	 * Базовый тест для проверки успешной загрузки контекста Spring Boot приложения.
	 * <p>
	 * Если данный тест проходит успешно, это означает, что:
	 * <ul>
	 *   <li>Все Spring компоненты правильно сконфигурированы</li>
	 *   <li>Отсутствуют циклические зависимости</li>
	 *   <li>Все необходимые бины созданы и готовы к использованию</li>
	 * </ul>
	 * </p>
	 */
	@Test
	void contextLoads() {
		log.info("Запуск теста загрузки контекста приложения");
		log.info("Контекст Spring Boot приложения успешно загружен");
	}
}