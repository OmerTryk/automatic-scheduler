// DOM içeriği yüklendiğinde çalışacak fonksiyonlar
document.addEventListener('DOMContentLoaded', function() {
    // Aktif menü öğesini işaretleme
    highlightActiveMenuItem();
    
    // Uyarı mesajlarını otomatik kapatma
    setupAlertAutoDismiss();
    
    // Müsaitlik seçimi için event listener
    setupAvailabilitySelection();
    
    // Form doğrulama
    setupFormValidation();
});

// Aktif menü öğesini işaretleme
function highlightActiveMenuItem() {
    const currentPath = window.location.pathname;
    
    document.querySelectorAll('.sidebar .nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (currentPath === href || currentPath.startsWith(href + '/')) {
            link.classList.add('active');
        }
    });
}

// Uyarı mesajlarını 5 saniye sonra otomatik kapatma
function setupAlertAutoDismiss() {
    const alerts = document.querySelectorAll('.alert-dismissible');
    
    alerts.forEach(alert => {
        setTimeout(() => {
            const closeButton = alert.querySelector('.btn-close');
            if (closeButton) {
                closeButton.click();
            }
        }, 5000);
    });
}

// Müsaitlik seçimi için tıklama olayları
function setupAvailabilitySelection() {
    const availabilityCells = document.querySelectorAll('.availability-cell:not(.header):not(.time)');
    
    availabilityCells.forEach(cell => {
        cell.addEventListener('click', function() {
            this.classList.toggle('available');
            this.classList.toggle('unavailable');
            
            // Input değerini güncelleme (varsa)
            const dayInput = document.getElementById('dayOfWeek');
            const startInput = document.getElementById('startHour');
            const endInput = document.getElementById('endHour');
            
            if (dayInput && startInput && endInput) {
                const day = this.dataset.day;
                const hour = this.dataset.hour;
                
                if (this.classList.contains('available')) {
                    dayInput.value = day;
                    startInput.value = hour;
                    endInput.value = parseInt(hour) + 1;
                }
            }
        });
    });
}

// Form doğrulama
function setupFormValidation() {
    const forms = document.querySelectorAll('.needs-validation');
    
    forms.forEach(form => {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            
            form.classList.add('was-validated');
        });
    });
}

// Ders programı oluşturma sürecinde ilerlemeyi göster
function showScheduleGenerationProgress() {
    const progressElement = document.getElementById('schedule-generation-progress');
    const progressBar = document.getElementById('progress-bar');
    
    if (progressElement && progressBar) {
        progressElement.style.display = 'block';
        
        let progress = 0;
        const interval = setInterval(() => {
            progress += 5;
            progressBar.style.width = progress + '%';
            progressBar.setAttribute('aria-valuenow', progress);
            
            if (progress >= 100) {
                clearInterval(interval);
            }
        }, 500);
    }
}

// Tablo satırlarını filtreleme
function filterTable(inputId, tableId) {
    const input = document.getElementById(inputId);
    const table = document.getElementById(tableId);
    const rows = table.getElementsByTagName('tr');
    
    input.addEventListener('keyup', function() {
        const filter = input.value.toUpperCase();
        
        for (let i = 1; i < rows.length; i++) {
            let visible = false;
            const cells = rows[i].getElementsByTagName('td');
            
            for (let j = 0; j < cells.length; j++) {
                const cellText = cells[j].textContent || cells[j].innerText;
                
                if (cellText.toUpperCase().indexOf(filter) > -1) {
                    visible = true;
                    break;
                }
            }
            
            rows[i].style.display = visible ? '' : 'none';
        }
    });
}