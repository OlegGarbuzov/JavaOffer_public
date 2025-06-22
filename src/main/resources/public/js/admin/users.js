let currentPage = 0;
let pageSize = 50;
let totalPages = 1;

function getFilters() {
    return {
        id: document.getElementById('filterId').value.trim(),
        email: document.getElementById('filterEmail').value.trim(),
        username: document.getElementById('filterUsername').value.trim(),
        role: document.getElementById('filterRole').value,
        accountNonLocked: document.getElementById('filterLocked').value
    };
}

function loadUsers(page = 0) {
    const filters = getFilters();
    pageSize = parseInt(document.getElementById('pageSize').value);
    let url = `/admin/api/users?page=${page}&size=${pageSize}`;
    if (filters.id) url += `&id=${filters.id}`;
    if (filters.email) url += `&email=${encodeURIComponent(filters.email)}`;
    if (filters.username) url += `&username=${encodeURIComponent(filters.username)}`;
    if (filters.role) url += `&role=${filters.role}`;
    if (filters.accountNonLocked) url += `&accountNonLocked=${filters.accountNonLocked}`;

    fetch(url)
        .then(r => r.json())
        .then(data => {
            renderUsers(data.content);
            totalPages = data.totalPages;
            currentPage = data.number;
            renderPagination();
        });
}

function renderUsers(users) {
    const tbody = document.getElementById('usersTableBody');
    tbody.innerHTML = '';
    users.forEach(user => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
      <td>${user.id}</td>
      <td>${user.email}</td>
      <td>${user.username}</td>
      <td>${user.role === 'ROLE_ADMIN' ? 'Администратор' : 'Пользователь'}</td>
      <td><button class="btn btn-link p-0 user-history-btn" data-user-id="${user.id}">${user.userScoreHistoriesCount}</button></td>
      <td>${user.accountNonLocked ? 'Активен' : 'Заблокирован'}</td>
      <td>
        <button class="btn btn-sm btn-primary edit-user-btn" data-user-id="${user.id}">Редактировать</button>
      </td>
    `;
        tbody.appendChild(tr);
    });
}

function renderPagination() {
    const pagination = document.getElementById('pagination');
    pagination.innerHTML = '';
    if (totalPages <= 1) {
        pagination.style.display = 'none';
        return;
    }
    pagination.style.display = '';
    // Prev
    pagination.innerHTML += `<li class="page-item${currentPage === 0 ? ' disabled' : ''}"><a class="page-link" href="#" data-page="${currentPage - 1}">&laquo;</a></li>`;
    for (let i = 0; i < totalPages; i++) {
        pagination.innerHTML += `<li class="page-item${i === currentPage ? ' active' : ''}"><a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`;
    }
    // Next
    pagination.innerHTML += `<li class="page-item${currentPage === totalPages - 1 ? ' disabled' : ''}"><a class="page-link" href="#" data-page="${currentPage + 1}">&raquo;</a></li>`;
    pagination.querySelectorAll('a.page-link').forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const page = parseInt(this.getAttribute('data-page'));
            if (!isNaN(page) && page >= 0 && page < totalPages) {
                loadUsers(page);
            }
        });
    });
}

document.addEventListener('DOMContentLoaded', function () {
    loadUsers();
    document.getElementById('searchButton').onclick = () => loadUsers(0);
    document.getElementById('resetFilters').onclick = () => {
        document.getElementById('filterId').value = '';
        document.getElementById('filterEmail').value = '';
        document.getElementById('filterUsername').value = '';
        document.getElementById('filterRole').value = '';
        document.getElementById('filterLocked').value = '';
        loadUsers(0);
    };
    document.getElementById('pageSize').onchange = () => loadUsers(0);

    document.getElementById('usersTableBody').addEventListener('click', function (e) {
        if (e.target.classList.contains('user-history-btn')) {
            const userId = e.target.getAttribute('data-user-id');
            openUserScoreHistoryModal(userId);
        }
        if (e.target.classList.contains('edit-user-btn')) {
            const userId = e.target.getAttribute('data-user-id');
            openEditUserModal(userId);
        }
    });

    document.getElementById('createUserBtn').onclick = () => {
        document.getElementById('createUserForm').reset();
        new bootstrap.Modal(document.getElementById('createUserModal')).show();
    };

    document.getElementById('createUserForm').onsubmit = function (e) {
        e.preventDefault();
        const xsrfToken = getCookie('X-XSRF-TOKEN');
        const data = {
            email: document.getElementById('createUserEmail').value,
            username: document.getElementById('createUserUsername').value,
            role: document.getElementById('createUserRole').value,
            password: document.getElementById('createUserPassword').value
        };
        fetch('/admin/api/users', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': xsrfToken
            },
            body: JSON.stringify(data)
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Ошибка при создании пользователя. Возможно, пользователь с таким именем или email уже существует.');
                }
                return response.json();
            })
            .then(data => {
                bootstrap.Modal.getInstance(document.getElementById('createUserModal')).hide();
                loadUsers(0);
                // Показываем уведомление об успешном создании
                showNotification('success', 'Пользователь успешно создан');
            })
            .catch(error => {
                console.error('Ошибка:', error);
                showNotification('error', error.message);
            });
    };

    document.getElementById('editUserForm').onsubmit = function (e) {
        e.preventDefault();
        const xsrfToken = getCookie('X-XSRF-TOKEN');
        const userId = document.getElementById('editUserId').value;
        const data = {
            email: document.getElementById('editUserEmail').value,
            username: document.getElementById('editUserUsername').value,
            role: document.getElementById('editUserRole').value,
            accountNonLocked: !document.getElementById('editUserLocked').checked
        };
        fetch(`/admin/api/users/${userId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': xsrfToken
            },
            body: JSON.stringify(data)
        })
            .then(r => r.json())
            .then(() => {
                bootstrap.Modal.getInstance(document.getElementById('editUserModal')).hide();
                loadUsers(currentPage);
            });
    };

    // Accessibility: inert для скрытых модалок
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('show.bs.modal', function () {
            modal.removeAttribute('inert');
        });
        modal.addEventListener('hidden.bs.modal', function () {
            // Если фокус на модалке или внутри неё — переводим фокус на body
            if (modal === document.activeElement || modal.contains(document.activeElement)) {
                document.body.focus();
            }
            modal.setAttribute('inert', '');
        });
    });
});

function openEditUserModal(userId) {
    fetch(`/admin/api/users/${userId}`)
        .then(r => r.json())
        .then(user => {
            document.getElementById('editUserId').value = user.id;
            document.getElementById('editUserEmail').value = user.email;
            document.getElementById('editUserUsername').value = user.username;
            document.getElementById('editUserRole').value = user.role;
            document.getElementById('editUserLocked').checked = !user.accountNonLocked;
            new bootstrap.Modal(document.getElementById('editUserModal')).show();
        });
}

function openUserScoreHistoryModal(userId) {
    let historyPage = 0;
    let historySize = 10;
    let historyExamId = '';

    const modal = new bootstrap.Modal(document.getElementById('userScoreHistoryModal'));
    const container = document.getElementById('userScoreHistoriesContent');

    function renderHistoryTable(pageData) {
        const histories = pageData.content;
        let html = `
      <div class="mb-2 d-flex gap-2 align-items-center">
        <input type="text" class="form-control form-control-sm" id="historyExamIdInput" placeholder="Поиск по examId" value="${historyExamId}">
        <select class="form-select form-select-sm" id="historyPageSize" style="width:auto;">
          <option value="10" ${historySize == 10 ? 'selected' : ''}>10</option>
          <option value="25" ${historySize == 25 ? 'selected' : ''}>25</option>
          <option value="50" ${historySize == 50 ? 'selected' : ''}>50</option>
        </select>
      </div>
      <div class="table-responsive">
        <table class="table table-bordered table-sm">
          <thead><tr>
            <th>ID</th><th>Дата</th><th>Экзамен</th><th>Правильных</th><th>Ошибок</th><th>Очки</th><th>bonusByTime</th><th>Время (сек)</th><th>Среднее время ответа (сек)</th><th></th>
          </tr></thead>
          <tbody>
            ${histories.map(h => {
            const totalAnswers = (h.successAnswersCountAbsolute || 0) + (h.failAnswersCountAbsolute || 0);
            const avgTime = totalAnswers > 0 ? (h.timeTakenToComplete / totalAnswers).toFixed(2) : '-';
            return `<tr>
                <td>${h.id}</td>
                <td>${h.createAt ? h.createAt.replace('T', ' ').substring(0, 19) : ''}</td>
                <td>${h.examID || ''}</td>
                <td>${h.successAnswersCountAbsolute}</td>
                <td>${h.failAnswersCountAbsolute}</td>
                <td>${h.score}</td>
                <td>${h.bonusByTime !== undefined ? h.bonusByTime : ''}</td>
                <td>${h.timeTakenToComplete}</td>
                <td>${avgTime}</td>
                <td><button class="btn btn-sm btn-danger delete-history-btn" data-history-id="${h.id}">Удалить</button></td>
              </tr>`;
        }).join('')}
          </tbody>
        </table>
      </div>
      <div class="d-flex justify-content-between align-items-center mt-2">
        <div>Всего: ${pageData.totalElements}</div>
        <nav><ul class="pagination pagination-sm mb-0" id="historyPagination"></ul></nav>
      </div>
    `;
        container.innerHTML = html;
        const pag = document.getElementById('historyPagination');
        pag.innerHTML = '';
        if (pageData.totalPages > 1) {
            pag.innerHTML += `<li class="page-item${pageData.number === 0 ? ' disabled' : ''}"><a class="page-link" href="#" data-page="${pageData.number - 1}">&laquo;</a></li>`;
            for (let i = 0; i < pageData.totalPages; i++) {
                pag.innerHTML += `<li class="page-item${i === pageData.number ? ' active' : ''}"><a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`;
            }
            pag.innerHTML += `<li class="page-item${pageData.number === pageData.totalPages - 1 ? ' disabled' : ''}"><a class="page-link" href="#" data-page="${pageData.number + 1}">&raquo;</a></li>`;
            pag.querySelectorAll('a.page-link').forEach(link => {
                link.addEventListener('click', function (e) {
                    e.preventDefault();
                    const p = parseInt(this.getAttribute('data-page'));
                    if (!isNaN(p) && p >= 0 && p < pageData.totalPages) {
                        loadHistory(p);
                    }
                });
            });
        }
        // Селектор размера страницы
        document.getElementById('historyPageSize').onchange = function () {
            historySize = parseInt(this.value);
            loadHistory(0);
        };
        // Поиск по examId
        document.getElementById('historyExamIdInput').oninput = function () {
            historyExamId = this.value;
            loadHistory(0);
        };
        // Обработчик удаления
        container.querySelectorAll('.delete-history-btn').forEach(btn => {
            btn.onclick = function () {
                if (confirm('Удалить эту историю?')) {
                    const xsrfToken = getCookie('X-XSRF-TOKEN');
                    fetch(`/admin/api/user-score-history/${btn.getAttribute('data-history-id')}`, {
                        method: 'DELETE',
                        headers: {'X-XSRF-TOKEN': xsrfToken}
                    }).then(() => loadHistory(pageData.number));
                }
            };
        });
    }

    function loadHistory(page) {
        fetch(`/admin/api/users/${userId}/score-histories?page=${page}&size=${historySize}&examId=${encodeURIComponent(historyExamId)}`)
            .then(r => r.json())
            .then(data => {
                renderHistoryTable(data);
            });
    }

    loadHistory(0);
    modal.show();
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Добавим функцию для отображения уведомлений
function showNotification(type, message) {
    const alertContainer = document.getElementById('alertContainer') || createAlertContainer();
    const alertElement = document.createElement('div');
    alertElement.className = `alert alert-${type === 'success' ? 'success' : 'danger'} alert-dismissible fade show`;
    alertElement.role = 'alert';
    alertElement.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Закрыть"></button>
    `;
    alertContainer.appendChild(alertElement);
    
    // Автоматически скрываем уведомление через 5 секунд
    setTimeout(() => {
        if (alertElement.parentNode) {
            const bsAlert = new bootstrap.Alert(alertElement);
            bsAlert.close();
        }
    }, 5000);
}

function createAlertContainer() {
    const container = document.createElement('div');
    container.id = 'alertContainer';
    container.className = 'position-fixed top-0 end-0 p-3';
    container.style.zIndex = '1050';
    document.body.appendChild(container);
    return container;
}