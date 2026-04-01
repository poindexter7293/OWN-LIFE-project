document.addEventListener('DOMContentLoaded', function () {
    if (!window.dashboardData || typeof Chart === 'undefined') {
        return;
    }

    const data = window.dashboardData;

    const commonNoLegend = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { display: false },
            tooltip: { enabled: true }
        }
    };

    const weightCanvas = document.getElementById('weightChart');
    if (weightCanvas) {
        new Chart(weightCanvas, {
            type: 'line',
            data: {
                labels: data.weightLabels || [],
                datasets: [{
                    data: data.weightData || [],
                    tension: 0.35,
                    fill: false,
                    borderColor: '#cfd3dd',
                    pointBackgroundColor: '#cfd3dd',
                    pointBorderColor: '#cfd3dd',
                    pointRadius: 2,
                    pointHoverRadius: 4,
                    borderWidth: 2
                }]
            },
            options: {
                ...commonNoLegend,
                scales: {
                    y: {
                        beginAtZero: false,
                        grid: { color: '#f0f1f5' },
                        ticks: { color: '#a1a7b4' }
                    },
                    x: {
                        grid: { color: '#f7f7fa' },
                        ticks: { color: '#a1a7b4' }
                    }
                }
            }
        });
    }

    const burnedProgressCanvas = document.getElementById('burnedProgressChart');
    if (burnedProgressCanvas) {
        const burnedCalories = Number(data.burnedCalories || 0);
        const targetCalories = Number(data.targetCalories || 0);
        const burnedPercent = targetCalories > 0
            ? Math.min(100, Math.round((burnedCalories * 100) / targetCalories))
            : 0;

        new Chart(burnedProgressCanvas, {
            type: 'doughnut',
            data: {
                labels: ['달성', '남음'],
                datasets: [{
                    data: [burnedPercent, Math.max(0, 100 - burnedPercent)],
                    backgroundColor: ['#e5e7eb', '#f6f7fb'],
                    borderWidth: 0,
                    cutout: '82%'
                }]
            },
            options: {
                ...commonNoLegend,
                plugins: { legend: { display: false }, tooltip: { enabled: false } }
            }
        });
    }

    const intakeProgressCanvas = document.getElementById('intakeProgressChart');
    if (intakeProgressCanvas) {
        const percent = Number(data.intakePercent || 0);
        new Chart(intakeProgressCanvas, {
            type: 'doughnut',
            data: {
                labels: ['달성', '남음'],
                datasets: [{
                    data: [percent, Math.max(0, 100 - percent)],
                    backgroundColor: ['#e5e7eb', '#f6f7fb'],
                    borderWidth: 0,
                    cutout: '82%'
                }]
            },
            options: {
                ...commonNoLegend,
                plugins: { legend: { display: false }, tooltip: { enabled: false } }
            }
        });
    }

    const macroCanvas = document.getElementById('macroChart');
    if (macroCanvas) {
        new Chart(macroCanvas, {
            type: 'doughnut',
            data: {
                labels: ['탄수화물', '지방', '단백질'],
                datasets: [{
                    data: [
                        Number(data.carbPercent || 0),
                        Number(data.fatPercent || 0),
                        Number(data.proteinPercent || 0)
                    ],
                    backgroundColor: ['#4c57e8', '#f5a000', '#22c55e'],
                    borderWidth: 0,
                    cutout: '64%'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }
});