document.addEventListener('DOMContentLoaded', function () {
    if (!window.dashboardData || typeof Chart === 'undefined') {
        return;
    }

    const data = window.dashboardData;

    const weightCtx = document.getElementById('weightChart');
    if (weightCtx) {
        new Chart(weightCtx, {
            type: 'line',
            data: {
                labels: data.weightLabels,
                datasets: [{
                    label: '체중',
                    data: data.weightData,
                    tension: 0.4,
                    fill: true,
                    borderColor: '#7ea9e8',
                    backgroundColor: 'rgba(126, 169, 232, 0.15)',
                    pointBackgroundColor: '#7ea9e8',
                    pointBorderColor: '#7ea9e8',
                    pointRadius: 4
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

    const burnCtx = document.getElementById('burnChart');
    if (burnCtx) {
        new Chart(burnCtx, {
            type: 'line',
            data: {
                labels: data.weightLabels,
                datasets: [{
                    data: [150, 240, 320, 410, 430, 510, data.burnedCalories],
                    tension: 0.45,
                    fill: true,
                    borderColor: '#7fb6ff',
                    backgroundColor: 'rgba(127, 182, 255, 0.20)',
                    pointRadius: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } }
            }
        });
    }

    const intakeCtx = document.getElementById('intakeChart');
    if (intakeCtx) {
        new Chart(intakeCtx, {
            type: 'bar',
            data: {
                labels: data.weightLabels,
                datasets: [{
                    data: [980, 1320, 1680, 1770, 1760, 2100, data.intakeCalories],
                    backgroundColor: [
                        '#bcd7fb', '#b0d0fb', '#a8cbfb', '#a8cbfb',
                        '#a8cbfb', '#9ec3fb', '#8e8cf4'
                    ],
                    borderRadius: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } }
            }
        });
    }

    const macroCtx = document.getElementById('macroChart');
    if (macroCtx) {
        new Chart(macroCtx, {
            type: 'doughnut',
            data: {
                labels: ['탄수화물', '지방', '단백질'],
                datasets: [{
                    data: [data.carbPercent, data.fatPercent, data.proteinPercent],
                    backgroundColor: ['#6fa8f3', '#7fd0c6', '#f2c293'],
                    borderWidth: 0,
                    cutout: '58%'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        });
    }
});