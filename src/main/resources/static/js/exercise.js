document.addEventListener('DOMContentLoaded', function () {
    const countUps = document.querySelectorAll('.exercise-page .count-up-int');
    countUps.forEach((element) => {
        const endValue = Number(element.dataset.target || 0);
        const startTime = performance.now();
        const duration = 1000;

        function frame(now) {
            const progress = Math.min((now - startTime) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            element.textContent = String(Math.round(endValue * eased));
            if (progress < 1) {
                requestAnimationFrame(frame);
            } else {
                element.textContent = String(Math.round(endValue));
            }
        }

        requestAnimationFrame(frame);
    });

    document.querySelectorAll('.exercise-chip[data-quick-target]').forEach((button) => {
        button.addEventListener('click', function () {
            document.querySelectorAll('.exercise-chip[data-quick-target]').forEach((chip) => chip.classList.remove('active'));
            document.querySelectorAll('.exercise-quick-form').forEach((form) => form.classList.remove('is-active'));

            button.classList.add('active');
            const targetId = button.dataset.quickTarget;
            const targetForm = document.getElementById(targetId);
            if (targetForm) {
                targetForm.classList.add('is-active');
            }
        });
    });

    if (typeof Chart === 'undefined' || !window.exercisePageData) {
        return;
    }

    const chartElement = document.getElementById('exerciseWeekChart');
    if (!chartElement) {
        return;
    }

    new Chart(chartElement, {
        type: 'bar',
        data: {
            labels: window.exercisePageData.weekLabels || [],
            datasets: [{
                data: window.exercisePageData.weekValues || [],
                backgroundColor: [
                    'rgba(34, 197, 94, 0.12)',
                    'rgba(34, 197, 94, 0.12)',
                    'rgba(34, 197, 94, 0.12)',
                    'rgba(34, 197, 94, 0.12)',
                    'rgba(34, 197, 94, 0.12)',
                    'rgba(34, 197, 94, 0.20)',
                    'rgba(34, 197, 94, 1)'
                ],
                borderRadius: 8,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: { enabled: true }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: '#6b7280' }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: '#eef0f5' },
                    ticks: { color: '#6b7280' }
                }
            }
        }
    });
});
