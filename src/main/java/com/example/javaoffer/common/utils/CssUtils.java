package com.example.javaoffer.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Утилитарный класс для управления путями к CSS файлам.
 * Автоматически переключает между обычными и минифицированными версиями 
 * в зависимости от конфигурации приложения.
 */
@Component
public class CssUtils {

    @Value("${app.css.minified:false}")
    private boolean useMinifiedCss;

    /**
     * Получить путь к CSS файлу с учетом минификации.
     * 
     * @param cssPath исходный путь к CSS файлу
     * @return путь к минифицированному файлу если включена минификация, иначе исходный путь
     */
    public String getCssPath(String cssPath) {
        if (!useMinifiedCss) {
            return cssPath;
        }
        
        // Пропускаем уже минифицированные файлы
        if (cssPath.contains(".min.css")) {
            return cssPath;
        }
        
        // Пропускаем внешние ссылки
        if (cssPath.startsWith("http://") || cssPath.startsWith("https://")) {
            return cssPath;
        }
        
        // Пропускаем Bootstrap файлы (они уже минифицированы)
        if (cssPath.contains("bootstrap")) {
            return cssPath;
        }
        
        // Заменяем .css на .min.css
        if (cssPath.endsWith(".css")) {
            return cssPath.substring(0, cssPath.length() - 4) + ".min.css";
        }
        
        return cssPath;
    }

    /**
     * Проверить, включена ли минификация CSS.
     * 
     * @return true если включена минификация
     */
    public boolean isMinifiedCssEnabled() {
        return useMinifiedCss;
    }
} 