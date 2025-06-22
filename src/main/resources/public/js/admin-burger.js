function toggleAdminBurgerMenu() {
  console.log("Toggle Admin Burger Menu called");
  const burgerNavbar = document.getElementById("adminBurgerNavbar");
  if (burgerNavbar) {
    if (burgerNavbar.classList.contains("isActive")) {
      burgerNavbar.classList.remove("isActive");
      burgerNavbar.style.display = "none";
    } else {
      // Правильное позиционирование относительно хедера
      const adminHeader = document.querySelector(".admin-header");
      if (adminHeader) {
        const headerRect = adminHeader.getBoundingClientRect();
        // Рассчитываем позицию, чтобы меню было правильно расположено
        burgerNavbar.style.top = (headerRect.bottom + 10) + "px";
        
        // Фиксируем ширину меню
        burgerNavbar.style.width = "auto";
        burgerNavbar.style.minWidth = "250px";
        
        // Размещаем меню справа от кнопки бургер
        const burgerButton = document.getElementById("adminBurgerButton");
        if (burgerButton) {
          const buttonRect = burgerButton.getBoundingClientRect();
          burgerNavbar.style.right = (window.innerWidth - buttonRect.right - 10) + "px";
          burgerNavbar.style.left = "auto";
        } else {
          // Запасной вариант, если кнопка не найдена
          burgerNavbar.style.right = "5%";
          burgerNavbar.style.left = "auto";
        }
      }
      
      burgerNavbar.classList.add("isActive");
      burgerNavbar.style.display = "block";
    }
  } else {
    console.error("Admin burger menu element not found!");
  }
  return false;
}

document.addEventListener("DOMContentLoaded", function() {
  console.log("Admin Burger Menu: DOM Content Loaded");
  
  // Инициализация состояния бургер-меню
  const burgerNavbar = document.getElementById("adminBurgerNavbar");
  if (burgerNavbar) {
    burgerNavbar.style.display = "none";
  }
  
  // Функция для управления адаптивностью заголовка - упрощенная версия
  function ensureAdminHeaderVisibility() {
    const adminHeader = document.querySelector(".admin-header");
    if (adminHeader) {
      // Базовые стили для видимости
      adminHeader.style.display = "block";
      adminHeader.style.visibility = "visible";
      adminHeader.style.opacity = "1";
      
      // Обновляем позицию бургер-меню при необходимости
      const burgerNavbar = document.getElementById("adminBurgerNavbar");
      if (burgerNavbar && burgerNavbar.classList.contains("isActive")) {
        const headerRect = adminHeader.getBoundingClientRect();
        burgerNavbar.style.top = (headerRect.bottom + 10) + "px";
      }
    }
  }
  
  // Функция обработки изменения размера окна
  function handleResize() {
    const burgerButton = document.getElementById("adminBurgerButton");
    const mainNavbar = document.getElementById("adminMainNavbar");
    const burgerNavbar = document.getElementById("adminBurgerNavbar");
    
    if (burgerButton && mainNavbar && burgerNavbar) {
      if (window.innerWidth <= 992) {
        burgerButton.style.display = "flex";
        mainNavbar.style.display = "none";
        
        // Если меню открыто, обновляем его позицию
        if (burgerNavbar.classList.contains("isActive")) {
          const adminHeader = document.querySelector(".admin-header");
          if (adminHeader) {
            const headerRect = adminHeader.getBoundingClientRect();
            burgerNavbar.style.top = (headerRect.bottom + 10) + "px";
            
            if (burgerButton) {
              const buttonRect = burgerButton.getBoundingClientRect();
              burgerNavbar.style.right = (window.innerWidth - buttonRect.right - 10) + "px";
            }
          }
        } else {
          burgerNavbar.style.display = "none";
        }
      } else {
        burgerButton.style.display = "none";
        mainNavbar.style.display = "flex";
        burgerNavbar.classList.remove("isActive");
        burgerNavbar.style.display = "none";
      }
    }
  }
  
  // Вызываем при загрузке и регистрируем на изменение размера окна
  handleResize();
  window.addEventListener("resize", handleResize);
  
  // Закрываем меню при клике вне его
  document.addEventListener("click", function(event) {
    const burgerButton = document.getElementById("adminBurgerButton");
    const burgerNavbar = document.getElementById("adminBurgerNavbar");
    
    if (burgerButton && burgerNavbar) {
      if (burgerNavbar.classList.contains("isActive") && 
          !burgerNavbar.contains(event.target) && 
          event.target !== burgerButton && 
          !burgerButton.contains(event.target)) {
        burgerNavbar.classList.remove("isActive");
        burgerNavbar.style.display = "none";
      }
    }
  });
  
  // Закрываем меню при клике на любую ссылку внутри него
  if (burgerNavbar) {
    const links = burgerNavbar.getElementsByTagName("a");
    for (let i = 0; i < links.length; i++) {
      links[i].addEventListener("click", function() {
        burgerNavbar.classList.remove("isActive");
        burgerNavbar.style.display = "none";
      });
    }
  }
  
  // Начальная проверка видимости
  ensureAdminHeaderVisibility();
}); 