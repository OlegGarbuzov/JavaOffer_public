document.addEventListener('DOMContentLoaded', function() {
    let isSubmitting = false;
    let ratingRulesModal = null;

    // Инициализация модального окна Bootstrap
    if (document.getElementById('ratingRulesModal')) {
        ratingRulesModal = new bootstrap.Modal(document.getElementById('ratingRulesModal'));
        
        // Добавляем обработчик события для исправления стилей модального окна
        document.getElementById('ratingRulesModal').addEventListener('show.bs.modal', function() {
            // Принудительно устанавливаем стили для модального окна
            setTimeout(function() {
                const modalBackdrops = document.querySelectorAll('.modal-backdrop');
                modalBackdrops.forEach(backdrop => {
                    backdrop.style.cssText = 'opacity: 0.5 !important; background-color: #000 !important;';
                });
                
                const modalContent = document.querySelector('#ratingRulesModal .modal-content');
                if (modalContent) {
                    const isDarkTheme = document.documentElement.getAttribute('data-theme') === 'dark' || 
                                       document.documentElement.getAttribute('data-bs-theme') === 'dark';
                    
                    if (isDarkTheme) {
                        modalContent.style.cssText = 'background-color: #212529 !important; color: #fff !important; opacity: 1 !important;';
                    } else {
                        modalContent.style.cssText = 'background-color: #fff !important; color: #212529 !important; opacity: 1 !important;';
                    }
                }
            }, 0);
            
            // Повторяем с задержкой для надежности
            setTimeout(function() {
                const modalBackdrops = document.querySelectorAll('.modal-backdrop');
                modalBackdrops.forEach(backdrop => {
                    backdrop.style.cssText = 'opacity: 0.5 !important; background-color: #000 !important;';
                });
                
                const modalContent = document.querySelector('#ratingRulesModal .modal-content');
                if (modalContent) {
                    const isDarkTheme = document.documentElement.getAttribute('data-theme') === 'dark' || 
                                       document.documentElement.getAttribute('data-bs-theme') === 'dark';
                    
                    if (isDarkTheme) {
                        modalContent.style.cssText = 'background-color: #212529 !important; color: #fff !important; opacity: 1 !important;';
                    } else {
                        modalContent.style.cssText = 'background-color: #fff !important; color: #212529 !important; opacity: 1 !important;';
                    }
                }
            }, 100);
        });
    }

    // Радикальное решение для неавторизованной карточки соревновательного режима в темной теме
    function fixDisabledRatingCardInDarkTheme() {
        // Проверяем, является ли текущая тема темной
        const isDarkTheme = document.documentElement.getAttribute('data-theme') === 'dark' || 
                           document.documentElement.getAttribute('data-bs-theme') === 'dark';
        
        // Проверяем, является ли текущее устройство десктопом (ширина экрана >= 769px)
        const isDesktop = window.innerWidth >= 769;
        
        // Находим неавторизованную карточку соревновательного режима для десктопа
        const disabledRatingCardDesktop = document.querySelector('.mode-block.mode-rating.disabled#modeRatingDesktop');
        
        if (disabledRatingCardDesktop && isDarkTheme && isDesktop) {
            // Полностью блокируем события наведения для карточки
            disabledRatingCardDesktop.style.pointerEvents = 'none';
            
            // Принудительно отключаем все эффекты
            disabledRatingCardDesktop.style.transform = 'scale(0.95)';
            disabledRatingCardDesktop.style.transition = 'none';
            disabledRatingCardDesktop.style.boxShadow = 'none';
            disabledRatingCardDesktop.style.border = 'none';
            disabledRatingCardDesktop.style.zIndex = '0';
            
            // Находим контейнер кнопки и саму кнопку
            const loginContainer = disabledRatingCardDesktop.querySelector('.login-container');
            const loginButton = disabledRatingCardDesktop.querySelector('.login-button');
            
            if (loginContainer) {
                // Разрешаем события наведения для контейнера кнопки
                loginContainer.style.pointerEvents = 'auto';
                loginContainer.style.zIndex = '100';
                loginContainer.style.position = 'absolute';
                loginContainer.style.bottom = '20px';
                loginContainer.style.left = '0';
                loginContainer.style.right = '0';
                loginContainer.style.textAlign = 'center';
                
                // Создаем новую кнопку, чтобы избежать наследования стилей
                if (loginButton) {
                    const newLoginButton = document.createElement('a');
                    newLoginButton.href = loginButton.href;
                    newLoginButton.className = 'btn btn-primary';
                    newLoginButton.textContent = loginButton.textContent;
                    newLoginButton.style.fontWeight = '600';
                    newLoginButton.style.padding = '8px 16px';
                    newLoginButton.style.borderRadius = '6px';
                    newLoginButton.style.boxShadow = '0 4px 8px rgba(0, 0, 0, 0.2)';
                    newLoginButton.style.transition = 'all 0.3s ease';
                    newLoginButton.style.border = 'none';
                    newLoginButton.style.backgroundColor = '#0d6efd';
                    newLoginButton.style.color = 'white';
                    newLoginButton.style.zIndex = '101';
                    newLoginButton.style.position = 'relative';
                    
                    // Добавляем эффект наведения только для кнопки
                    newLoginButton.addEventListener('mouseover', function() {
                        this.style.transform = 'translateY(-2px)';
                        this.style.boxShadow = '0 6px 12px rgba(0, 0, 0, 0.3)';
                        this.style.backgroundColor = '#0b5ed7';
                    });
                    
                    newLoginButton.addEventListener('mouseout', function() {
                        this.style.transform = '';
                        this.style.boxShadow = '0 4px 8px rgba(0, 0, 0, 0.2)';
                        this.style.backgroundColor = '#0d6efd';
                    });
                    
                    // Заменяем старую кнопку на новую
                    loginContainer.innerHTML = '';
                    loginContainer.appendChild(newLoginButton);
                }
            }
            
            // Отключаем эффекты для всех дочерних элементов карточки
            const cardChildren = disabledRatingCardDesktop.querySelectorAll('*:not(.login-container):not(.login-container *)');
            cardChildren.forEach(child => {
                child.style.transition = 'none';
                child.style.transform = 'none';
            });
        }
    }
    
    // Вызываем функцию при загрузке страницы
    fixDisabledRatingCardInDarkTheme();
    
    // Вызываем функцию при изменении размера окна
    window.addEventListener('resize', fixDisabledRatingCardInDarkTheme);
    
    // Отслеживаем изменение темы
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.type === 'attributes' && 
                (mutation.attributeName === 'data-theme' || mutation.attributeName === 'data-bs-theme')) {
                fixDisabledRatingCardInDarkTheme();
            }
        });
    });
    
    observer.observe(document.documentElement, { attributes: true });

    // Добавляем обработчики для неактивных карточек соревновательного режима
    const disabledRatingCards = document.querySelectorAll('.mode-block.mode-rating.disabled, .mobile-card.disabled#mobileCardRating');
    disabledRatingCards.forEach(card => {
        // Принудительно отключаем эффекты свечения
        card.style.boxShadow = 'none';
        card.style.transform = 'scale(0.95)';
        card.style.zIndex = '0';
        card.style.border = 'none';
        card.style.outline = 'none';
        
        // Добавляем обработчики для кнопки входа в систему
        const loginButton = card.querySelector('.login-button');
        if (loginButton) {
            // Убеждаемся, что кнопка всегда поверх полупрозрачного слоя
            loginButton.style.zIndex = '30';
            loginButton.style.position = 'relative';
            loginButton.style.color = 'white';
            loginButton.style.opacity = '1';
        }
    });

    function submitForm(mode) {
        isSubmitting = true;
        const form = document.getElementById('modeForm');
        document.getElementById('examModeInput').value = mode;
        form.action = '/testing/process';
        form.method = 'post';
        form.submit();
    }

    // Обработчик кнопки в модальном окне
    const acceptRulesButton = document.getElementById('acceptRulesButton');
    if (acceptRulesButton) {
        acceptRulesButton.addEventListener('click', function() {
            ratingRulesModal.hide();
            submitForm('RATING');
        });
    }

    const modeFree = document.getElementById('modeFree');
    const modeRatingDesktop = document.getElementById('modeRatingDesktop');

    if (modeFree) {
        modeFree.addEventListener('click', function() {
            submitForm('FREE');
        });
    }

    if (modeRatingDesktop && !modeRatingDesktop.classList.contains('disabled')) {
        modeRatingDesktop.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            // Показываем модальное окно с правилами вместо прямой отправки формы
            if (ratingRulesModal) {
                ratingRulesModal.show();
            } else {
                // Если модальное окно не инициализировано, выполняем стандартное поведение
                submitForm('RATING');
            }
        });
    }

    if (window.innerWidth < 768) {
        const mobileCards = document.getElementById('mobileCards');
        const mobileCardFree = document.getElementById('mobileCardFree');
        const mobileCardRating = document.getElementById('mobileCardRating');
        const indicators = document.querySelectorAll('.mobile-indicator');
        
        let currentIndex = 0;
        
        function updateDisplay() {
            mobileCards.style.transform = `translateX(-${currentIndex * 50}%)`;
            
            indicators.forEach((indicator, index) => {
                if (index === currentIndex) {
                    indicator.classList.add('active');
                } else {
                    indicator.classList.remove('active');
                }
            });
            
            document.getElementById('examModeInput').value = currentIndex === 0 ? 'FREE' : 'RATING';
        }
        
        if (mobileCardFree) {
            mobileCardFree.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                if (isSubmitting) return;
                isSubmitting = true;
                submitForm('FREE');
            });
        }
        
        if (mobileCardRating && !mobileCardRating.classList.contains('disabled')) {
            mobileCardRating.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                if (isSubmitting) return;
                
                // Показываем модальное окно с правилами вместо прямой отправки формы
                if (ratingRulesModal) {
                    ratingRulesModal.show();
                } else {
                    // Если модальное окно не инициализировано, выполняем стандартное поведение
                    isSubmitting = true;
                    submitForm('RATING');
                }
            });
        }
        
        indicators.forEach(function(indicator, index) {
            indicator.addEventListener('click', function(e) {
                if (isSubmitting) return;
                e.preventDefault();
                e.stopPropagation();
                currentIndex = index;
                updateDisplay();
            });
        });
        
        let startX = null;
        let endX = null;
        
        mobileCards.addEventListener('touchstart', function(e) {
            if (isSubmitting) return;
            startX = e.touches[0].clientX;
            endX = startX;
        }, { passive: false });
        
        mobileCards.addEventListener('touchmove', function(e) {
            if (isSubmitting) return;
            if (e.touches.length > 0) {
                endX = e.touches[0].clientX;
            }
        }, { passive: false });
        
        mobileCards.addEventListener('touchend', function(e) {
            if (isSubmitting || startX === null || endX === null) return;
            const diff = startX - endX;
            
            if (Math.abs(diff) > 50) {
                if (diff > 0 && currentIndex === 0) {
                    currentIndex = 1;
                    updateDisplay();
                } else if (diff < 0 && currentIndex === 1) {
                    currentIndex = 0;
                    updateDisplay();
                }
            }
            startX = null;
            endX = null;
        }, { passive: false });
        
        updateDisplay();
    }
}); 