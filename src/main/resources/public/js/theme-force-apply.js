// Скрипт для принудительного применения темы на всех страницах
(function() {
  // Функция для получения текущей темы из localStorage
  function getCurrentTheme() {
    return localStorage.getItem('theme') || 'light';
  }

  // Функция для применения темы
  function applyTheme(theme) {
    // Проверка, нужно ли менять тему
    const currentTheme = document.documentElement.getAttribute('data-theme');
    if (currentTheme === theme) {
      return; // Если тема уже применена, ничего не делаем
    }
    
    document.documentElement.setAttribute('data-theme', theme);
    
    // Обновляем состояние переключателей, если они есть на странице
    const themeSwitch = document.getElementById('theme-switch');
    if (themeSwitch) {
      themeSwitch.checked = theme === 'dark';
    }
    
    const themeSwitchMobile = document.getElementById('theme-switch-mobile');
    if (themeSwitchMobile) {
      themeSwitchMobile.checked = theme === 'dark';
    }
    
    // Добавляем или удаляем класс dark-theme для body, если body существует
    if (document.body) {
      if (theme === 'dark') {
        document.body.classList.add('dark-theme');
      } else {
        document.body.classList.remove('dark-theme');
      }
    }
  }

  // Применяем тему немедленно, чтобы избежать мигания
  applyTheme(getCurrentTheme());
  
  // Добавляем обработчик для обновления темы при изменении localStorage из других вкладок
  window.addEventListener('storage', function(e) {
    if (e.key === 'theme') {
      applyTheme(e.newValue || 'light');
    }
  });
  
  // Обработчик для применения темы после полной загрузки страницы
  window.addEventListener('load', function() {
    applyTheme(getCurrentTheme());
    
    // Принудительно применяем стили темы ко всем основным элементам
    if (getCurrentTheme() === 'dark' && document.body) {
      const elements = document.querySelectorAll('body, .card, .navbar, .header-container, .cover-container, .modal-content, .btn, .form-control, .table, .answer-btn, .solution-block');
      elements.forEach(element => {
        if (element) {
          element.classList.add('theme-force-dark');
        }
      });
    }
  });
  
  // Добавляем обработчик для DOMContentLoaded, который сработает раньше load
  document.addEventListener('DOMContentLoaded', function() {
    // Применяем тему повторно, когда DOM загружен
    applyTheme(getCurrentTheme());
  });
})(); 