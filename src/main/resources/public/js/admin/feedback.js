let feedbackCurrentPage = 0;
let feedbackPageSize = 50;
let feedbackTotalPages = 1;

function getFeedbackCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
}

function loadFeedbacks(page = 0) {
    feedbackPageSize = parseInt(document.getElementById('feedbackPageSize').value);
    let url = `/admin/api/feedback?page=${page}&size=${feedbackPageSize}`;
    fetch(url)
        .then(r => r.json())
        .then(data => {
            renderFeedbacks(data.content);
            feedbackTotalPages = data.totalPages;
            feedbackCurrentPage = data.number;
            renderFeedbackPagination();
        });
}

function renderFeedbacks(feedbacks) {
    const tbody = document.getElementById('feedbackTableBody');
    tbody.innerHTML = '';
    feedbacks.forEach(fb => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
      <td>${fb.id}</td>
      <td><a href="#" class="feedback-text-link" data-text="${encodeURIComponent(fb.text)}">${fb.text.length > 40 ? fb.text.substring(0, 40) + '…' : fb.text}</a></td>
      <td>${fb.remoteAdd || ''}</td>
      <td>${fb.user || ''}</td>
      <td><button class="btn btn-sm btn-danger delete-feedback-btn" data-id="${fb.id}">Удалить</button></td>
    `;
        tbody.appendChild(tr);
    });
}

function renderFeedbackPagination() {
    const pagination = document.getElementById('feedbackPagination');
    pagination.innerHTML = '';
    if (feedbackTotalPages <= 1) {
        pagination.style.display = 'none';
        return;
    }
    pagination.style.display = '';
    pagination.innerHTML += `<li class="page-item${feedbackCurrentPage === 0 ? ' disabled' : ''}"><a class="page-link" href="#" data-page="${feedbackCurrentPage - 1}">&laquo;</a></li>`;
    for (let i = 0; i < feedbackTotalPages; i++) {
        pagination.innerHTML += `<li class="page-item${i === feedbackCurrentPage ? ' active' : ''}"><a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`;
    }
    pagination.innerHTML += `<li class="page-item${feedbackCurrentPage === feedbackTotalPages - 1 ? ' disabled' : ''}"><a class="page-link" href="#" data-page="${feedbackCurrentPage + 1}">&raquo;</a></li>`;
    pagination.querySelectorAll('a.page-link').forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const page = parseInt(this.getAttribute('data-page'));
            if (!isNaN(page) && page >= 0 && page < feedbackTotalPages) {
                loadFeedbacks(page);
            }
        });
    });
}

document.addEventListener('DOMContentLoaded', function () {
    loadFeedbacks();
    document.getElementById('feedbackPageSize').onchange = () => loadFeedbacks(0);
    document.getElementById('feedbackTableBody').addEventListener('click', function (e) {
        if (e.target.classList.contains('delete-feedback-btn')) {
            const id = e.target.getAttribute('data-id');
            if (confirm('Удалить обращение?')) {
                const xsrfToken = getFeedbackCookie('X-XSRF-TOKEN');
                fetch(`/admin/api/feedback/${id}`, {
                    method: 'DELETE',
                    headers: {'X-XSRF-TOKEN': xsrfToken}
                }).then(() => loadFeedbacks(feedbackCurrentPage));
            }
        }
        if (e.target.classList.contains('feedback-text-link')) {
            e.preventDefault();
            const text = decodeURIComponent(e.target.getAttribute('data-text'));
            document.getElementById('feedbackTextContent').textContent = text;
            new bootstrap.Modal(document.getElementById('feedbackTextModal')).show();
        }
    });
}); 