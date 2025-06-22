// Приоритетная инициализация обработчиков событий
(function () {
    // Предварительная инициализация requestId
    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        initRequestId();
    } else {
        document.addEventListener('DOMContentLoaded', initRequestId, {once: true});
    }

    // Инициализация классов для ответов
    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        initAnswerButtons();
    } else {
        document.addEventListener('DOMContentLoaded', initAnswerButtons, {once: true});
    }

    // Инициализация античита для рейтингового режима
    document.addEventListener('DOMContentLoaded', function () {
        const examModeElement = document.getElementById('examMode');
        const examId = document.getElementById('examId');
        const taskId = document.getElementById('taskId');

        if (examModeElement && examModeElement.value === 'RATING' && examId && taskId) {
            if (window._uiMenuSet) {
                console.log('Инициализация системы UI-компонентов для экзамена...');
                // Не передаем init_token, позволяем модулю античита самому инициализировать heartbeat
                window._uiMenuSet.initHeader(examId.value, null, taskId.value);
            }
        }
    });

    // Создание глобального объекта для управления античитом
    window._uiStateManager = {
        // Деактивация античита
        deactivate: function () {
            if (window._uiMenuSet && typeof window._uiMenuSet.deactivateHeader === 'function') {
                window._uiMenuSet.deactivateHeader();
                console.log('Античит деактивирован');
                return true;
            }
            return false;
        }
    };

    // Глобальная функция для обновления ID вопроса в античите
    window.updateElementId = function (newQuestionId) {
        if (!newQuestionId) {
            console.log('updateElementId: ID вопроса не предоставлен');
            return false;
        }

        console.log('updateElementId: Запрос на обновление ID вопроса:', newQuestionId);

        // Используем safeUpdateQuestionId если доступен (для безопасной синхронизации с механизмом heartbeat)
        if (window._uiMenuSet && typeof window._uiMenuSet.safeUpdateQuestionId === 'function') {
            console.log('updateElementId: Используем safeUpdateQuestionId для безопасного обновления');
            return window._uiMenuSet.safeUpdateQuestionId(newQuestionId);
        }
        // Используем стандартный updateHeader как запасной вариант
        else if (window._uiMenuSet && typeof window._uiMenuSet.updateHeader === 'function') {
            console.log('updateElementId: Используем стандартный updateHeader');
            return window._uiMenuSet.updateHeader(newQuestionId);
        }
        return false;
    };

    function initRequestId() {
        var form = document.querySelector('#examForm');
        if (form && form.dataset.nextRequestId) {
            window.requestId = form.dataset.nextRequestId;
            console.log('Инициализация requestId из data-next-request-id:', window.requestId);
        }
    }

    function initAnswerButtons() {
        // Добавление классов к уже выбранным ответам
        const checked = document.querySelector('.answer-btn input[type="radio"]:checked');
        if (checked) {
            checked.closest('.answer-btn').classList.add('selected');
        }

        // Принудительное применение золотой рамки для рейтингового режима
        const ratingCard = document.querySelector('.exam-question-card.question-block-rating');
        if (ratingCard) {
            ratingCard.style.border = '5px solid darkgoldenrod';
            ratingCard.style.borderColor = 'darkgoldenrod';
            ratingCard.style.boxShadow = '0 0 20px rgba(184, 134, 11, 0.7)';

            // Добавляем обработчики событий для сохранения золотой рамки
            ratingCard.addEventListener('mouseover', function () {
                this.style.border = '5px solid darkgoldenrod';
                this.style.borderColor = 'darkgoldenrod';
                this.style.boxShadow = '0 0 25px rgba(184, 134, 11, 0.8)';
            });

            ratingCard.addEventListener('mouseout', function () {
                this.style.border = '5px solid darkgoldenrod';
                this.style.borderColor = 'darkgoldenrod';
                this.style.boxShadow = '0 0 20px rgba(184, 134, 11, 0.7)';
            });
        }
    }
})();

window.addEventListener('DOMContentLoaded', function () {
    var style = document.getElementById('instant-theme');
    if (style) style.remove();
});

// Переменная для отслеживания последнего времени вызова selectAnswer
window._lastSelectAnswerTime = 0;

window.selectAnswer = function (radio) {
    // Защита от слишком частых вызовов (минимум 300 мс между вызовами)
    const now = Date.now();
    if (now - window._lastSelectAnswerTime < 300) {
        console.log('Слишком частый вызов selectAnswer, пропускаем');
        return;
    }
    window._lastSelectAnswerTime = now;

    document.querySelectorAll('.answer-btn').forEach(btn => {
        btn.classList.remove('selected', 'correct', 'incorrect');
        const oldSpinner = btn.querySelector('.answer-spinner');
        if (oldSpinner) oldSpinner.remove();
    });
    const label = radio.closest('.answer-btn');
    label.classList.add('selected');

    label.style.position = '';
    let spinner = document.createElement('span');
    spinner.className = 'answer-spinner';
    spinner.style.display = 'inline-block';
    spinner.style.marginLeft = '12px';
    spinner.innerHTML = '<span class="spinner-border spinner-border-sm" style="color:#bbb;width:1em;height:1em;min-width:1em;min-height:1em;border-width:2px;" role="status" aria-hidden="true"></span>';
    label.appendChild(spinner);

    const examId = window.currentExamId || document.getElementById('examId').value;
    const selectedAnswer = radio.value;
    let requestId = null;

    // Получаем requestId из формы, если он не был установлен в window
    const form = document.querySelector('#examForm');
    if (window.requestId) {
        requestId = window.requestId;
        console.log('Используем requestId из window:', requestId);
    } else if (form && form.dataset.nextRequestId) {
        requestId = form.dataset.nextRequestId;
        window.requestId = requestId;
        console.log('Получен requestId из формы:', requestId);
    } else {
        console.warn('requestId не найден ни в window, ни в форме!');
    }

    const examRequest = {
        examId: examId,
        selectedAnswer: selectedAnswer,
        requestId: requestId
    };

    console.log('Отправляем запрос с данными:', JSON.stringify(examRequest));

    const xsrfToken = getCookie('X-XSRF-TOKEN');

    const allRadios = document.querySelectorAll('.answer-btn input[type="radio"]');
    allRadios.forEach(r => r.disabled = true);

    const nextBtn = document.getElementById('nextBtn');
    if (nextBtn) {
        nextBtn.disabled = true;
        nextBtn.className = (nextBtn._originalClass || nextBtn.className) + ' disabled';
        nextBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Загрузка...';
    }

    let timeoutId;
    const fetchWithTimeout = (url, options, timeout = 10000) => {
        console.log('[DEBUG] Вызов fetchWithTimeout для URL:', url);
        
        // Проверка текущего состояния fetch
        console.log('[DEBUG] Текущая функция fetch (хэш):', window.fetch.toString().slice(0, 50).replace(/\s+/g, ' '));
        console.log('[DEBUG] Оригинальная fetch есть в _s4t._om?', window._s4t && window._s4t._om && window._s4t._om.fetch ? 'Да' : 'Нет');
        
        if (window._s4t && window._s4t._om && window._s4t._om.fetch) {
            console.log('[DEBUG] _s4t._om.fetch === window.fetch?', window._s4t._om.fetch === window.fetch ? 'Да' : 'Нет');
        }
        
        return Promise.race([
            fetch(url, options),
            new Promise((_, reject) => {
                timeoutId = setTimeout(() => reject(new Error('Время ожидания ответа истекло')), timeout);
            })
        ]);
    };

    fetchWithTimeout('/testing/answerCheck', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': xsrfToken,
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify(examRequest)
    }, 10000)
        .then(response => {
            clearTimeout(timeoutId);
            if (!response.ok) {
                return response.text().then(text => {
                    let message = text;
                    try {
                        const json = JSON.parse(text);
                        message = json.message || json.error || text || 'Ошибка проверки ответа';
                    } catch (e) {
                        // text не JSON, оставляем как есть
                    }
                    throw new Error(message);
                });
            }
            return response.json();
        })
        .then(data => {
            console.log('Получен ответ от сервера:', data);
            window.requestId = data.requestId;
            console.log('Обновлен requestId:', window.requestId);

            const userChooseIsCorrect = data.userChooseIsCorrect;
            const correctAnswerId = data.id;
            const correctContent = data.content;
            const explanation = data.explanation;

            if (userChooseIsCorrect) {
                label.classList.add('correct');
            } else {
                label.classList.add('incorrect');
                const correctLabel = Array.from(document.querySelectorAll('.answer-btn input[type="radio"]')).find(r => r.value == correctAnswerId);
                if (correctLabel) {
                    correctLabel.closest('.answer-btn').classList.add('correct');
                }
            }

            const solutionBlock = document.getElementById('solutionBlock');
            const solutionContent = document.getElementById('solutionContent');
            const solutionExplanation = document.getElementById('solutionExplanation');
            const memeGif = document.getElementById('memeGif');
            const solutionTitle = document.getElementById('solutionTitle');
            const solutionEmoji = document.getElementById('solutionEmoji');

            solutionContent.textContent = correctContent || '';
            let explanationText = explanation || '';
            if (explanationText.length > 220) {
                solutionExplanation.textContent = explanationText.slice(0, 220) + '... ';
                if (!document.getElementById('showFullSolutionBtn')) {
                    const btn = document.createElement('button');
                    btn.id = 'showFullSolutionBtn';
                    btn.textContent = 'Показать полностью';
                    btn.className = 'btn btn-link p-0';
                    btn.onclick = function () {
                        window.showSolutionModal(explanationText);
                    };
                    solutionExplanation.appendChild(btn);
                }
            } else {
                solutionExplanation.textContent = explanationText;
                const btn = document.getElementById('showFullSolutionBtn');
                if (btn) btn.remove();
            }
            solutionBlock.style.display = 'flex';
            setTimeout(() => solutionBlock.classList.add('visible'), 10);
            if (userChooseIsCorrect) {
                const idx = Math.floor(Math.random() * window.goodGifs.length);
                const gifSrc = window.goodGifs[idx];
                memeGif.src = gifSrc;
                memeGif.style.display = 'block';
                solutionTitle.textContent = 'Верно';
                solutionEmoji.textContent = '💡🎉';
            } else {
                const idx = Math.floor(Math.random() * window.badGifs.length);
                const gifSrc = window.badGifs[idx];
                memeGif.src = gifSrc;
                memeGif.style.display = 'block';
                solutionTitle.textContent = 'Неверно';
                solutionEmoji.textContent = '💡🤨';
            }
            if (nextBtn) {
                nextBtn.style.display = 'block';
                nextBtn.disabled = false;
                nextBtn.className = nextBtn._originalClass || 'btn btn-primary w-auto mt-3';
                nextBtn.textContent = 'Следующий вопрос';
            }
            allRadios.forEach(r => r.disabled = true);
            if (spinner) spinner.remove();
        })
        .catch(error => {
            console.error('Ошибка при отправке запроса:', error);
            clearTimeout(timeoutId);
            showErrorNotification(error.message);
            allRadios.forEach(r => r.disabled = false);
            document.querySelectorAll('.answer-btn').forEach(btn => btn.classList.remove('selected', 'correct', 'incorrect'));
            allRadios.forEach(r => r.checked = false);
            if (spinner) spinner.remove();
            if (nextBtn) {
                nextBtn.disabled = false;
                nextBtn.className = nextBtn._originalClass || 'btn btn-primary w-auto mt-3';
                nextBtn.textContent = 'Следующий вопрос';
            }
        });
};
window.showSolutionModal = function (text) {
    var modal = document.getElementById('fullSolutionModal');
    if (!modal) return;
    document.getElementById('fullSolutionText').textContent = text;
    modal.style.display = 'flex';
    setTimeout(() => {
        modal.style.opacity = 1;
    }, 10);
};
window.closeSolutionModal = function () {
    var modal = document.getElementById('fullSolutionModal');
    if (!modal) return;
    modal.style.opacity = 0;
    setTimeout(() => {
        modal.style.display = 'none';
    }, 200);
};
if (document.getElementById('fullSolutionModal')) {
    document.getElementById('fullSolutionModal').addEventListener('mousedown', function (e) {
        if (e.target === this) window.closeSolutionModal();
    });
}
document.addEventListener('keydown', function (e) {
    var modal = document.getElementById('fullSolutionModal');
    if (e.key === 'Escape' && modal && modal.style.display === 'flex') {
        window.closeSolutionModal();
    }
});

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Переменная для отслеживания последнего времени вызова nextQuestion
window._lastNextQuestionTime = 0;

window.nextQuestion = function (buttonElement) {
    // Защита от слишком частых вызовов (минимум 500 мс между вызовами)
    const now = Date.now();
    if (now - window._lastNextQuestionTime < 500) {
        console.log('Слишком частый вызов nextQuestion, пропускаем');
        return;
    }
    window._lastNextQuestionTime = now;

    if (buttonElement) {
        buttonElement.disabled = true;
        buttonElement._originalClass = buttonElement.className;
        buttonElement.className = buttonElement._originalClass + ' disabled';
        buttonElement.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Загрузка...';
    }

    const selected = document.querySelector('input[name="selectedAnswerId"]:checked');
    if (!selected) {
        if (buttonElement) {
            buttonElement.disabled = false;
            buttonElement.className = buttonElement._originalClass || 'btn btn-primary w-auto mt-3';
            buttonElement.textContent = 'Следующий вопрос';
        }
        return;
    }

    const examId = window.currentExamId || document.getElementById('examId').value;

    let requestId = null;
    if (window.requestId) {
        requestId = window.requestId;
        console.log('nextQuestion: Используем requestId из window:', requestId);
    } else {
        const form = document.querySelector('#examForm');
        if (form && form.dataset.nextRequestId) {
            requestId = form.dataset.nextRequestId;
            window.requestId = requestId;
            console.log('nextQuestion: Получен requestId из формы:', requestId);
        } else {
            console.warn('nextQuestion: requestId не найден ни в window, ни в форме!');
        }
    }

    const examRequest = {
        examId: examId,
        selectedAnswer: selected.value,
        requestId: requestId
    };

    console.log('nextQuestion: Отправляем запрос с данными:', JSON.stringify(examRequest));

    const xsrfToken = getCookie('X-XSRF-TOKEN');

    const currentPath = window.location.pathname;
    let nextQuestionUrl = '/testing/nextQuestion';
    if (currentPath.includes('ratingExam')) {
        nextQuestionUrl = '/ratingExam/nextQuestion';
    }

    fetch(nextQuestionUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': xsrfToken,
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify(examRequest)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    let message = text;
                    try {
                        const json = JSON.parse(text);
                        message = json.message || json.error || text || 'Ошибка получения следующего вопроса';
                    } catch (e) {
                        // text не JSON, оставляем как есть
                    }
                    throw new Error(message);
                });
            }
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return response.json().then(data => {
                    // Проверяем, содержит ли JSON-ответ флаг examTerminated
                    if (data.examTerminatedByViolation === true) {
                        console.warn('nextQuestion: Получен флаг о прерывании экзамена (JSON)');

                        // Деактивируем античит, если он активирован
                        if (window._uiStateManager && typeof window._uiStateManager.deactivate === 'function') {
                            window._uiStateManager.deactivate();
                            console.log('Античит деактивирован при прерывании экзамена');
                        }

                        // Добавляем флаг в форму экзамена
                        const examForm = document.getElementById('examForm');
                        if (examForm) {
                            examForm.dataset.examTerminatedByViolation = 'true';
                        }

                        // Показываем модальное окно о прерывании экзамена
                        if (window._showTerminationModal && typeof window._showTerminationModal === 'function') {
                            window._showTerminationModal('Прогресс прерван из-за нарушений правил.');
                            console.log('Отображено модальное окно о прерывании экзамена');
                        } else {
                            showErrorNotification('Прогресс прерван из-за нарушений правил. Вы будете перенаправлены на страницу результатов.');
                            console.warn('Функция _showTerminationModal не найдена, используется стандартное уведомление');

                            // Через небольшую задержку перенаправляем на страницу результатов
                            setTimeout(function () {
                                window.location.href = '/modeSelect';
                            }, 5000);
                        }

                        // Возвращаем специальный флаг, который будет обработан в блоке then
                        return {_isExamTerminated: true, examId: examId};
                    }
                    
                    // Проверяем флаг превышения количества неверных ответов
                    if (data.examTerminatedByFailAnswerCount === true) {
                        console.warn('nextQuestion: Получен флаг о прерывании экзамена из-за превышения количества неверных ответов (JSON)');

                        // Деактивируем античит, если он активирован
                        if (window._uiStateManager && typeof window._uiStateManager.deactivate === 'function') {
                            window._uiStateManager.deactivate();
                            console.log('Античит деактивирован при прерывании экзамена');
                        }

                        // Добавляем флаг в форму экзамена
                        const examForm = document.getElementById('examForm');
                        if (examForm) {
                            examForm.dataset.examTerminatedByFailAnswerCount = 'true';
                        }

                        // Показываем модальное окно о прерывании экзамена из-за превышения количества неверных ответов
                        if (window._showFailAnswersLimitModal && typeof window._showFailAnswersLimitModal === 'function') {
                            window._showFailAnswersLimitModal(data.abortResults);
                            console.log('Отображено модальное окно о прерывании экзамена из-за превышения количества неверных ответов');
                        } else {
                            showErrorNotification('Тестирование прервано из-за превышения количества неверных ответов. Вы будете перенаправлены на страницу выбора режима.');
                            console.warn('Функция _showFailAnswersLimitModal не найдена, используется стандартное уведомление');

                            // Через небольшую задержку перенаправляем на страницу выбора режима
                            setTimeout(function () {
                                window.location.href = '/modeSelect';
                            }, 5000);
                        }

                        // Возвращаем специальный флаг, который будет обработан в блоке then
                        return {_isExamTerminated: true, examId: examId};
                    }

                    if (data.message) {
                        throw new Error(data.message);
                    }
                    return JSON.stringify(data);
                });
            } else {
                return response.text();
            }
        })
        .then(html => {
            // Проверяем, был ли это JSON с флагом прерывания
            if (html && typeof html === 'object' && html._isExamTerminated) {
                // Через небольшую задержку перенаправляем на страницу результатов
                setTimeout(function () {
                    window.location.href = '/modeSelect';
                }, 5000);
                return;
            }

            document.getElementById('question-block').innerHTML = html;
            const newProgressElement = document.querySelector('[data-next-request-id]');
            if (newProgressElement && newProgressElement.dataset.nextRequestId) {
                // Сохраняем текущий requestId перед обновлением
                const prevRequestId = window.requestId;

                // Обновляем requestId
                window.requestId = newProgressElement.dataset.nextRequestId;
                console.log('nextQuestion: Обновлен requestId из нового вопроса:', window.requestId,
                    'предыдущий:', prevRequestId);
            }

            // Проверяем, содержит ли HTML код флаг examTerminated
            if (html.includes('data-exam-terminated="true"')) {
                console.warn('nextQuestion: Получен флаг о прерывании экзамена (HTML)');

                // Деактивируем античит, если он активирован
                if (window._uiStateManager && typeof window._uiStateManager.deactivate === 'function') {
                    window._uiStateManager.deactivate();
                    console.log('Античит деактивирован при прерывании экзамена');
                }

                // Добавляем флаг в форму экзамена
                const examForm = document.getElementById('examForm');
                if (examForm) {
                    // Проверяем, является ли это прерыванием из-за превышения количества неверных ответов
                    // Для этого проверяем наличие скрытого поля с флагом examTerminatedByFailAnswerCount
                    const isFailAnswerCountExceeded = html.includes('data-exam-terminated-by-fail-answer-count="true"') || 
                                                     (examForm.dataset && examForm.dataset.examTerminatedByFailAnswerCount === 'true');
                    
                    console.log('nextQuestion: Проверка типа прерывания экзамена:', 
                                'isFailAnswerCountExceeded =', isFailAnswerCountExceeded);
                    
                    if (isFailAnswerCountExceeded) {
                        // Устанавливаем флаг прерывания из-за превышения количества неверных ответов
                        examForm.dataset.examTerminatedByFailAnswerCount = 'true';
                        
                        // Показываем модальное окно о прерывании экзамена из-за превышения количества неверных ответов
                        if (window._showFailAnswersLimitModal && typeof window._showFailAnswersLimitModal === 'function') {
                            // Пытаемся извлечь данные статистики из ответа
                            let abortResults = null;
                            try {
                                // Проверяем, есть ли в DOM отдельные поля с данными abortResults
                                const examMode = document.getElementById('abort-results-exam-mode');
                                const successCount = document.getElementById('abort-results-success-count');
                                const failCount = document.getElementById('abort-results-fail-count');
                                const time = document.getElementById('abort-results-time');
                                const score = document.getElementById('abort-results-score');
                                
                                if (examMode && successCount && failCount && time && score) {
                                    abortResults = {
                                        examMode: examMode.value,
                                        successAnswersCountAbsolute: parseInt(successCount.value) || 0,
                                        failAnswersCountAbsolute: parseInt(failCount.value) || 0,
                                        timeTakenToComplete: parseFloat(time.value) || 0,
                                        score: parseInt(score.value) || 0
                                    };
                                    console.log('Извлечены данные статистики:', abortResults);
                                }
                            } catch (e) {
                                console.error('Ошибка при извлечении данных abortResults:', e);
                            }
                            
                            window._showFailAnswersLimitModal(abortResults);
                            console.log('Отображено модальное окно о прерывании экзамена из-за превышения количества неверных ответов');
                        } else {
                            showErrorNotification('Тестирование прервано из-за превышения количества неверных ответов. Вы будете перенаправлены на страницу выбора режима.');
                            console.warn('Функция _showFailAnswersLimitModal не найдена, используется стандартное уведомление');
                            
                            // Через небольшую задержку перенаправляем на страницу выбора режима
                            setTimeout(function () {
                                window.location.href = '/modeSelect';
                            }, 5000);
                        }
                    } else {
                        // Это обычное прерывание из-за нарушений правил
                        examForm.dataset.examTerminatedByViolation = 'true';
                        
                        // Показываем модальное окно о прерывании экзамена
                        if (window._showTerminationModal && typeof window._showTerminationModal === 'function') {
                            window._showTerminationModal('Прогресс прерван из-за нарушений правил.');
                            console.log('Отображено модальное окно о прерывании экзамена');
                        } else {
                            showErrorNotification('Прогресс прерван из-за нарушений правил. Вы будете перенаправлены на страницу результатов.');
                            console.warn('Функция _showTerminationModal не найдена, используется стандартное уведомление');

                            // Через небольшую задержку перенаправляем на страницу результатов
                            setTimeout(function () {
                                window.location.href = '/modeSelect';
                            }, 5000);
                        }
                    }
                }

                return;
            }

            // Инициализируем снова обработчики после загрузки нового вопроса
            if (typeof window.reinitAnswerHandlers === 'function') {
                setTimeout(window.reinitAnswerHandlers, 50);
            }

            // Обновляем ID вопроса в мониторе с небольшой задержкой 
            // для корректной последовательности обновлений
            setTimeout(function () {
                try {
                    const newTaskId = document.getElementById('taskId');
                    if (newTaskId && newTaskId.value) {
                        console.log('nextQuestion: Обновляем ID вопроса в мониторе:', newTaskId.value);

                        if (window.updateElementId) {
                            // Используем безопасное обновление ID
                            window.updateElementId(newTaskId.value);
                        }
                    }
                } catch (e) {
                    console.error('nextQuestion: Ошибка при обновлении ID вопроса:', e);
                }
            }, 150); // Небольшая задержка для стабильности
        })
        .catch(error => {
            console.error('nextQuestion: Ошибка при отправке запроса:', error);
            const nextBtn = document.getElementById('nextBtn');
            if (nextBtn) {
                nextBtn.disabled = false;
                nextBtn.className = nextBtn._originalClass || 'btn btn-start mt-3';
                nextBtn.textContent = 'Следующий вопрос';
            }
            showErrorNotification(error.message);
        });
};

window.selectMode = function (mode) {
    document.getElementById('examModeInput').value = mode;
    document.getElementById('modeFree').classList.remove('active');
    document.getElementById('modeRating').classList.remove('active');
    if (mode === 'FREE') {
        document.getElementById('modeFree').classList.add('active');
    } else {
        document.getElementById('modeRating').classList.add('active');
    }
};

document.addEventListener('DOMContentLoaded', function () {
    const container = document.querySelector('.mode-select-container');
    const hint = document.querySelector('.swipe-hint');
    const modeFree = document.getElementById('modeFree');
    const modeRating = document.getElementById('modeRating');
    let touchStartX = 0;
    let touchEndX = 0;

    function showHint() {
        if (!hint) return;
        hint.style.display = 'flex';
        setTimeout(() => {
            hint.style.opacity = '0.75';
        }, 10);
    }

    if (hint) showHint();

    function centerFreeMode() {
        if (window.innerWidth <= 768 && container && modeFree) {
            setTimeout(() => {
                try {
                    if (window.innerWidth <= 600) {
                        container.scrollLeft = 0;
                    } else {
                        const firstCard = modeFree;

                        if (!firstCard) return;

                        const containerWidth = container.offsetWidth;
                        const cardWidth = firstCard.offsetWidth;

                        const offset = Math.max(0, (containerWidth - cardWidth) / 2);

                        container.scrollLeft = 0;
                    }
                } catch (e) {
                    console.error('Ошибка при центрировании:', e);
                }
            }, 50);
        }
    }

    const dots = document.querySelectorAll('.indicator-dot');
    dots.forEach((dot, index) => {
        dot.addEventListener('click', () => {
            const mode = index === 0 ? 'FREE' : 'RATING';
            selectMode(mode);
        });
    });

    if (container) {
        container.addEventListener('scroll', function () {
            if (container.scrollWidth <= container.clientWidth) return;

            const containerWidth = container.offsetWidth;
            const scrollLeft = container.scrollLeft;
            const scrollPosition = scrollLeft / (container.scrollWidth - containerWidth);

            if (scrollPosition < 0.5) {
                updateIndicatorsUI(0);
                if (document.getElementById('examModeInput').value !== 'FREE') {
                    document.getElementById('modeFree').classList.add('active');
                    document.getElementById('modeRating').classList.remove('active');
                    document.getElementById('examModeInput').value = 'FREE';
                }
            } else {
                updateIndicatorsUI(1);
                if (document.getElementById('examModeInput').value !== 'RATING') {
                    document.getElementById('modeFree').classList.remove('active');
                    document.getElementById('modeRating').classList.add('active');
                    document.getElementById('examModeInput').value = 'RATING';
                }
            }
        });

        container.addEventListener('touchstart', function (e) {
            touchStartX = e.touches[0].clientX;
        }, {passive: true});

        container.addEventListener('touchend', function (e) {
            touchEndX = e.changedTouches[0].clientX;
            handleSwipe();
        }, {passive: true});

        function handleSwipe() {
            const currentMode = document.getElementById('examModeInput').value;

            if (touchEndX < touchStartX - 50) {
                if (currentMode === 'FREE') {
                    selectMode('RATING');
                }
            } else if (touchEndX > touchStartX + 50) {
                if (currentMode === 'RATING') {
                    selectMode('FREE');
                }
            }
        }
    }

    if (container && modeFree) {
        centerFreeMode();
        setTimeout(centerFreeMode, 100);
        setTimeout(centerFreeMode, 300);
        setTimeout(centerFreeMode, 500);

        window.addEventListener('resize', () => {
            centerFreeMode();
        });
    }

    const modeForm = document.getElementById('modeForm');
    if (modeForm) {
        modeForm.addEventListener('submit', function (e) {
            const submitButton = this.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Загрузка...';
            }

            const selectedMode = document.getElementById('examModeInput').value;
            modeForm.action = '/testing/process';
            modeForm.method = 'post';
        });
    }
});

function updateIndicatorsUI(activeIndex) {
    const dots = document.querySelectorAll('.indicator-dot');
    dots.forEach((dot, index) => {
        if (index === activeIndex) {
            dot.classList.add('active');
        } else {
            dot.classList.remove('active');
        }
    });
}

function showErrorNotification(message, type = 'error') {
    // Удаляем предыдущее уведомление, если оно есть
    const existing = document.getElementById('exam-toast-notification');
    if (existing) existing.remove();

    // Создаем новое уведомление с уникальным ID и классом
    const notification = document.createElement('div');
    notification.id = 'exam-toast-notification';
    notification.className = type === 'success' ? 'exam-toast-success' : 'exam-toast-error';

    // Применяем инлайновые стили для гарантированного позиционирования
    // Используем z-index: 2147483647 (максимальное значение), чтобы перебить все другие стили
    const styles = {
        position: 'fixed',
        bottom: '24px',
        right: '24px',
        minWidth: '280px',
        maxWidth: '360px',
        padding: '16px 20px',
        borderRadius: '8px',
        boxShadow: '0 4px 24px rgba(0,0,0,0.18)',
        zIndex: '2147483647', // Максимальное значение z-index
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        display: 'flex',
        alignItems: 'center',
        opacity: '0',
        transform: 'translateY(20px)',
        transition: 'opacity 0.3s, transform 0.3s',
        backgroundColor: type === 'success' ? '#e6f9ed' : '#fae9eb',
        color: type === 'success' ? '#207a3c' : '#d9304e'
    };

    // Применяем стили напрямую к элементу
    Object.keys(styles).forEach(key => {
        notification.style[key] = styles[key];
    });

    // Создаем кнопку закрытия
    const closeBtn = document.createElement('button');
    closeBtn.setAttribute('aria-label', 'Закрыть');
    Object.assign(closeBtn.style, {
        position: 'absolute',
        top: '10px',
        right: '12px',
        width: '24px',
        height: '24px',
        background: 'none',
        border: 'none',
        fontSize: '22px',
        color: 'inherit',
        cursor: 'pointer',
        opacity: '0.7',
        zIndex: '1'
    });
    closeBtn.textContent = '×';
    closeBtn.onclick = closeErrorNotification;

    // При наведении делаем кнопку более заметной
    closeBtn.onmouseover = () => {
        closeBtn.style.opacity = '1';
    };
    closeBtn.onmouseout = () => {
        closeBtn.style.opacity = '0.7';
    };

    // Создаем контейнер для содержимого
    const content = document.createElement('div');
    Object.assign(content.style, {
        flex: '1 1 auto',
        paddingRight: '32px'
    });

    // Создаем заголовок уведомления
    const title = document.createElement('div');
    Object.assign(title.style, {
        fontWeight: '600',
        fontSize: '16px',
        marginBottom: '4px'
    });
    title.textContent = type === 'success' ? 'Успешно' : 'Ошибка';

    // Создаем текст уведомления
    const messageText = document.createElement('div');
    Object.assign(messageText.style, {
        fontSize: '14px',
        lineHeight: '1.5'
    });
    messageText.textContent = message;

    // Собираем элементы вместе
    content.appendChild(title);
    content.appendChild(messageText);
    notification.appendChild(closeBtn);
    notification.appendChild(content);

    // Добавляем уведомление в DOM
    document.body.appendChild(notification);

    // Показываем уведомление с анимацией
    setTimeout(() => {
        notification.style.opacity = '1';
        notification.style.transform = 'translateY(0)';
    }, 10);

    // Автоматически скрываем через 5 секунд
    setTimeout(closeErrorNotification, 5000);
}

function closeErrorNotification() {
    const notification = document.getElementById('exam-toast-notification');
    if (notification) {
        notification.style.opacity = '0';
        notification.style.transform = 'translateY(20px)';
        setTimeout(() => {
            if (notification && notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }
}

document.addEventListener('click', function (e) {
    if (!e.target || typeof e.target.closest !== 'function') return;
    const target = e.target.nodeType === 1 ? e.target : e.target.parentElement;
    const suggestBtn = target && target.closest('#suggestCorrectionBtn');
    if (!suggestBtn || suggestBtn.classList.contains('disabled')) return;
    const modal = document.getElementById('suggestCorrectionModal');
    const textarea = document.getElementById('suggestCorrectionText');
    if (modal && textarea) {
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.style.opacity = 1;
        }, 10);
        textarea.value = '';
        textarea.focus();
    }
});

document.addEventListener('mouseover', function (e) {
    if (!e.target || typeof e.target.closest !== 'function') return;
    const btn = e.target.closest('#suggestCorrectionBtn.disabled');
    if (btn) {
        btn.setAttribute('data-tooltip', 'Войдите, что бы предложить исправление');
        btn.style.position = 'relative';
        let tooltip = btn.querySelector('.custom-tooltip');
        if (!tooltip) {
            tooltip = document.createElement('div');
            tooltip.className = 'custom-tooltip';
            tooltip.textContent = 'Войдите, что бы предложить исправление';
            tooltip.style.position = 'absolute';
            tooltip.style.top = '110%';
            tooltip.style.left = '0';
            tooltip.style.background = '#222';
            tooltip.style.color = '#fff';
            tooltip.style.padding = '4px 10px';
            tooltip.style.borderRadius = '6px';
            tooltip.style.fontSize = '0.95em';
            tooltip.style.whiteSpace = 'nowrap';
            tooltip.style.zIndex = '999';
            tooltip.style.opacity = '0.95';
            btn.appendChild(tooltip);
        }
    }
});
document.addEventListener('mouseout', function (e) {
    if (!e.target || typeof e.target.closest !== 'function') return;
    const btn = e.target.closest('#suggestCorrectionBtn.disabled');
    if (btn) {
        const tooltip = btn.querySelector('.custom-tooltip');
        if (tooltip) tooltip.remove();
    }
});

document.addEventListener('DOMContentLoaded', function () {
    const modal = document.getElementById('suggestCorrectionModal');
    const closeBtn = document.getElementById('closeSuggestCorrectionModal');
    const sendBtn = document.getElementById('sendSuggestCorrectionBtn');
    const textarea = document.getElementById('suggestCorrectionText');

    if (modal && closeBtn && sendBtn && textarea) {
        document.addEventListener('mousedown', function (e) {
            if (!e.target || typeof e.target.closest !== 'function') return;
            if (e.target.closest('#suggestCorrectionBtn')) {
                e.target.closest('#suggestCorrectionBtn').style.transform = 'scale(0.96)';
            }
        });
        document.addEventListener('mouseup', function (e) {
            if (!e.target || typeof e.target.closest !== 'function') return;
            if (e.target.closest('#suggestCorrectionBtn')) {
                e.target.closest('#suggestCorrectionBtn').style.transform = '';
            }
        });
        document.addEventListener('mouseleave', function (e) {
            if (!e.target || typeof e.target.closest !== 'function') return;
            if (e.target.closest('#suggestCorrectionBtn')) {
                e.target.closest('#suggestCorrectionBtn').style.transform = '';
            }
        });
        closeBtn.addEventListener('click', function () {
            modal.style.opacity = 0;
            setTimeout(() => {
                modal.style.display = 'none';
            }, 200);
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && modal.style.display === 'flex') {
                modal.style.opacity = 0;
                setTimeout(() => {
                    modal.style.display = 'none';
                }, 200);
            }
        });
        modal.addEventListener('mousedown', function (e) {
            if (e.target === modal) {
                modal.style.opacity = 0;
                setTimeout(() => {
                    modal.style.display = 'none';
                }, 200);
            }
        });
        sendBtn.addEventListener('click', function () {
            const text = textarea.value.trim();
            if (!text) {
                showErrorNotification('Пожалуйста, заполните поле для предложения.', 'error');
                return;
            }
            sendBtn.disabled = true;
            sendBtn.textContent = 'Отправка...';

            let taskId = null;
            const taskIdInput = document.getElementById('taskId');
            if (taskIdInput && taskIdInput.value) {
                taskId = taskIdInput.value;
            }

            if (!taskId) {
                showErrorNotification('Не удалось определить ID вопроса.', 'error');
                sendBtn.disabled = false;
                sendBtn.textContent = 'Отправить';
                return;
            }

            const xsrfToken = getCookie('X-XSRF-TOKEN');
            const message = `TaskId: ${taskId}. Сообщение: ${text}`;
            fetch('/feedBack', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': xsrfToken,
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({text: message})
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Ошибка сервера при отправке предложения.');
                    }
                    modal.style.opacity = 0;
                    setTimeout(() => {
                        modal.style.display = 'none';
                    }, 200);
                    showErrorNotification('Спасибо! Ваше предложение отправлено.', 'success');
                })
                .catch(() => {
                    showErrorNotification('Ошибка при отправке предложения.', 'error');
                })
                .finally(() => {
                    sendBtn.disabled = false;
                    sendBtn.textContent = 'Отправить';
                });
        });
    }
});

document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('feedbackForm');
    const sendBtn = document.getElementById('sendBtn');
    const feedbackText = document.getElementById('feedbackText');
    const successAlert = document.getElementById('feedbackSuccess');
    const errorAlert = document.getElementById('feedbackError');
    if (form && sendBtn && feedbackText && successAlert && errorAlert) {
        form.addEventListener('submit', function () {
            const text = feedbackText.value.trim();
            if (!text) return;
            const xsrfToken = getCookie('X-XSRF-TOKEN');
            fetch('/feedBack', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-XSRF-TOKEN': xsrfToken,
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({text})
            })
                .then(response => {
                    if (response.ok) {
                        successAlert.classList.remove('d-none');
                        errorAlert.classList.add('d-none');
                        feedbackText.value = '';
                    } else {
                        throw new Error('Ошибка');
                    }
                })
                .catch(() => {
                    errorAlert.classList.remove('d-none');
                    successAlert.classList.add('d-none');
                });
        });
    }
});

window.abortExam = function () {
    try {
        // Деактивируем модуль безопасности перед отправкой запроса
        if (window._uiMenuSet) {
            console.log('Деактивация модуля безопасности при завершении экзамена');

            // Останавливаем все процессы безопасности
            if (typeof window._uiMenuSet.deactivateHeader === 'function') {
                window._uiMenuSet.deactivateHeader();
            }

            // Очищаем все интервалы и таймеры, которые могли быть созданы
            // Обходной путь: мы не знаем какие точно интервалы используются скриптом безопасности,
            // поэтому очищаем все интервалы и таймеры
            const maxId = setTimeout(() => {
            }, 0);
            for (let i = 1; i <= maxId; i++) {
                clearTimeout(i);
                clearInterval(i);
            }

            // Также принудительно удаляем обработчики событий через UI-Insight Security Module
            if (window._s9xI) {
                window._s9xI = false;
            }

            // Устанавливаем флаг остановки в объекте _s4t
            if (window._s4t) {
                window._s4t._sp = true; // остановлено
                window._s4t._ad = true; // деактивировано
                window._s4t._av = false; // не активно
                console.log('Флаги безопасности установлены в остановленное состояние');
            }
        }
    } catch (securityError) {
        console.error('Ошибка при деактивации модуля безопасности:', securityError);
    }

    // Устанавливаем флаг завершения экзамена
    const examForm = document.getElementById('examForm');
    if (examForm) {
        examForm.dataset.examTerminatedByViolation = 'true';
        console.log('Установлен флаг завершения экзамена');
    }

    const examId = window.currentExamId || document.getElementById('examId').value;
    const examMode = document.getElementById('examMode') ? document.getElementById('examMode').value : null;
    const xsrfToken = getCookie('X-XSRF-TOKEN');

    const examRequest = {
        examId: examId,
        examMode: examMode
    };

    console.log('Отправка запроса на завершение экзамена:', examRequest);

    // Показываем индикатор загрузки
    const abortBtn = document.getElementById('abortExamBtn');
    // Сохраняем оригинальный текст кнопки за пределами блока условия,
    // чтобы он был доступен в блоке catch
    let originalText = '';
    if (abortBtn) {
        abortBtn.disabled = true;
        originalText = abortBtn.innerHTML;
        abortBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Загрузка...';
    }

    fetch('/testing/abort', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': xsrfToken,
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify(examRequest),
        credentials: 'same-origin'
    })
        .then(response => {
            // Сохраняем статус ответа для дальнейшей обработки
            const statusCode = response.status;
            if (!response.ok) {
                return response.text().then(text => {
                    let message = text;
                    try {
                        const json = JSON.parse(text);
                        message = json.message || json.error || text || 'Ошибка завершения экзамена';
                    } catch (e) {
                        // text не JSON, оставляем как есть
                    }
                    // Создаем объект ошибки с дополнительным полем statusCode
                    const error = new Error(message);
                    error.statusCode = statusCode;
                    throw error;
                });
            }
            return response.text();
        })
        .then(html => {
            console.log('Экзамен успешно завершен, загружаем страницу с результатами');
            const container = document.querySelector('.exam-question-card').parentNode;
            container.innerHTML = html;
        })
        .catch(error => {
            console.error('Ошибка при завершении экзамена:', error);

            // Восстанавливаем кнопку
            if (abortBtn) {
                abortBtn.disabled = false;
                abortBtn.innerHTML = originalText || 'Сохранить и завершить';
            }

            // Показываем уведомление об ошибке
            showErrorNotification(error.message || error.toString());
            
            // Если это ошибка 410 (Gone) - сессия не найдена
            if (error.statusCode === 410) {
                console.log('Сессия не найдена, экзамен не может быть завершен');
            }
        });
};

window.reinitAnswerHandlers = function () {
    // Инициализируем стили и классы
    if (typeof initAnswerButtons === 'function') {
        initAnswerButtons();
    }

    // Переинициализируем обработчики радио-кнопок, если доступна функция
    if (typeof window.reinitRadioButtons === 'function') {
        window.reinitRadioButtons();
    }
};

window.addEventListener('load', function () {
    setTimeout(function () {
        if (typeof initAnswerButtons === 'function') {
            initAnswerButtons();
        }
    }, 0);
});

function showToastNotify(message, type = 'error') {
    const old = document.getElementById('toast-notify');
    if (old) old.remove();

    const toast = document.createElement('div');
    toast.id = 'toast-notify';
    toast.className = `toast-notify-box toast-${type}`;
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(30px)';
    toast.style.position = 'fixed'; // на всякий случай

    const closeBtn = document.createElement('button');
    closeBtn.className = 'toast-close-btn';
    closeBtn.textContent = '×';
    closeBtn.setAttribute('aria-label', 'Закрыть');
    closeBtn.onclick = () => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(30px)';
        setTimeout(() => toast.remove(), 300);
    };

    const content = document.createElement('div');
    content.className = 'toast-content';
    const title = document.createElement('div');
    title.className = 'toast-title';
    title.textContent = type === 'success' ? 'Успех' : 'Ошибка';
    const msg = document.createElement('div');
    msg.className = 'toast-message';
    msg.textContent = message;
    content.appendChild(title);
    content.appendChild(msg);

    toast.appendChild(closeBtn);
    toast.appendChild(content);
    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0)';
    }, 10);
    setTimeout(() => {
        if (toast.parentNode) {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(30px)';
            setTimeout(() => toast.remove(), 300);
        }
    }, 5000);
}

function closeCustomNotification() {
    const notification = document.getElementById('js-notification-popup');
    if (notification) {
        notification.style.opacity = '0';
        notification.style.transform = 'translateY(20px)';
        setTimeout(() => {
            if (notification && notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }
}

// Проверяем, что находимся в рейтинговом режиме перед вызовом функции обновления токена античита
if (window.refreshAntiCheatToken && typeof window.refreshAntiCheatToken === 'function') {
    const examMode = document.getElementById('examMode')?.value;
    if (examMode === 'RATING') {
        window.refreshAntiCheatToken();
    } else {
        console.log('FREE режим - античит не используется');
    }
}

// Функция для отображения модального окна о превышении количества неверных ответов
window._showFailAnswersLimitModal = function(abortResults) {
    try {
        console.log('Отображение модального окна о превышении количества неверных ответов:', abortResults);

        // Проверяем, не отображалось ли уже модальное окно
        if (document.querySelector('.fail-limit-modal-overlay')) {
            console.log('Модальное окно уже отображается, пропускаем создание дубликата');
            return;
        }

        // Определяем, используется ли темная тема
        const isDarkTheme = document.documentElement.classList.contains('dark-theme') ||
            document.body.classList.contains('dark-theme') ||
            document.documentElement.getAttribute('data-theme') === 'dark' ||
            document.body.getAttribute('data-theme') === 'dark' ||
            window.matchMedia('(prefers-color-scheme: dark)').matches;

        // Создаем затемнение (оверлей)
        const overlay = document.createElement('div');
        overlay.className = 'fail-limit-modal-overlay';
        overlay.id = 'fail-limit-modal-overlay';
        overlay.style.position = 'fixed';
        overlay.style.top = '0';
        overlay.style.left = '0';
        overlay.style.width = '100%';
        overlay.style.height = '100%';
        overlay.style.backgroundColor = 'rgba(0, 0, 0, 0)';
        overlay.style.backdropFilter = 'blur(0px)';
        overlay.style.WebkitBackdropFilter = 'blur(0px)';
        overlay.style.display = 'flex';
        overlay.style.justifyContent = 'center';
        overlay.style.alignItems = 'center';
        overlay.style.zIndex = '999999';
        overlay.style.transition = 'all 0.3s ease-in-out';

        // Создаем модальное окно с желтой рамкой и желтым свечением
        const modal = document.createElement('div');
        modal.className = 'fail-limit-modal-content';
        modal.setAttribute('style', `
            background-color: ${isDarkTheme ? 'rgba(30, 30, 30, 0.95)' : 'rgba(255, 255, 255, 0.95)'};
            color: ${isDarkTheme ? '#ffffff' : '#000000'};
            backdrop-filter: blur(10px);
            -webkit-backdrop-filter: blur(10px);
            border-radius: 10px;
            padding: 30px;
            max-width: 500px;
            width: 80%;
            text-align: center;
            box-shadow: 0 0 30px rgba(255, 193, 7, 0.7) !important;
            border: 2px solid #ffc107 !important;
            transform: scale(0.8);
            opacity: 0;
            transition: all 0.3s ease-in-out;
        `);

        // Иконка предупреждения
        const warningIcon = document.createElement('div');
        warningIcon.style.fontSize = '4rem';
        warningIcon.style.marginBottom = '1rem';
        warningIcon.innerHTML = '⚠️';
        warningIcon.style.color = isDarkTheme ? '#FFD700' : '#8B6914';

        // Заголовок - темно-желтый
        const title = document.createElement('h2');
        title.textContent = 'Тестирование остановлено';
        title.style.color = '#b8860b'; // темно-желтый цвет (darkgoldenrod)
        title.style.marginBottom = '20px';
        title.style.fontWeight = 'bold';
        title.style.fontSize = '24px';

        // Сообщение о причине
        const errorMessage = document.createElement('p');
        errorMessage.textContent = 'Тестирование прервано из-за превышения количества неверных ответов';
        errorMessage.style.color = isDarkTheme ? '#ffe082' : '#856404';
        errorMessage.style.fontSize = '18px';
        errorMessage.style.marginBottom = '25px';

        // Создаем блок со статистикой в стиле resultExam.html
        const statsBlock = document.createElement('div');
        statsBlock.className = 'stats-block';
        statsBlock.style.width = '100%';
        statsBlock.style.marginBottom = '20px';

        const statsRow = document.createElement('div');
        statsRow.className = 'stats-row';
        statsRow.style.display = 'flex';
        statsRow.style.flexDirection = 'column';
        statsRow.style.alignItems = 'center';
        statsRow.style.justifyContent = 'center';
        statsRow.style.gap = '0.7rem';
        statsRow.style.margin = '0 auto';
        statsRow.style.width = '100%';
        statsRow.style.maxWidth = '300px';

        // Добавляем статистику, если она доступна
        if (abortResults) {
            // Режим экзамена
            const examModeItem = document.createElement('div');
            examModeItem.className = 'stat-item exam-mode-display';
            examModeItem.classList.add(abortResults.examMode === 'RATING' ? 'mode-rating' : 'mode-free');
            examModeItem.style.display = 'flex';
            examModeItem.style.justifyContent = 'space-between';
            examModeItem.style.alignItems = 'center';
            examModeItem.style.width = '100%';
            examModeItem.style.maxWidth = '300px';
            examModeItem.style.margin = '0 auto';
            examModeItem.style.textAlign = 'left';
            examModeItem.style.backgroundColor = isDarkTheme ? 'rgba(255, 255, 255, 0.1)' : 'rgba(240, 240, 240, 0.7)';
            examModeItem.style.borderRadius = '8px';
            examModeItem.style.padding = '0.5rem 1rem';
            
            // Для RATING режима добавляем золотистый стиль
            if (abortResults.examMode === 'RATING') {
                if (isDarkTheme) {
                    examModeItem.style.backgroundColor = 'rgba(255, 215, 0, 0.15)';
                    examModeItem.style.border = '1px solid rgba(255, 215, 0, 0.3)';
                } else {
                    examModeItem.style.backgroundColor = 'rgba(184, 134, 11, 0.15)';
                    examModeItem.style.border = '1px solid rgba(184, 134, 11, 0.3)';
                }
            }
            
            const examModeIconSpan = document.createElement('span');
            examModeIconSpan.className = 'stat-icon';
            examModeIconSpan.textContent = '🎓';
            examModeIconSpan.style.fontSize = '1.2rem';
            examModeIconSpan.style.marginRight = '0.5rem';
            
            const examModeValueSpan = document.createElement('span');
            examModeValueSpan.className = 'stat-value';
            examModeValueSpan.textContent = abortResults.examMode || 'Неизвестно';
            examModeValueSpan.style.marginLeft = 'auto';
            examModeValueSpan.style.textAlign = 'right';
            examModeValueSpan.style.fontWeight = '500';
            
            // Для RATING режима добавляем золотистый цвет текста
            if (abortResults.examMode === 'RATING') {
                examModeValueSpan.style.color = isDarkTheme ? '#FFD700' : '#8B6914';
                examModeValueSpan.style.fontWeight = 'bold';
            }
            
            examModeItem.appendChild(examModeIconSpan);
            examModeItem.appendChild(examModeValueSpan);
            statsRow.appendChild(examModeItem);

            // Всего вопросов
            const totalQuestionsItem = document.createElement('div');
            totalQuestionsItem.className = 'stat-item';
            totalQuestionsItem.style.display = 'flex';
            totalQuestionsItem.style.justifyContent = 'space-between';
            totalQuestionsItem.style.alignItems = 'center';
            totalQuestionsItem.style.width = '100%';
            totalQuestionsItem.style.maxWidth = '300px';
            totalQuestionsItem.style.margin = '0 auto';
            totalQuestionsItem.style.textAlign = 'left';
            totalQuestionsItem.style.backgroundColor = isDarkTheme ? 'rgba(255, 255, 255, 0.1)' : 'rgba(240, 240, 240, 0.7)';
            totalQuestionsItem.style.borderRadius = '8px';
            totalQuestionsItem.style.padding = '0.5rem 1rem';
            
            const totalQuestionsIconSpan = document.createElement('span');
            totalQuestionsIconSpan.className = 'stat-icon';
            totalQuestionsIconSpan.textContent = '❔';
            totalQuestionsIconSpan.style.fontSize = '1.2rem';
            totalQuestionsIconSpan.style.marginRight = '0.5rem';
            
            const totalQuestionsTextSpan = document.createElement('span');
            totalQuestionsTextSpan.textContent = 'Вопросов:';
            
            const totalQuestionsValueSpan = document.createElement('span');
            totalQuestionsValueSpan.className = 'stat-value';
            const totalQuestions = (abortResults.successAnswersCountAbsolute || 0) + (abortResults.failAnswersCountAbsolute || 0);
            totalQuestionsValueSpan.textContent = totalQuestions;
            totalQuestionsValueSpan.style.marginLeft = 'auto';
            totalQuestionsValueSpan.style.textAlign = 'right';
            totalQuestionsValueSpan.style.fontWeight = '500';
            
            const totalQuestionsLeftGroup = document.createElement('span');
            totalQuestionsLeftGroup.style.display = 'flex';
            totalQuestionsLeftGroup.style.alignItems = 'center';
            totalQuestionsLeftGroup.appendChild(totalQuestionsIconSpan);
            totalQuestionsLeftGroup.appendChild(totalQuestionsTextSpan);
            
            totalQuestionsItem.appendChild(totalQuestionsLeftGroup);
            totalQuestionsItem.appendChild(totalQuestionsValueSpan);
            statsRow.appendChild(totalQuestionsItem);

            // Правильные ответы
            const correctAnswersItem = document.createElement('div');
            correctAnswersItem.className = 'stat-item';
            correctAnswersItem.style.display = 'flex';
            correctAnswersItem.style.justifyContent = 'space-between';
            correctAnswersItem.style.alignItems = 'center';
            correctAnswersItem.style.width = '100%';
            correctAnswersItem.style.maxWidth = '300px';
            correctAnswersItem.style.margin = '0 auto';
            correctAnswersItem.style.textAlign = 'left';
            correctAnswersItem.style.backgroundColor = isDarkTheme ? 'rgba(255, 255, 255, 0.1)' : 'rgba(240, 240, 240, 0.7)';
            correctAnswersItem.style.borderRadius = '8px';
            correctAnswersItem.style.padding = '0.5rem 1rem';
            
            const correctAnswersIconSpan = document.createElement('span');
            correctAnswersIconSpan.className = 'stat-icon';
            correctAnswersIconSpan.textContent = '✅';
            correctAnswersIconSpan.style.fontSize = '1.2rem';
            correctAnswersIconSpan.style.marginRight = '0.5rem';
            correctAnswersIconSpan.style.color = '#28a745';
            
            const correctAnswersTextSpan = document.createElement('span');
            correctAnswersTextSpan.textContent = 'Верных ответов:';
            
            const correctAnswersValueSpan = document.createElement('span');
            correctAnswersValueSpan.className = 'stat-value';
            correctAnswersValueSpan.textContent = abortResults.successAnswersCountAbsolute || 0;
            correctAnswersValueSpan.style.marginLeft = 'auto';
            correctAnswersValueSpan.style.textAlign = 'right';
            correctAnswersValueSpan.style.fontWeight = '500';
            
            const correctAnswersLeftGroup = document.createElement('span');
            correctAnswersLeftGroup.style.display = 'flex';
            correctAnswersLeftGroup.style.alignItems = 'center';
            correctAnswersLeftGroup.appendChild(correctAnswersIconSpan);
            correctAnswersLeftGroup.appendChild(correctAnswersTextSpan);
            
            correctAnswersItem.appendChild(correctAnswersLeftGroup);
            correctAnswersItem.appendChild(correctAnswersValueSpan);
            statsRow.appendChild(correctAnswersItem);

            // Неправильные ответы
            const incorrectAnswersItem = document.createElement('div');
            incorrectAnswersItem.className = 'stat-item';
            incorrectAnswersItem.style.display = 'flex';
            incorrectAnswersItem.style.justifyContent = 'space-between';
            incorrectAnswersItem.style.alignItems = 'center';
            incorrectAnswersItem.style.width = '100%';
            incorrectAnswersItem.style.maxWidth = '300px';
            incorrectAnswersItem.style.margin = '0 auto';
            incorrectAnswersItem.style.textAlign = 'left';
            incorrectAnswersItem.style.backgroundColor = isDarkTheme ? 'rgba(255, 255, 255, 0.1)' : 'rgba(240, 240, 240, 0.7)';
            incorrectAnswersItem.style.borderRadius = '8px';
            incorrectAnswersItem.style.padding = '0.5rem 1rem';
            
            const incorrectAnswersIconSpan = document.createElement('span');
            incorrectAnswersIconSpan.className = 'stat-icon';
            incorrectAnswersIconSpan.textContent = '❌';
            incorrectAnswersIconSpan.style.fontSize = '1.2rem';
            incorrectAnswersIconSpan.style.marginRight = '0.5rem';
            incorrectAnswersIconSpan.style.color = '#dc3545';
            
            const incorrectAnswersTextSpan = document.createElement('span');
            incorrectAnswersTextSpan.textContent = 'Неверных ответов:';
            
            const incorrectAnswersValueSpan = document.createElement('span');
            incorrectAnswersValueSpan.className = 'stat-value';
            incorrectAnswersValueSpan.textContent = abortResults.failAnswersCountAbsolute || 0;
            incorrectAnswersValueSpan.style.marginLeft = 'auto';
            incorrectAnswersValueSpan.style.textAlign = 'right';
            incorrectAnswersValueSpan.style.fontWeight = '500';
            
            const incorrectAnswersLeftGroup = document.createElement('span');
            incorrectAnswersLeftGroup.style.display = 'flex';
            incorrectAnswersLeftGroup.style.alignItems = 'center';
            incorrectAnswersLeftGroup.appendChild(incorrectAnswersIconSpan);
            incorrectAnswersLeftGroup.appendChild(incorrectAnswersTextSpan);
            
            incorrectAnswersItem.appendChild(incorrectAnswersLeftGroup);
            incorrectAnswersItem.appendChild(incorrectAnswersValueSpan);
            statsRow.appendChild(incorrectAnswersItem);

            // Время выполнения
            const timeItem = document.createElement('div');
            timeItem.className = 'stat-item';
            timeItem.style.display = 'flex';
            timeItem.style.justifyContent = 'space-between';
            timeItem.style.alignItems = 'center';
            timeItem.style.width = '100%';
            timeItem.style.maxWidth = '300px';
            timeItem.style.margin = '0 auto';
            timeItem.style.textAlign = 'left';
            timeItem.style.backgroundColor = isDarkTheme ? 'rgba(255, 255, 255, 0.1)' : 'rgba(240, 240, 240, 0.7)';
            timeItem.style.borderRadius = '8px';
            timeItem.style.padding = '0.5rem 1rem';
            
            const timeIconSpan = document.createElement('span');
            timeIconSpan.className = 'stat-icon';
            timeIconSpan.textContent = '⏳';
            timeIconSpan.style.fontSize = '1.2rem';
            timeIconSpan.style.marginRight = '0.5rem';
            
            const timeTextSpan = document.createElement('span');
            timeTextSpan.textContent = 'Затрачено времени:';
            
            const timeValueSpan = document.createElement('span');
            timeValueSpan.className = 'stat-value';
            
            // Форматирование времени в формате ЧЧ:ММ:СС
            const formatTime = (seconds) => {
                const hours = Math.floor(seconds / 3600);
                const minutes = Math.floor((seconds % 3600) / 60);
                const secs = Math.floor(seconds % 60);
                return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
            };
            
            timeValueSpan.textContent = abortResults.timeTakenToComplete ? formatTime(abortResults.timeTakenToComplete) : '0:00:00';
            timeValueSpan.style.marginLeft = 'auto';
            timeValueSpan.style.textAlign = 'right';
            timeValueSpan.style.fontWeight = '500';
            
            const timeLeftGroup = document.createElement('span');
            timeLeftGroup.style.display = 'flex';
            timeLeftGroup.style.alignItems = 'center';
            timeLeftGroup.appendChild(timeIconSpan);
            timeLeftGroup.appendChild(timeTextSpan);
            
            timeItem.appendChild(timeLeftGroup);
            timeItem.appendChild(timeValueSpan);
            statsRow.appendChild(timeItem);

            // Добавляем блок статистики в модальное окно
            statsBlock.appendChild(statsRow);
            
            // Итоговый счет - только желтая звездочка и цифра
            if (abortResults.score) {
                const scoreContainer = document.createElement('div');
                scoreContainer.className = 'score-container';
                scoreContainer.style.marginTop = '2.2rem';
                scoreContainer.style.textAlign = 'center';
                scoreContainer.style.width = '100%';
                
                const scoreDisplay = document.createElement('div');
                scoreDisplay.className = 'score-display';
                scoreDisplay.style.fontSize = '2.5rem';
                scoreDisplay.style.fontWeight = 'bold';
                scoreDisplay.style.margin = '1rem auto';
                scoreDisplay.style.padding = '1rem';
                scoreDisplay.style.display = 'flex';
                scoreDisplay.style.alignItems = 'center';
                scoreDisplay.style.justifyContent = 'center';
                scoreDisplay.style.borderRadius = '12px';
                scoreDisplay.style.minWidth = '120px';
                scoreDisplay.style.height = '120px';
                scoreDisplay.style.width = 'auto';
                scoreDisplay.style.paddingLeft = '1.5rem';
                scoreDisplay.style.paddingRight = '1.5rem';
                
                // Стили в зависимости от темы
                if (isDarkTheme) {
                    scoreDisplay.style.backgroundColor = 'rgba(255, 215, 0, 0.15)';
                    scoreDisplay.style.border = '2px solid rgba(255, 215, 0, 0.3)';
                    scoreDisplay.style.boxShadow = '0 0 25px rgba(255, 215, 0, 0.4)';
                } else {
                    scoreDisplay.style.backgroundColor = 'rgba(184, 134, 11, 0.15)';
                    scoreDisplay.style.border = '2px solid rgba(184, 134, 11, 0.3)';
                    scoreDisplay.style.boxShadow = '0 0 25px rgba(184, 134, 11, 0.4)';
                }
                
                const scoreStar = document.createElement('span');
                scoreStar.className = 'score-star';
                scoreStar.textContent = '⭐';
                scoreStar.style.marginRight = '0.7rem';
                scoreStar.style.fontSize = '2.2rem';
                
                if (isDarkTheme) {
                    scoreStar.style.color = '#FFD700';
                    scoreStar.style.textShadow = '0 0 10px rgba(255, 215, 0, 0.7)';
                } else {
                    scoreStar.style.color = 'darkgoldenrod';
                    scoreStar.style.textShadow = '0 0 10px rgba(184, 134, 11, 0.5)';
                }
                
                const scoreValue = document.createElement('span');
                scoreValue.className = 'score-value';
                scoreValue.textContent = abortResults.score;
                scoreValue.style.fontSize = '2.2rem';
                scoreValue.style.fontWeight = 'bold';
                
                if (isDarkTheme) {
                    scoreValue.style.color = '#FFD700';
                } else {
                    scoreValue.style.color = '#8B6914';
                }
                
                scoreDisplay.appendChild(scoreStar);
                scoreDisplay.appendChild(scoreValue);
                scoreContainer.appendChild(scoreDisplay);
                modal.appendChild(scoreContainer);
            }
        } else {
            const noStatsMessage = document.createElement('p');
            noStatsMessage.textContent = 'Статистика недоступна';
            noStatsMessage.style.textAlign = 'center';
            statsBlock.appendChild(noStatsMessage);
        }

        modal.appendChild(warningIcon);
        modal.appendChild(title);
        modal.appendChild(errorMessage);
        modal.appendChild(statsBlock);

        // Кнопка "К выбору режима" - синяя (primary)
        const button = document.createElement('button');
        button.textContent = 'К выбору режима';
        button.style.backgroundColor = '#007bff';
        button.style.color = '#fff';
        button.style.border = 'none';
        button.style.borderRadius = '5px';
        button.style.padding = '10px 20px';
        button.style.fontSize = '16px';
        button.style.cursor = 'pointer';
        button.style.marginTop = '20px';
        button.style.fontWeight = 'bold';
        button.style.transition = 'background-color 0.3s ease';
        button.onmouseover = function() {
            this.style.backgroundColor = '#0069d9';
        };
        button.onmouseout = function() {
            this.style.backgroundColor = '#007bff';
        };
        button.onclick = function() {
            window.location.href = '/modeSelect';
        };
        
        modal.appendChild(button);
        overlay.appendChild(modal);

        // Добавляем модальное окно на страницу
        document.body.appendChild(overlay);

        // Создаем функцию для периодической проверки и восстановления стилей модального окна
        const enforceYellowStyles = () => {
            try {
                // Принудительно применяем желтые стили к модальному окну
                const currentStyle = modal.getAttribute('style') || '';
                
                // Проверяем, содержит ли стиль желтую рамку и тень
                if (!currentStyle.includes('border: 2px solid #ffc107') || 
                    !currentStyle.includes('box-shadow: 0 0 30px rgba(255, 193, 7, 0.7)')) {
                    
                    // Добавляем желтые стили, если они отсутствуют
                    const newStyle = currentStyle + '; border: 2px solid #ffc107 !important; box-shadow: 0 0 30px rgba(255, 193, 7, 0.7) !important;';
                    modal.setAttribute('style', newStyle);
                    
                    console.log('Восстановлены желтые стили модального окна');
                }
            } catch (e) {
                console.error('Ошибка при принудительном применении стилей:', e);
            }
        };
        
        // Запускаем функцию сразу и затем периодически
        enforceYellowStyles();
        
        // Запускаем периодическую проверку и восстановление стилей
        const styleEnforcerInterval = setInterval(enforceYellowStyles, 500);
        
        // Через 5 секунд останавливаем периодическую проверку
        setTimeout(() => {
            clearInterval(styleEnforcerInterval);
            console.log('Периодическая проверка стилей остановлена');
        }, 5000);

        // Добавляем обработчик события изменения DOM для гарантии сохранения стилей
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'attributes' && 
                    mutation.attributeName === 'style' && 
                    mutation.target === modal) {
                    enforceYellowStyles();
                }
            });
        });
        
        // Запускаем наблюдение за изменениями атрибута style
        observer.observe(modal, { attributes: true });

        // Принудительно останавливаем все интервалы для предотвращения heartbeat запросов
        try {
            console.log('Принудительная остановка всех интервалов для предотвращения heartbeat запросов');
            
            // Останавливаем все интервалы в диапазоне от 1 до 1000 (обычно достаточно)
            for (let i = 1; i < 1000; i++) {
                try {
                    window.clearInterval(i);
                } catch (e) {
                    // Игнорируем ошибки при очистке несуществующих интервалов
                }
            }
            
            // Дополнительная защита - устанавливаем флаги остановки в объекте _s4t
            if (window._s4t) {
                window._s4t._av = false; // неактивен
                window._s4t._sp = true;  // остановлен
                window._s4t._ad = true;  // деактивирован
                
                // Очищаем сохраненный интервал, если он есть
                if (window._s4t._in !== undefined && window._s4t._in !== null) {
                    try {
                        window.clearInterval(window._s4t._in);
                        window._s4t._in = null;
                        console.log('Интервал heartbeat успешно остановлен');
                    } catch (e) {
                        console.error('Ошибка при остановке интервала heartbeat:', e);
                    }
                }
            }
        } catch (e) {
            console.error('Ошибка при остановке интервалов:', e);
        }

        // Запускаем анимацию появления через небольшую задержку
        setTimeout(function () {
            overlay.style.backgroundColor = 'rgba(25, 20, 0, 0.7)';
            overlay.style.backdropFilter = 'blur(5px)';
            overlay.style.WebkitBackdropFilter = 'blur(5px)';
            
            // Обновляем стили модального окна, сохраняя желтую рамку
            modal.setAttribute('style', modal.getAttribute('style').replace('transform: scale(0.8);', 'transform: scale(1);').replace('opacity: 0;', 'opacity: 1;'));
        }, 10);

    } catch (e) {
        console.error('Ошибка при отображении модального окна о превышении количества неверных ответов:', e);
        
        // В случае ошибки пытаемся показать хотя бы какое-то сообщение
        try {
            alert('Тестирование прервано из-за превышения количества неверных ответов');
            setTimeout(function () {
                window.location.href = '/modeSelect';
            }, 2000);
        } catch (alertError) {
            console.error('Не удалось отобразить сообщение через alert:', alertError);
        }
    }
};

