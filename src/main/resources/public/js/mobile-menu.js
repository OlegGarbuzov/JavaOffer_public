/**
 * Скрипт для управления мобильным меню
 * Работает самостоятельно и не зависит от Bootstrap
 */
(function() {
  // Инициализация только после загрузки DOM
  document.addEventListener('DOMContentLoaded', initMobileMenu);
  
  // Также инициализируем после полной загрузки страницы для надежности
  window.addEventListener('load', initMobileMenu);
  
  // Флаг, чтобы избежать повторной инициализации
  var menuInitialized = false;
  
  function initMobileMenu() {
    // Проверяем, была ли уже инициализация
    if (menuInitialized) {
      console.log('Меню уже инициализировано, пропускаем');
      return;
    }
    
    console.log('Инициализация мобильного меню');
    
    // Находим элементы меню
    var toggle = document.getElementById('mobileMenuToggle');
    var menu = document.getElementById('mainNavbar');
    
    // Проверяем что элементы существуют
    if (!toggle || !menu) {
      console.error('Элементы меню не найдены');
      return;
    }
    
    // Удаляем любые атрибуты Bootstrap, которые могут вызывать конфликты
    if (toggle.hasAttribute('data-bs-toggle')) {
      toggle.removeAttribute('data-bs-toggle');
    }
    if (toggle.hasAttribute('data-bs-target')) {
      toggle.removeAttribute('data-bs-target');
    }
    
    // Инициализация состояния меню на мобильных
    function updateMenuState() {
      if (window.innerWidth <= 992) {
        menu.style.display = 'none';
        console.log('Мобильное меню скрыто при инициализации');
      } else {
        menu.style.display = '';
        console.log('Десктопное меню отображается');
      }
    }
    
    // Запускаем инициализацию
    updateMenuState();
    
    // Функция переключения меню
    function toggleMenu(e) {
      console.log('Клик по кнопке меню!');
      
      if (e) {
        e.preventDefault();
        e.stopPropagation();
      }
      
      if (window.innerWidth <= 992) {
        console.log('Текущее состояние меню:', menu.style.display);
        
        if (menu.style.display === 'none' || menu.style.display === '') {
          menu.style.display = 'block';
          console.log('Меню открыто');
        } else {
          menu.style.display = 'none';
          console.log('Меню закрыто');
        }
      }
      
      return false;
    }
    
    // Добавляем прямой обработчик события по клику
    toggle.onclick = toggleMenu;
    
    // Для надежности добавляем и addEventListener
    toggle.addEventListener('click', toggleMenu);
    
    console.log('Добавлен обработчик клика для кнопки меню');
    
    // Закрытие при клике вне меню
    document.addEventListener('click', function(e) {
      if (window.innerWidth <= 992 && 
          menu.style.display === 'block' && 
          !menu.contains(e.target) && 
          e.target !== toggle && 
          !toggle.contains(e.target)) {
        menu.style.display = 'none';
        console.log('Меню закрыто при клике вне');
      }
    });
    
    // Закрытие меню при клике на ссылки
    var links = menu.querySelectorAll('a');
    for (var i = 0; i < links.length; i++) {
      links[i].addEventListener('click', function() {
        if (window.innerWidth <= 992) {
          menu.style.display = 'none';
          console.log('Меню закрыто при клике на ссылку');
        }
      });
    }
    
    // Обработка изменения размера окна
    window.addEventListener('resize', updateMenuState);
    
    // Устанавливаем флаг инициализации
    menuInitialized = true;
    
    console.log('Инициализация мобильного меню завершена');
    
    // Явно показываем как работает обработчик клика
    console.log('Проверка клика на кнопке меню:');
    console.log('toggle.onclick существует:', toggle.onclick !== null);
    
    // Эмулируем нажатие на кнопку для проверки
    setTimeout(function() {
      console.log('ДИАГНОСТИКА: Эмуляция клика на кнопке');
      toggle.click();
    }, 1000);
  }
})(); 