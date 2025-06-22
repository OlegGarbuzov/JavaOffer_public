// Функция для предпросмотра вопроса
function showPreview() {
  const question = document.querySelector('[name="question"]').value;
  const topic = document.querySelector('[name="topic"]').value;
  const difficulty = document.querySelector('[name="difficulty"]').value;
  const grade = document.querySelector('[name="grade"]').value;

  const answerCards = document.querySelectorAll('#answers-container .card');
  const previewAnswers = document.getElementById('preview-answers');
  previewAnswers.innerHTML = '';

  answerCards.forEach((card, index) => {
    const content = card.querySelector('textarea[name^="answers["]').value;
    const explanation = card.querySelector('textarea[name$=".explanation"]').value;
    const isCorrect = card.querySelector('input[name$=".isCorrect"]').value === 'true';

    const answerHtml = `
      <div class="answer-btn" data-correct="${isCorrect}">
        <span>${content}</span>
        ${explanation ? `<span class="text-muted ms-2">${explanation}</span>` : ''}
      </div>`;
    previewAnswers.insertAdjacentHTML('beforeend', answerHtml);
  });

  document.getElementById('preview-question').innerHTML = question;
  document.getElementById('preview-topic').textContent = topic;
  document.getElementById('preview-difficulty').textContent = difficulty;
  document.getElementById('preview-grade').textContent = grade;

  const modal = new bootstrap.Modal(document.getElementById('previewModal'));
  modal.show();
}

// Функция для предпросмотра в форме редактирования
function showEditPreview() {
  const editModalEl = document.getElementById('editQuestionModal');
  const previewModalEl = document.getElementById('previewModal');
  const editModal = bootstrap.Modal.getInstance(editModalEl);
  if (editModal) editModal.hide();

  const question = document.getElementById('editQuestion').value;
  const topic = document.getElementById('editTopic').options[document.getElementById('editTopic').selectedIndex].text;
  const difficulty = document.getElementById('editDifficulty').options[document.getElementById('editDifficulty').selectedIndex].text;
  const grade = document.getElementById('editGrade').options[document.getElementById('editGrade').selectedIndex].text;

  // Вставляем HTML вопроса с поддержкой <pre><code>...
  document.getElementById('preview-question').innerHTML = question;

  const answerCards = document.querySelectorAll('#editAnswersContainer .card');
  const previewAnswers = document.getElementById('preview-answers');
  previewAnswers.innerHTML = '';

  answerCards.forEach((card, index) => {
    const content = card.querySelector('textarea[name^="answers["]').value;
    const explanation = card.querySelector('textarea[name$=".explanation"]').value;
    const isCorrect = card.querySelector('input[name$=".isCorrect"]').value === 'true';

    const answerHtml = `
      <div class="answer-btn" data-correct="${isCorrect}">
        <span>${content}</span>
        ${explanation ? `<span class="text-muted ms-2">${explanation}</span>` : ''}
      </div>`;
    previewAnswers.insertAdjacentHTML('beforeend', answerHtml);
  });

  document.getElementById('preview-topic').textContent = topic;
  document.getElementById('preview-difficulty').textContent = difficulty;
  document.getElementById('preview-grade').textContent = grade;

  const previewModal = new bootstrap.Modal(previewModalEl);
  previewModal.show();

  // После закрытия предпросмотра — снова показываем модалку редактирования
  previewModalEl.addEventListener('hidden.bs.modal', function handler() {
    editModal.show();
    previewModalEl.removeEventListener('hidden.bs.modal', handler);
  });
}

document.addEventListener('DOMContentLoaded', function() {
  // Константы URL-путей
  const URL_ADMIN_ROOT = '/admin';
  const URL_ADMIN_FILTER_ROOT = `${URL_ADMIN_ROOT}/filter`;
  const URL_ADMIN_FILTER_QUESTIONS = `${URL_ADMIN_FILTER_ROOT}/questions`;
  
  // Добавляем обработчики для кнопок редактирования
  document.querySelectorAll('.edit-task-btn').forEach(button => {
    button.addEventListener('click', function() {
      const taskId = this.getAttribute('data-id');
      loadTaskForEdit(taskId);
    });
  });
  
  // Создаем модальное окно предпросмотра, если его нет
  if (!document.getElementById('previewModal')) {
    const previewModal = document.createElement('div');
    previewModal.className = 'modal fade';
    previewModal.id = 'previewModal';
    previewModal.tabIndex = '-1';
    previewModal.setAttribute('aria-labelledby', 'previewModalLabel');
    previewModal.setAttribute('aria-hidden', 'true');
    
    previewModal.innerHTML = `
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="previewModalLabel">Предпросмотр вопроса</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <div class="card mb-3">
              <div class="card-header">
                <div class="d-flex justify-content-between align-items-center">
                  <h5 id="preview-topic" class="mb-0"></h5>
                  <div>
                    <span class="badge bg-primary me-2" id="preview-difficulty"></span>
                    <span class="badge bg-secondary" id="preview-grade"></span>
                  </div>
                </div>
              </div>
              <div class="card-body">
                <p id="preview-question"></p>
                <hr>
                <h6>Ответы:</h6>
                <ul class="list-group" id="preview-answers"></ul>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Закрыть</button>
          </div>
        </div>
      </div>
    `;
    
    document.body.appendChild(previewModal);
  }
  
  // Обработчик добавления нового ответа
  document.getElementById('addAnswerBtn').addEventListener('click', function() {
    const container = document.getElementById('answers-container');
    const answerCards = container.querySelectorAll('.card');
    const newIndex = answerCards.length;
    
    const card = document.createElement('div');
    card.className = 'card mb-3';
    card.innerHTML = `
      <div class="card-body">
        <div class="d-flex justify-content-between mb-3">
          <div class="form-check">
            <input class="form-check-input" type="radio" name="correctAnswer" value="${newIndex}">
            <label class="form-check-label">Правильный ответ</label>
          </div>
          <button type="button" class="btn btn-sm btn-outline-danger remove-answer">
            <i class="fa-solid fa-times"></i> Удалить
          </button>
        </div>
        <div class="mb-3">
          <label class="form-label">Текст ответа <span class="text-danger">*</span></label>
          <textarea class="form-control" name="answers[${newIndex}].content" rows="2" required></textarea>
        </div>
        <div class="mb-3">
          <label class="form-label">Пояснение</label>
          <textarea class="form-control" name="answers[${newIndex}].explanation" rows="2"></textarea>
        </div>
        <input type="hidden" name="answers[${newIndex}].isCorrect" value="false">
      </div>
    `;
    
    container.appendChild(card);
    
    // Добавляем обработчик для кнопки удаления
    card.querySelector('.remove-answer').addEventListener('click', function() {
      card.remove();
      updateAnswerIndices();
    });
    
    // Добавляем обработчик для радиокнопок
    card.querySelector('input[type="radio"]').addEventListener('change', function() {
      updateCorrectAnswers();
    });
  });
  
  // Навешиваем обработчик на радио-кнопки стартовых ответов
  document.querySelectorAll('#answers-container input[type="radio"][name="correctAnswer"]').forEach(radio => {
    radio.addEventListener('change', function() {
      updateCorrectAnswers();
    });
  });
  
  // Обновление индексов ответов после удаления
  function updateAnswerIndices() {
    const container = document.getElementById('answers-container');
    const answerCards = container.querySelectorAll('.card');
    
    answerCards.forEach((card, index) => {
      const contentTextarea = card.querySelector('textarea[name^="answers["]');
      const explanationTextarea = card.querySelector('textarea[name$=".explanation"]');
      const isCorrectInput = card.querySelector('input[name$=".isCorrect"]');
      const radioInput = card.querySelector('input[type="radio"]');
      
      contentTextarea.name = `answers[${index}].content`;
      explanationTextarea.name = `answers[${index}].explanation`;
      isCorrectInput.name = `answers[${index}].isCorrect`;
      radioInput.value = index;
    });
  }
  
  // Обновление скрытых полей isCorrect при изменении выбора правильного ответа
  function updateCorrectAnswers() {
    const container = document.getElementById('answers-container');
    const answerCards = container.querySelectorAll('.card');
    const selectedRadio = container.querySelector('input[type="radio"]:checked');
    
    if (selectedRadio) {
      const selectedIndex = parseInt(selectedRadio.value);
      
      answerCards.forEach((card, index) => {
        const isCorrectInput = card.querySelector('input[name$=".isCorrect"]');
        isCorrectInput.value = (index === selectedIndex) ? 'true' : 'false';
      });
    }
  }
  
  function loadTaskForEdit(taskId) {
    fetch(`/admin/questions/${taskId}/data`)
      .then(response => response.json())
      .then(task => {
        document.getElementById('editTaskId').value = task.id;
        document.getElementById('editQuestion').value = task.question;
        document.getElementById('editTopic').value = task.topic;
        document.getElementById('editDifficulty').value = task.difficulty;
        document.getElementById('editGrade').value = task.grade;
        const container = document.getElementById('editAnswersContainer');
        container.innerHTML = '';
        task.answers.forEach((answer, index) => {
          const card = document.createElement('div');
          card.className = 'card mb-3';
          card.innerHTML = `
            <div class="card-body">
              <div class="d-flex justify-content-between mb-3">
                <div class="form-check">
                  <input class="form-check-input" type="radio" name="editCorrectAnswer" value="${index}" ${answer.isCorrect ? 'checked' : ''}>
                  <label class="form-check-label">Правильный ответ</label>
                </div>
                <button type="button" class="btn btn-sm btn-outline-danger edit-remove-answer">
                  <i class="fa-solid fa-times"></i> Удалить
                </button>
              </div>
              <div class="mb-3">
                <label class="form-label">Текст ответа <span class="text-danger">*</span></label>
                <textarea class="form-control" name="answers[${index}].content" rows="2" required>${answer.content}</textarea>
              </div>
              <div class="mb-3">
                <label class="form-label">Пояснение</label>
                <textarea class="form-control" name="answers[${index}].explanation" rows="2">${answer.explanation || ''}</textarea>
              </div>
              <input type="hidden" name="answers[${index}].isCorrect" value="${answer.isCorrect}">
            </div>
          `;
          container.appendChild(card);
          card.querySelector('.edit-remove-answer').addEventListener('click', function() {
            card.remove();
            updateEditAnswerIndices();
          });
          card.querySelector('input[type="radio"]').addEventListener('change', function() {
            updateEditCorrectAnswers();
          });
        });
        // Открываем модальное окно редактирования через один экземпляр
        const editModalEl = document.getElementById('editQuestionModal');
        let editModal = bootstrap.Modal.getInstance(editModalEl);
        if (!editModal) {
          editModal = new bootstrap.Modal(editModalEl);
        }
        editModal.show();
      })
      .catch(error => console.error('Ошибка при загрузке вопроса:', error));
  }
  
  // Обработчик формы редактирования
  document.getElementById('saveEditBtn').addEventListener('click', function(e) {
    e.preventDefault();
    
    const taskId = document.getElementById('editTaskId').value;
    const formData = new FormData(document.getElementById('editTaskForm'));
    const data = {};
    
    formData.forEach((value, key) => {
      // Обработка вложенных объектов (answers)
      if (key.includes('.')) {
        const parts = key.split('.');
        const parentKey = parts[0].split('[')[0];
        const index = parseInt(parts[0].match(/\d+/)[0]);
        const childKey = parts[1];
        
        if (!data[parentKey]) data[parentKey] = [];
        if (!data[parentKey][index]) data[parentKey][index] = {};
        
        data[parentKey][index][childKey] = value === 'true' ? true : (value === 'false' ? false : value);
      } else {
        data[key] = value;
      }
    });
    
    // Получаем CSRF токен из meta-тегов
    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
    
    fetch(`/admin/questions/${taskId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        [header]: token
      },
      body: JSON.stringify(data)
    })
      .then(response => {
        if (response.ok) {
          window.location.href = '/admin/questions';
        } else {
          throw new Error('Ошибка при обновлении вопроса');
        }
      })
      .catch(error => {
        console.error('Ошибка:', error);
        alert('Произошла ошибка при обновлении вопроса');
      });
  });
  
  // Функции для формы редактирования
  function updateEditAnswerIndices() {
    const container = document.getElementById('editAnswersContainer');
    const answerCards = container.querySelectorAll('.card');
    
    answerCards.forEach((card, index) => {
      const contentTextarea = card.querySelector('textarea[name^="answers["]');
      const explanationTextarea = card.querySelector('textarea[name$=".explanation"]');
      const isCorrectInput = card.querySelector('input[name$=".isCorrect"]');
      const radioInput = card.querySelector('input[type="radio"]');
      
      contentTextarea.name = `answers[${index}].content`;
      explanationTextarea.name = `answers[${index}].explanation`;
      isCorrectInput.name = `answers[${index}].isCorrect`;
      radioInput.value = index;
    });
  }
  
  function updateEditCorrectAnswers() {
    const container = document.getElementById('editAnswersContainer');
    const answerCards = container.querySelectorAll('.card');
    const selectedRadio = container.querySelector('input[type="radio"]:checked');
    
    if (selectedRadio) {
      const selectedIndex = parseInt(selectedRadio.value);
      
      answerCards.forEach((card, index) => {
        const isCorrectInput = card.querySelector('input[name$=".isCorrect"]');
        isCorrectInput.value = (index === selectedIndex) ? 'true' : 'false';
      });
    }
  }
  
  // Обработчик добавления нового ответа в форме редактирования
  document.getElementById('editAddAnswerBtn').addEventListener('click', function() {
    const container = document.getElementById('editAnswersContainer');
    const answerCards = container.querySelectorAll('.card');
    const newIndex = answerCards.length;
    
    const card = document.createElement('div');
    card.className = 'card mb-3';
    card.innerHTML = `
      <div class="card-body">
        <div class="d-flex justify-content-between mb-3">
          <div class="form-check">
            <input class="form-check-input" type="radio" name="editCorrectAnswer" value="${newIndex}">
            <label class="form-check-label">Правильный ответ</label>
          </div>
          <button type="button" class="btn btn-sm btn-outline-danger edit-remove-answer">
            <i class="fa-solid fa-times"></i> Удалить
          </button>
        </div>
        <div class="mb-3">
          <label class="form-label">Текст ответа <span class="text-danger">*</span></label>
          <textarea class="form-control" name="answers[${newIndex}].content" rows="2" required></textarea>
        </div>
        <div class="mb-3">
          <label class="form-label">Пояснение</label>
          <textarea class="form-control" name="answers[${newIndex}].explanation" rows="2"></textarea>
        </div>
        <input type="hidden" name="answers[${newIndex}].isCorrect" value="false">
      </div>
    `;
    
    container.appendChild(card);
    
    // Добавляем обработчик для кнопки удаления
    card.querySelector('.edit-remove-answer').addEventListener('click', function() {
      card.remove();
      updateEditAnswerIndices();
    });
    
    // Добавляем обработчик для радиокнопок
    card.querySelector('input[type="radio"]').addEventListener('change', function() {
      updateEditCorrectAnswers();
    });
  });

  // --- Фильтрация вопросов ---
  const topicFilter = document.getElementById('topicFilter');
  const difficultyFilter = document.getElementById('difficultyFilter');
  const gradeFilter = document.getElementById('gradeFilter');
  const searchInput = document.getElementById('searchInput');
  const resetFilters = document.getElementById('resetFilters');
  const tasksTableBody = document.getElementById('tasksTableBody');

  function fetchFilteredQuestions() {
    const params = new URLSearchParams({
      topic: topicFilter.value,
      difficulty: difficultyFilter.value,
      grade: gradeFilter.value,
      search: searchInput.value
    });
    fetch(`${URL_ADMIN_FILTER_QUESTIONS}?${params.toString()}`)
      .then(res => res.text())
      .then(html => {
        tasksTableBody.innerHTML = html;
        // Перепривязываем обработчики кнопок после обновления таблицы
        attachEditButtonHandlers();
      });
  }

  // Функция для привязки обработчиков к кнопкам редактирования
  function attachEditButtonHandlers() {
    document.querySelectorAll('.edit-task-btn').forEach(button => {
      button.addEventListener('click', function() {
        const taskId = this.getAttribute('data-id');
        loadTaskForEdit(taskId);
      });
    });
  }

  // Привязываем обработчики при загрузке страницы
  attachEditButtonHandlers();
  
  topicFilter.addEventListener('change', fetchFilteredQuestions);
  difficultyFilter.addEventListener('change', fetchFilteredQuestions);
  gradeFilter.addEventListener('change', fetchFilteredQuestions);
  searchInput.addEventListener('input', function() {
    // Можно добавить debounce для оптимизации
    fetchFilteredQuestions();
  });
  resetFilters.addEventListener('click', function() {
    topicFilter.value = '';
    difficultyFilter.value = '';
    gradeFilter.value = '';
    searchInput.value = '';
    fetchFilteredQuestions();
  });
}); 