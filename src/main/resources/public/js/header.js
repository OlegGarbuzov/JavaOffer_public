/**
 * Простой скрипт для мобильного меню
 */
document.addEventListener('DOMContentLoaded', function() {
  console.log('DOM загружен, настраиваю мобильное меню');
  
  // Прямое получение элементов
  var toggle = document.getElementById('mobileMenuToggle');
  var menu = document.getElementById('mainNavbar');
  
  if (!toggle || !menu) {
    console.error('Ошибка: не найдены элементы меню');
    return;
  }
  
  console.log('Найдены элементы мобильного меню');
  
  // Предотвращаем автоинициализацию Bootstrap
  if (toggle.hasAttribute('data-bs-toggle')) {
    toggle.removeAttribute('data-bs-toggle');
  }
  if (toggle.hasAttribute('data-bs-target')) {
    toggle.removeAttribute('data-bs-target');
  }
  
  // Явно скрываем меню на мобильных
  if (window.innerWidth <= 992) {
    menu.style.display = 'none';
    console.log('Мобильное меню установлено в скрытое состояние');
  }
  
  // Обработчик клика по кнопке меню
  toggle.onclick = function(e) {
    e.preventDefault();
    e.stopPropagation();
    console.log('Клик по кнопке меню');
    
    // Прямое переключение видимости
    if (menu.style.display === 'none') {
      menu.style.display = 'block';
      console.log('Меню отображено');
    } else {
      menu.style.display = 'none';
      console.log('Меню скрыто');
    }
    
    return false; // Останавливаем обработку события
  };
  
  console.log('Установлен обработчик клика кнопки меню');
  
  // Закрытие при клике вне меню
  document.addEventListener('click', function(e) {
    if (window.innerWidth <= 992 && 
        menu.style.display !== 'none' && 
        !menu.contains(e.target) && 
        e.target !== toggle && 
        !toggle.contains(e.target)) {
      menu.style.display = 'none';
      console.log('Меню закрыто по внешнему клику');
    }
  });
});

// Дублирующий обработчик на случай, если DOMContentLoaded уже произошел
window.onload = function() {
  console.log('Window loaded, проверяю состояние меню');
  
  var toggle = document.getElementById('mobileMenuToggle');
  var menu = document.getElementById('mainNavbar');
  
  if (!toggle || !menu) {
    return;
  }
  
  // Если на мобильном и еще не установлен обработчик
  if (window.innerWidth <= 992 && !toggle._hasClickHandler) {
    // Явно скрываем меню
    menu.style.display = 'none';
    
    // Устанавливаем обработчик
    toggle.onclick = function(e) {
      e.preventDefault();
      e.stopPropagation();
      
      if (menu.style.display === 'none') {
        menu.style.display = 'block';
      } else {
        menu.style.display = 'none';
      }
      
      return false;
    };
    
    // Метка что обработчик установлен
    toggle._hasClickHandler = true;
    
    console.log('Резервный обработчик установлен');
  }
}; 