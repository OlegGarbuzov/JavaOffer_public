function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
}

function initAjaxForm(formSelector, notificationId, defaultError) {
    const form = document.querySelector(formSelector);
    if (!form) return;
    
    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        
        // Используем обычную FormData вместо JSON
        const formData = new FormData(form);
        
        const csrfToken = getCookie('X-XSRF-TOKEN');
        try {
            // Отправляем как обычную форму, а не как JSON
            const response = await fetch(form.action, {
                method: 'POST',
                headers: {
                    ...(csrfToken ? {'X-XSRF-TOKEN': csrfToken} : {})
                },
                body: formData
            });
            
            if (response.ok) {
                // Перенаправляем на главную страницу при успехе
                window.location.href = '/';
            } else {
                let errorMessage = defaultError;
                try {
                    const errorText = await response.text();
                    
                    try {
                        const error = JSON.parse(errorText);

                        // Извлекаем понятный текст ошибки из разных форматов
                        if (error.message) {
                            errorMessage = error.message;
                        } else if (error.error && error.status) {
                            errorMessage = error.message || error.error;
                        } else if (error.error) {
                            errorMessage = error.error;
                        } else if (typeof error === 'string') {
                            errorMessage = error;
                        }
                        
                        // Переводим стандартные сообщения об ошибках на русский
                        if (errorMessage === 'Bad credentials') {
                            errorMessage = 'Неверное имя пользователя или пароль';
                        }
                    } catch (jsonError) {
                        errorMessage = errorText || defaultError;
                    }
                } catch (e) {
                    // Ошибка при чтении ответа
                }

                showNotification(notificationId, errorMessage);
            }
        } catch (err) {
            showNotification(notificationId, 'Ошибка сети или сервера');
        }
    });
}

function showNotification(notificationId, message) {
    let notif = document.getElementById(notificationId);
    if (!notif) {
        notif = document.createElement('div');
        notif.id = notificationId;
        notif.className = 'alert alert-danger position-fixed top-0 start-50 translate-middle-x mt-3';
        notif.style.zIndex = 9999;
        document.body.appendChild(notif);
    }
    notif.textContent = message;
    notif.classList.remove('d-none');
    setTimeout(() => {
        notif.classList.add('d-none');
    }, 4000);
}

window.initAjaxForm = initAjaxForm; 