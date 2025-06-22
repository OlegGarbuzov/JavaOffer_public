// При загрузке страницы показываем уведомление, если оно помечено как видимое
document.addEventListener('DOMContentLoaded', function() {
  var notification = document.getElementById('error-notification');
  if (notification) {
    // Устанавливаем текст сообщения
    if (notification.getAttribute('data-message')) {
      var messageEl = notification.querySelector('span');
      if (messageEl) {
        messageEl.textContent = notification.getAttribute('data-message');
      }
    }
    
    // Показываем, если нужно
    if (notification.getAttribute('data-show') === 'true') {
      showNotification();
    }
  }
});

function showNotification() {
  var notification = document.getElementById('error-notification');
  if (notification) {
    notification.style.display = 'block';
    // Небольшая задержка для анимации
    setTimeout(function() {
      notification.style.opacity = '1';
      notification.style.transform = 'translateY(0)';
    }, 10);
    
    // Автоматически скрываем через 5 секунд
    setTimeout(function() {
      closeNotification();
    }, 5000);
  }
}

function closeNotification() {
  var notification = document.getElementById('error-notification');
  if (notification) {
    notification.style.opacity = '0';
    notification.style.transform = 'translateY(20px)';
    setTimeout(function() {
      notification.style.display = 'none';
    }, 300);
  }
} 