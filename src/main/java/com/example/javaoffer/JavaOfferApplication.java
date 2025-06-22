package com.example.javaoffer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Главный класс Spring Boot приложения JavaOffer.
 * <p>
 * Приложение представляет собой систему онлайн-экзаменов и тестирования
 * с функциями администрирования, аутентификации пользователей,
 * антишпионского контроля и рейтинговой системы.
 * 
 * <p>
 * Основные функции системы:
 * <ul>
 *   <li>Регистрация и аутентификация пользователей</li>
 *   <li>Проведение экзаменов в различных режимах (свободный, рейтинговый, интервью)</li>
 *   <li>Антишпионский контроль во время экзаменов</li>
 *   <li>Административная панель для управления пользователями и вопросами</li>
 *   <li>Система обратной связи</li>
 * </ul>
 * 
 * 
 * @author Garbuzov Oleg
 */
@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableAspectJAutoProxy
@Slf4j
public class JavaOfferApplication {

	/**
	 * Точка входа в приложение.
	 * <p>
	 * Запускает Spring Boot приложение с переданными аргументами командной строки.
	 * Логирует информацию о запуске приложения.
	 * 
	 *
	 * @param args аргументы командной строки
	 */
	public static void main(String[] args) {
		// Устанавливаем кодировку по умолчанию для JVM
		System.setProperty("file.encoding", "UTF-8");
		System.setProperty("sun.jnu.encoding", "UTF-8");
		
		log.info("=== Запуск приложения JavaOffer ===");
		log.info("Автор: Garbuzov Oleg");
		log.debug("Аргументы запуска: {}", (Object) args);
		
		SpringApplication.run(JavaOfferApplication.class, args);
		log.info("=== Приложение JavaOffer успешно запущено ===");
	}

	/**
	 * Конфигурирует резолвер параметров пагинации.
	 * <p>
	 * Настраивает использование индексации страниц с 1 (вместо стандартной с 0),
	 * что более интуитивно для пользователей веб-интерфейса.
	 * 
	 *
	 * @return кастомизатор для резолвера параметров пагинации
	 */
	@Bean
	public PageableHandlerMethodArgumentResolverCustomizer pageableResolverCustomizer() {
		log.debug("Настройка резолвера пагинации: индексация страниц начинается с 1");
		return pageableResolver -> {
			pageableResolver.setOneIndexedParameters(true);
			log.trace("Резолвер пагинации настроен: oneIndexedParameters = true");
		};
	}
}
