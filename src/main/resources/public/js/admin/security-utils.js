/**
 * Утилиты безопасности для административного интерфейса
 */

// Функция для получения значения cookie по имени
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Настройка CSRF-защиты для AJAX-запросов
function setupCSRFProtection() {
    document.addEventListener('DOMContentLoaded', function() {
        const xsrfToken = getCookie('XSRF-TOKEN');
        if (xsrfToken) {
            // Добавляем перехватчик для всех AJAX запросов
            const originalFetch = window.fetch;
            window.fetch = function(url, options = {}) {
                // Если options.headers не существует, создаем его
                if (!options.headers) {
                    options.headers = {};
                }
                
                // Добавляем X-XSRF-TOKEN заголовок
                if (!options.headers['X-XSRF-TOKEN']) {
                    options.headers['X-XSRF-TOKEN'] = xsrfToken;
                }
                
                return originalFetch(url, options);
            };
        }
    });
}

// Инициализация всех утилит безопасности
function initSecurityUtils() {
    setupCSRFProtection();
}

// Автоматически инициализируем утилиты при загрузке скрипта
initSecurityUtils(); 