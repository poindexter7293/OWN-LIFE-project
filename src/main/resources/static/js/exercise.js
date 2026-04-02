document.addEventListener('DOMContentLoaded', function () {
    initCountUp();
    initExerciseModeTabs();
    initQuickTabs();
    initExerciseWeekChart();
    initSummaryCardActions();
});

function initCountUp() {
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
}

function initExerciseModeTabs() {
    const modeTabs = document.querySelectorAll('.exercise-mode-tab');
    const modePanels = document.querySelectorAll('.exercise-mode-panel');

    modeTabs.forEach((tab) => {
        tab.addEventListener('click', function () {
            const targetId = this.dataset.modeTarget;

            modeTabs.forEach(btn => btn.classList.remove('active'));
            modePanels.forEach(panel => panel.classList.remove('is-active'));

            this.classList.add('active');

            const targetPanel = document.getElementById(targetId);
            if (targetPanel) {
                targetPanel.classList.add('is-active');
            }
        });
    });
}

function initQuickTabs() {
    const quickTabs = document.querySelectorAll('.exercise-quick-tabs .exercise-chip');
    const quickForms = document.querySelectorAll('.exercise-quick-form');

    quickTabs.forEach((tab) => {
        tab.addEventListener('click', function () {
            const targetId = this.dataset.quickTarget;

            quickTabs.forEach(btn => btn.classList.remove('active'));
            quickForms.forEach(form => form.classList.remove('is-active'));

            this.classList.add('active');

            const targetForm = document.getElementById(targetId);
            if (targetForm) {
                targetForm.classList.add('is-active');
            }
        });
    });
}

function initExerciseWeekChart() {
    if (typeof Chart === 'undefined' || !window.exercisePageData) {
        return;
    }

    const chartElement = document.getElementById('exerciseWeekChart');
    if (!chartElement) {
        return;
    }

    const labels = window.exercisePageData.weekLabels || [];
    const values = window.exercisePageData.weekValues || [];
    const goalBurnedKcal = Number(window.exercisePageData.goalBurnedKcal || 0);

    const datasets = [
        {
            type: 'bar',
            label: '일일 소모 칼로리',
            data: values,
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
            borderSkipped: false,
            barPercentage: 0.62,
            categoryPercentage: 0.7
        }
    ];

    if (goalBurnedKcal > 0) {
        datasets.push({
            type: 'line',
            label: '하루 목표 칼로리',
            data: Array(labels.length).fill(goalBurnedKcal),
            borderColor: 'rgba(249, 115, 22, 0.95)',
            borderWidth: 2,
            borderDash: [6, 6],
            pointRadius: 0,
            pointHoverRadius: 0,
            fill: false,
            tension: 0
        });
    }

    new Chart(chartElement, {
        data: {
            labels: labels,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,

            animation: {
                duration: 1200,
                easing: 'easeOutQuart'
            },

            plugins: {
                legend: {
                    display: goalBurnedKcal > 0
                },
                tooltip: {
                    enabled: true,
                    callbacks: {
                        afterBody: function (tooltipItems) {
                            if (goalBurnedKcal <= 0 || !tooltipItems || !tooltipItems.length) {
                                return [];
                            }

                            const barItem = tooltipItems.find(item => item.dataset.type === 'bar') || tooltipItems[0];
                            const currentValue = Number(barItem.raw || 0);
                            const rate = Math.round((currentValue * 100) / goalBurnedKcal);

                            return ['해당일 목표 달성률: ' + rate + '%'];
                        }
                    }
                }
            },

            scales: {
                x: {
                    grid: { display: false },
                    ticks: { color: '#6b7280' }
                },
                y: {
                    beginAtZero: true,
                    suggestedMax: Math.max(...values, goalBurnedKcal, 100) * 1.15,
                    grid: { color: '#eef0f5' },
                    ticks: { color: '#6b7280' }
                }
            },

            datasets: {
                bar: {
                    animations: {
                        y: {
                            duration: 1200,
                            easing: 'easeOutQuart'
                        },
                        base: {
                            duration: 1200,
                            easing: 'easeOutQuart'
                        }
                    }
                },
                line: {
                    animations: {
                        y: {
                            duration: 1200,
                            easing: 'easeOutQuart'
                        }
                    }
                }
            }
        }
    });
}

function initSummaryCardActions() {
    const todayBurnedCard = document.getElementById('todayBurnedCard');
    const goalBurnedCard = document.getElementById('goalBurnedCard');

    const exerciseAddSection = document.getElementById('exerciseAddSection');
    const directAddTab = document.getElementById('directAddTab');
    const directAddPanel = document.getElementById('directAddPanel');
    const exerciseNameInput = document.getElementById('exerciseNameInput');

    if (todayBurnedCard) {
        todayBurnedCard.style.cursor = 'pointer';

        todayBurnedCard.addEventListener('click', function () {
            if (exerciseAddSection) {
                exerciseAddSection.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }

            if (directAddTab && !directAddTab.classList.contains('active')) {
                directAddTab.click();
            }

            window.setTimeout(function () {
                if (directAddPanel && !directAddPanel.classList.contains('is-active')) {
                    directAddPanel.classList.add('is-active');
                }

                if (exerciseNameInput) {
                    exerciseNameInput.focus();
                    if (typeof exerciseNameInput.select === 'function') {
                        exerciseNameInput.select();
                    }
                }
            }, 380);
        });
    }

    if (goalBurnedCard) {
        goalBurnedCard.style.cursor = 'pointer';

        goalBurnedCard.addEventListener('click', function () {
            const confirmed = window.confirm('목표 소모 칼로리 수정을 위해 마이페이지로 이동하겠습니다.');

            if (confirmed) {
                window.location.href = '/mypage';
            }
        });
    }
}