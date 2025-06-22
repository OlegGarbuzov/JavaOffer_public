document.addEventListener('DOMContentLoaded', function() {
  // Добавляем класс для плавного перехода между темами
  document.body.classList.add('theme-transition');
  
  // Получаем текущую тему из localStorage или используем светлую тему по умолчанию
  const getCurrentTheme = () => {
    return localStorage.getItem('theme') || 'light';
  };

  // Устанавливаем тему
  const setTheme = (theme) => {
    document.documentElement.setAttribute('data-bs-theme', theme);
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
    
    // Обновляем состояние чекбокса переключателя
    const themeSwitch = document.getElementById('theme-switch');
    if (themeSwitch) {
      themeSwitch.checked = theme === 'dark';
    }
    
    // Обновляем состояние мобильного переключателя
    const themeSwitchMobile = document.getElementById('theme-switch-mobile');
    if (themeSwitchMobile) {
      themeSwitchMobile.checked = theme === 'dark';
    }
    
    // Обновляем состояние переключателя в админ-панели
    const themeSwitchAdmin = document.getElementById('theme-switch-admin');
    if (themeSwitchAdmin) {
      themeSwitchAdmin.checked = theme === 'dark';
    }
    
    // Добавляем или удаляем класс dark-theme для body
    if (theme === 'dark') {
      document.body.classList.add('dark-theme');
      document.documentElement.classList.add('dark-theme');
    } else {
      document.body.classList.remove('dark-theme');
      document.documentElement.classList.remove('dark-theme');
    }
    
    // Принудительно обновляем стили карточек и таблиц
    if (theme === 'dark') {
      const cardHeaders = document.querySelectorAll('.card-header');
      cardHeaders.forEach(header => {
        // Удаляем инлайновые стили, которые могут мешать применению темной темы
        header.removeAttribute('style');
        header.style.backgroundColor = '#2c3034';
        header.style.color = '#fff';
        header.style.borderColor = '#444';
      });
      
      const tables = document.querySelectorAll('.table');
      tables.forEach(table => {
        table.style.color = '#dee2e6';
        table.style.borderColor = '#444';
      });
    } else {
      // Для светлой темы также удаляем инлайновые стили
      const cardHeaders = document.querySelectorAll('.card-header');
      cardHeaders.forEach(header => {
        header.removeAttribute('style');
      });
    }
    
    // Отправляем событие изменения темы
    const event = new CustomEvent('themeChanged', { detail: { theme } });
    document.dispatchEvent(event);
    
    // Принудительно отключаем эффекты свечения для неактивных карточек соревновательного режима
    const disabledRatingCards = document.querySelectorAll('.mode-block.mode-rating.disabled, .mobile-card.disabled#mobileCardRating');
    disabledRatingCards.forEach(card => {
      card.style.boxShadow = 'none !important';
      card.style.transform = 'scale(0.95) !important';
      card.style.zIndex = '0 !important';
      card.style.border = 'none !important';
      card.style.outline = 'none !important';
      
      // Добавляем обработчик события наведения для неактивной карточки
      if (!card._hoverHandlerAdded) {
        card.addEventListener('mouseenter', function() {
          this.style.boxShadow = 'none !important';
          this.style.transform = 'scale(0.95) !important';
          this.style.zIndex = '0 !important';
          this.style.border = 'none !important';
          this.style.outline = 'none !important';
        });
        
        card.addEventListener('mouseleave', function() {
          this.style.boxShadow = 'none !important';
          this.style.transform = 'scale(0.95) !important';
          this.style.zIndex = '0 !important';
          this.style.border = 'none !important';
          this.style.outline = 'none !important';
        });
        
        // Добавляем обработчики для ссылки внутри карточки
        const loginLink = card.querySelector('.login-link');
        if (loginLink) {
          loginLink.addEventListener('mouseenter', function(e) {
            e.stopPropagation();
            card.style.boxShadow = 'none !important';
            card.style.transform = 'scale(0.95) !important';
            card.style.zIndex = '0 !important';
            card.style.border = 'none !important';
            card.style.outline = 'none !important';
          });
          
          loginLink.addEventListener('mouseleave', function(e) {
            e.stopPropagation();
            card.style.boxShadow = 'none !important';
            card.style.transform = 'scale(0.95) !important';
            card.style.zIndex = '0 !important';
            card.style.border = 'none !important';
            card.style.outline = 'none !important';
          });
        }
        
        card._hoverHandlerAdded = true;
      }
    });
  };

  // Инициализация темы при загрузке страницы
  setTheme(getCurrentTheme());

  // Обработчик изменения переключателя темы
  const themeSwitch = document.getElementById('theme-switch');
  if (themeSwitch) {
    themeSwitch.addEventListener('change', function() {
      const newTheme = this.checked ? 'dark' : 'light';
      setTheme(newTheme);
    });
  }
  
  // Обработчик изменения мобильного переключателя темы
  const themeSwitchMobile = document.getElementById('theme-switch-mobile');
  if (themeSwitchMobile) {
    themeSwitchMobile.addEventListener('change', function() {
      const newTheme = this.checked ? 'dark' : 'light';
      setTheme(newTheme);
    });
  }
  
  // Обработчик изменения переключателя темы в админ-панели
  const themeSwitchAdmin = document.getElementById('theme-switch-admin');
  if (themeSwitchAdmin) {
    themeSwitchAdmin.addEventListener('change', function() {
      const newTheme = this.checked ? 'dark' : 'light';
      setTheme(newTheme);
    });
  }
  
  // Обработчик изменения системной темы
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  mediaQuery.addEventListener('change', (e) => {
    // Проверяем, установлена ли пользовательская тема
    const userTheme = localStorage.getItem('theme');
    if (!userTheme) {
      // Если пользователь не выбрал тему, используем системную
      setTheme(e.matches ? 'dark' : 'light');
    }
  });
  
  // Добавляем обработчик для всех страниц, чтобы убедиться, что тема применяется везде
  window.addEventListener('load', function() {
    setTheme(getCurrentTheme());
    
    // Принудительно применяем стили темы ко всем элементам
    if (getCurrentTheme() === 'dark') {
      document.querySelectorAll('.card-header, .card, .table, .table-striped tbody tr, .table th, .table td').forEach(element => {
        if (!element.classList.contains('theme-checked')) {
          element.classList.add('theme-checked');
        }
      });
    }
    
    // Исправляем проблему с прозрачным фоном модального окна при смене темы
    const fixModalBackdrop = () => {
      const modalBackdrops = document.querySelectorAll('.modal-backdrop');
      modalBackdrops.forEach(backdrop => {
        backdrop.style.cssText = 'opacity: 0.5 !important; background-color: #000 !important;';
      });
      
      const modals = document.querySelectorAll('.modal');
      modals.forEach(modal => {
        if (modal.classList.contains('rating-rules-modal') || modal.querySelector('.rating-rules-modal')) {
          const modalContent = modal.querySelector('.modal-content');
          if (modalContent) {
            if (getCurrentTheme() === 'dark') {
              modalContent.style.cssText = 'background-color: #212529 !important; color: #fff !important; opacity: 1 !important;';
            } else {
              modalContent.style.cssText = 'background-color: #fff !important; color: #212529 !important; opacity: 1 !important;';
            }
          }
        }
      });
    };
    
    // Вызываем функцию сразу и при изменении темы
    fixModalBackdrop();
    document.addEventListener('themeChanged', function() {
      // Вызываем несколько раз с разными задержками для большей надежности
      setTimeout(fixModalBackdrop, 0);
      setTimeout(fixModalBackdrop, 50);
      setTimeout(fixModalBackdrop, 150);
      setTimeout(fixModalBackdrop, 300);
    });
    
    // Также добавляем обработчик для модальных окон
    const modalButtons = document.querySelectorAll('[data-bs-toggle="modal"]');
    modalButtons.forEach(button => {
      button.addEventListener('click', function() {
        // Вызываем несколько раз с разными задержками для большей надежности
        setTimeout(fixModalBackdrop, 0);
        setTimeout(fixModalBackdrop, 50);
        setTimeout(fixModalBackdrop, 150);
        setTimeout(fixModalBackdrop, 300);
      });
    });
    
    // Добавляем обработчик для обработки модальных окон, открываемых через JavaScript
    const observer = new MutationObserver(function(mutations) {
      mutations.forEach(function(mutation) {
        if (mutation.addedNodes && mutation.addedNodes.length > 0) {
          mutation.addedNodes.forEach(function(node) {
            if (node.nodeType === 1) {
              if (node.classList && (node.classList.contains('modal') || node.classList.contains('modal-backdrop'))) {
                setTimeout(fixModalBackdrop, 50);
              }
            }
          });
        }
      });
    });
    
    // Наблюдаем за изменениями в DOM для обнаружения новых модальных окон
    observer.observe(document.body, {
      childList: true,
      subtree: true
    });
    
    // Дополнительно обрабатываем события показа модальных окон
    document.body.addEventListener('shown.bs.modal', function() {
      // Вызываем несколько раз с разными задержками для большей надежности
      setTimeout(fixModalBackdrop, 0);
      setTimeout(fixModalBackdrop, 50);
      setTimeout(fixModalBackdrop, 150);
      setTimeout(fixModalBackdrop, 300);
    });
    
    // Принудительно исправляем фон модального окна при смене темы
    document.addEventListener('themeChanged', function() {
      setTimeout(function() {
        const modalBackdrops = document.querySelectorAll('.modal-backdrop');
        if (modalBackdrops.length > 0) {
          modalBackdrops.forEach(backdrop => {
            backdrop.style.cssText = 'opacity: 0.5 !important; background-color: #000 !important;';
          });
        }
        
        const modals = document.querySelectorAll('.modal');
        modals.forEach(modal => {
          const modalContent = modal.querySelector('.modal-content');
          if (modalContent) {
            if (getCurrentTheme() === 'dark') {
              modalContent.style.cssText = 'background-color: #212529 !important; color: #fff !important; opacity: 1 !important;';
            } else {
              modalContent.style.cssText = 'background-color: #fff !important; color: #212529 !important; opacity: 1 !important;';
            }
          }
        });
      }, 50);
      
      // Повторно вызываем с большей задержкой для надежности
      setTimeout(function() {
        fixModalBackdrop();
      }, 200);
    });
  });
}); 