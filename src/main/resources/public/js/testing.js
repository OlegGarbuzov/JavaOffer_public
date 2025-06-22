document.addEventListener('DOMContentLoaded', function() {
    console.log('testing.js загружен');

    // Очищаем кэш ANTIOCR только при старте экзамена (не при смене вопросов)
    if (window._uiAntiOcrConfig && typeof window._uiAntiOcrConfig.clearCache === 'function') {
        // Проверяем, что это первый запуск страницы экзамена
        const examForm = document.getElementById('examForm');
        const examId = document.getElementById('examId');
        
        if (examForm && examId && examId.value) {
            console.log('Старт экзамена в testing режиме, очищаем кэш ANTIOCR конфигурации');
            window._uiAntiOcrConfig.clearCache();
        }
    }

    // Проверяем режим экзамена для активации античита
    const examMode = document.getElementById('examMode')?.value;
    const isRatingMode = examMode === 'RATING';
    
    console.log('Режим экзамена:', examMode);
    
    // Загружаем скрипты античита только для RATING режима
    if (isRatingMode) {
        console.log('Активация античита для RATING режима');
        // Здесь можно добавить динамическую загрузку скриптов античита, если необходимо
        
        // Проверяем наличие функции refreshAntiCheatToken и вызываем её
        if (window.refreshAntiCheatToken && typeof window.refreshAntiCheatToken === 'function') {
            window.refreshAntiCheatToken();
        }
    } else {
        console.log('FREE режим - античит не используется');
    }
    
    const abortBtn = document.getElementById('abortExamBtn');
    if (abortBtn) {
        if (typeof window.abortExam === 'undefined') {
            window.abortExam = function() {
                // Деактивируем античит при прерывании экзамена
                if (window._uiStateManager && typeof window._uiStateManager.deactivate === 'function') {
                    window._uiStateManager.deactivate();
                    console.log('Античит деактивирован при прерывании экзамена');
                }
                
                // Получаем ID экзамена
                const examId = document.getElementById('examId').value;
                // Отправляем запрос на сервер для сохранения прогресса
                fetch('/testing/abort', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': getCookie('X-XSRF-TOKEN')
                    },
                    body: JSON.stringify({
                        examId: examId
                    })
                })
                .then(response => {
                    if (response.ok) {
                        return response.json();
                    }
                    throw new Error('Ошибка при прерывании экзамена');
                })
                .then(data => {
                    // Перенаправляем на страницу результатов
                    window.location.href = data.redirectUrl || '/score-summary';
                })
                .catch(error => {
                    console.error('Ошибка:', error);
                    // В случае ошибки все равно перенаправляем
                    window.location.href = '/score-summary';
                });
            };
        }
    }
});

function getCookie(name) {
    const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
    if (match) return match[2];
    return "";
}

// Перехватываем событие beforeunload, чтобы остановить античит
window.addEventListener('beforeunload', function() {
    // Деактивируем античит при закрытии страницы
    if (window._uiStateManager && typeof window._uiStateManager.deactivate === 'function') {
        window._uiStateManager.deactivate();
        console.log('Античит деактивирован при закрытии страницы');
    }
}); 