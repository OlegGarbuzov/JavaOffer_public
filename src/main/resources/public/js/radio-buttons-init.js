/**
 * Скрипт инициализации радио-кнопок для страницы тестирования
 */

// Инициализация обработчиков через direct assignment
function setupRadioButtons() {
    console.log('Инициализация радио-кнопок');
    document.querySelectorAll('.answer-btn input[type="radio"]').forEach(radio => {
        // Проверяем, есть ли уже обработчик через атрибут onchange в HTML
        if (!radio.hasAttribute('onchange')) {
            radio.onchange = function() {
                if (typeof window.selectAnswer === 'function') {
                    window.selectAnswer(this);
                } else {
                    console.error('Функция selectAnswer не определена');
                }
            };
        }
    });
}

// Вызываем только после загрузки DOM для избежания дублирования
document.addEventListener('DOMContentLoaded', setupRadioButtons);

// Функция для переинициализации после AJAX-обновления вопроса
window.reinitRadioButtons = setupRadioButtons; 