package com.example.javaoffer.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация кэширования статических ресурсов.
 * <p>
 * Настраивает правила кэширования для различных типов статических файлов:
 * <ul>
 *   <li>CSS и статические изображения кэшируются на 2 дня</li>
 *   <li>SEO-файлы (robots.txt, sitemap.xml) кэшируются на 1 день</li>
 *   <li>GIF-анимации, JS-файлы и другие ресурсы не кэшируются</li>
 * </ul>
 * 
 */
@Slf4j
@Configuration
public class WebCacheConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		log.info("Настройка кэширования статических ресурсов");

		// Кэшируем CSS файлы на 2 дня
		registry.addResourceHandler("/css/**")
				.addResourceLocations("classpath:/public/css/")
				.setCachePeriod(60 * 60 * 24 * 2); // 2 дней в секундах
		log.debug("CSS файлы: кэширование на 2 дня");

		// Кэшируем статические изображения на 2 дней
		registry.addResourceHandler("/img/**", "/img/favicons/**")
				.addResourceLocations("classpath:/public/img/", "classpath:/public/img/favicons/")
				.setCachePeriod(60 * 60 * 24 * 2); // 2 дней в секундах
		log.debug("Изображения: кэширование на 2 дня");

		// Запрещаем кэширование GIF-анимаций
		registry.addResourceHandler("/gifs/**")
				.addResourceLocations("classpath:/public/gifs/")
				.setCachePeriod(0); // Запрет кэширования
		log.debug("GIF-анимации: кэширование запрещено");

		// Запрещаем кэширование JS-файлов (включая античит-скрипты)
		registry.addResourceHandler("/js/**")
				.addResourceLocations("classpath:/public/js/")
				.setCachePeriod(0); // Запрет кэширования
		log.debug("JavaScript файлы: кэширование запрещено");

		// Настройка SEO-файлов
		registry.addResourceHandler("/robots.txt")
				.addResourceLocations("classpath:/")
				.setCachePeriod(60 * 60 * 24); // 1 день
		log.debug("robots.txt: кэширование на 1 день");

		registry.addResourceHandler("/sitemap.xml")
				.addResourceLocations("classpath:/")
				.setCachePeriod(60 * 60 * 24); // 1 день
		log.debug("sitemap.xml: кэширование на 1 день");

		// Запрещаем кэширование всех остальных ресурсов
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/public/")
				.setCachePeriod(0); // Запрет кэширования
		log.debug("Все остальные ресурсы: кэширование запрещено");
	}
} 