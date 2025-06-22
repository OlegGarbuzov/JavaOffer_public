function toggleBurgerMenu() {
  console.log('toggleBurgerMenu called');
  const burgerNavbar = document.getElementById("burgerNavbar");
  
  if (burgerNavbar) {
    if (burgerNavbar.classList.contains("isActive")) {
      burgerNavbar.classList.remove("isActive");
    } else {
      burgerNavbar.classList.add("isActive");
    }
  }
  return false;
}

document.addEventListener("DOMContentLoaded", function() {
  function handleResize() {
    const burgerButton = document.getElementById("burgerButton");
    const mainNavbar = document.getElementById("mainNavbar");
    const burgerNavbar = document.getElementById("burgerNavbar");
    
    if (burgerButton && mainNavbar && burgerNavbar) {
      if (window.innerWidth <= 992) {
        burgerButton.style.display = "flex";
        mainNavbar.style.display = "none";
        burgerNavbar.classList.remove("isActive");
      } else {
        burgerButton.style.display = "none";
        mainNavbar.style.display = "flex";
        burgerNavbar.classList.remove("isActive");
      }
    }
  }
  
  handleResize();
  window.addEventListener("resize", handleResize);
  
  document.addEventListener("click", function(event) {
    const burgerButton = document.getElementById("burgerButton");
    const burgerNavbar = document.getElementById("burgerNavbar");
    
    if (burgerButton && burgerNavbar && window.innerWidth <= 992) {
      if (burgerNavbar.classList.contains("isActive") && 
          !burgerNavbar.contains(event.target) && 
          event.target !== burgerButton && 
          !burgerButton.contains(event.target)) {
        burgerNavbar.classList.remove("isActive");
      }
    }
  });
  
  const burgerNavbar = document.getElementById("burgerNavbar");
  if (burgerNavbar) {
    const links = burgerNavbar.getElementsByTagName("a");
    for (let i = 0; i < links.length; i++) {
      links[i].addEventListener("click", function() {
        burgerNavbar.classList.remove("isActive");
      });
    }
  }
}); 