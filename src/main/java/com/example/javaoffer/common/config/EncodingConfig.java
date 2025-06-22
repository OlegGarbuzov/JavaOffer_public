package com.example.javaoffer.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.nio.charset.StandardCharsets;

/**
 * Конфигурация для обеспечения правильной работы с кодировкой UTF-8
 * во всем приложении, включая обработку YAML файлов с кириллическими символами.
 */
@Configuration
public class EncodingConfig {

    /**
     * Фильтр для принудительного использования UTF-8 кодировки
     * для всех HTTP запросов и ответов.
     *
     * @return настроенный фильтр кодировки
     */
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding(StandardCharsets.UTF_8.name());
        filter.setForceEncoding(true);
        return filter;
    }
} 