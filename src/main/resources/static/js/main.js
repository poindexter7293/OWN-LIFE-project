document.addEventListener('DOMContentLoaded', function () {
    const setupDashboardCardLinks = () => {
        const cards = document.querySelectorAll('[data-dashboard-link]');

        cards.forEach((card) => {
            const href = card.dataset.href;
            const confirmMessage = card.dataset.confirmMessage || '해당 페이지로 이동하시겠습니까?';

            if (!href) return;

            const moveWithConfirm = () => {
                const ok = window.confirm(confirmMessage);
                if (ok) {
                    window.location.href = href;
                }
            };

            card.addEventListener('click', () => {
                moveWithConfirm();
            });

            card.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    moveWithConfirm();
                }
            });
        });
    };

    const setupFocusFromQuery = () => {
        const params = new URLSearchParams(window.location.search);
        const focusTarget = params.get('focus');

        if (!focusTarget) return;

        const targetElement = document.getElementById(focusTarget);
        if (!targetElement) return;

        requestAnimationFrame(() => {
            targetElement.scrollIntoView({
                behavior: 'smooth',
                block: 'center'
            });

            setTimeout(() => {
                targetElement.focus();

                if (typeof targetElement.select === 'function') {
                    targetElement.select();
                }
            }, 250);
        });

        if (window.history && typeof window.history.replaceState === 'function') {
            params.delete('focus');
            const nextQuery = params.toString();
            const nextUrl = `${window.location.pathname}${nextQuery ? `?${nextQuery}` : ''}${window.location.hash || ''}`;
            window.history.replaceState({}, '', nextUrl);
        }
    };

    setupDashboardCardLinks();
    setupFocusFromQuery();

    if (!window.dashboardData || typeof Chart === 'undefined') {
        return;
    }

    const data = window.dashboardData;

    const animateNumber = ({
                               element,
                               endValue,
                               duration = 1200,
                               decimals = 0,
                               suffix = '',
                               prefix = '',
                               formatter = null
                           }) => {
        if (!element) return;

        const startTime = performance.now();
        const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);

        const frame = (now) => {
            const progress = Math.min((now - startTime) / duration, 1);
            const eased = easeOutCubic(progress);

            let current = endValue * eased;

            if (decimals === 0) {
                current = Math.round(current);
            } else {
                current = Number(current.toFixed(decimals));
            }

            if (formatter) {
                element.textContent = formatter(current);
            } else {
                element.textContent = `${prefix}${current}${suffix}`;
            }

            if (progress < 1) {
                requestAnimationFrame(frame);
            } else {
                if (formatter) {
                    element.textContent = formatter(endValue);
                } else if (decimals === 0) {
                    element.textContent = `${prefix}${Math.round(endValue)}${suffix}`;
                } else {
                    element.textContent = `${prefix}${Number(endValue).toFixed(decimals)}${suffix}`;
                }
            }
        };

        requestAnimationFrame(frame);
    };

    const runCountAnimations = () => {
        document.querySelectorAll('.count-up-weight').forEach((el) => {
            const target = Number(el.dataset.target || 0);
            animateNumber({
                element: el,
                endValue: target,
                duration: 1400,
                decimals: 1,
                formatter: (value) => `${Number(value).toFixed(1)}kg`
            });
        });

        document.querySelectorAll('.count-up-int').forEach((el) => {
            const target = Number(el.dataset.target || 0);
            animateNumber({
                element: el,
                endValue: target,
                duration: 1100,
                decimals: 0
            });
        });

        document.querySelectorAll('.count-up-percent').forEach((el) => {
            const target = Number(el.dataset.target || 0);
            animateNumber({
                element: el,
                endValue: target,
                duration: 1200,
                decimals: 0,
                suffix: '%'
            });
        });

        document.querySelectorAll('.count-up-gram').forEach((el) => {
            const target = Number(el.dataset.target || 0);
            animateNumber({
                element: el,
                endValue: target,
                duration: 1300,
                decimals: 0,
                suffix: 'g'
            });
        });
    };

    const commonNoLegend = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { display: false },
            tooltip: { enabled: true }
        }
    };

    const lineDelayAnimation = {
        x: {
            type: 'number',
            easing: 'easeOutCubic',
            duration: 500,
            from: NaN,
            delay(ctx) {
                if (ctx.type !== 'data' || ctx.xStarted) return 0;
                ctx.xStarted = true;
                return ctx.dataIndex * 140;
            }
        },
        y: {
            type: 'number',
            easing: 'easeOutCubic',
            duration: 500,
            from(ctx) {
                if (ctx.type !== 'data') {
                    return ctx.chart.scales.y.getPixelForValue(ctx.chart.scales.y.min);
                }

                const meta = ctx.chart.getDatasetMeta(ctx.datasetIndex);
                const prev = ctx.index === 0
                    ? meta.data[0].getProps(['y'], true).y
                    : meta.data[ctx.index - 1].getProps(['y'], true).y;

                return prev;
            },
            delay(ctx) {
                if (ctx.type !== 'data' || ctx.yStarted) return 0;
                ctx.yStarted = true;
                return ctx.dataIndex * 140;
            }
        }
    };

    const animateDoughnutFill = (chart, targetPercent, duration = 1200) => {
        const startTime = performance.now();
        const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);

        const frame = (now) => {
            const progress = Math.min((now - startTime) / duration, 1);
            const eased = easeOutCubic(progress);

            const currentPercent = targetPercent * eased;
            const remain = Math.max(0, 100 - currentPercent);

            chart.data.datasets[0].data = [currentPercent, remain];
            chart.update('none');

            if (progress < 1) {
                requestAnimationFrame(frame);
            } else {
                chart.data.datasets[0].data = [targetPercent, Math.max(0, 100 - targetPercent)];
                chart.update('none');
            }
        };

        requestAnimationFrame(frame);
    };

    const createProgressDoughnut = (canvas, targetPercent, type) => {
        if (!canvas) return null;

        const safePercent = Math.max(0, Number(targetPercent || 0));

        let fillColor = '#d7d9e1';
        const remainColor = '#f3f4f8';

        if (type === 'burned') {
            fillColor = safePercent >= 100 ? '#4c57e8' : '#d7d9e1';
        } else if (type === 'intake') {
            fillColor = safePercent > 100 ? '#ef4444' : '#4c57e8';
        }

        const chart = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: ['달성', '남음'],
                datasets: [{
                    data: [0, 100],
                    backgroundColor: [fillColor, remainColor],
                    borderWidth: 0,
                    cutout: '82%',
                    hoverOffset: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                rotation: -90,
                circumference: 360,
                animation: false,
                plugins: {
                    legend: { display: false },
                    tooltip: { enabled: false }
                }
            }
        });

        animateDoughnutFill(chart, safePercent, 1250);
        return chart;
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
                animation: lineDelayAnimation,
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

    const burnedTargetCalories = Number(data.burnedTargetCalories ?? 0);
    const intakeTargetCalories = Number(data.intakeTargetCalories ?? 0);

    const burnedPercent = Number(data.burnedPercent ?? 0);
    const intakePercent = Number(data.intakePercent ?? 0);

    if (burnedTargetCalories > 0) {
        createProgressDoughnut(
            document.getElementById('burnedProgressChart'),
            burnedPercent,
            'burned'
        );
    }

    if (intakeTargetCalories > 0) {
        createProgressDoughnut(
            document.getElementById('intakeProgressChart'),
            intakePercent,
            'intake'
        );
    }

    const macroCanvas = document.getElementById('macroChart');
    if (macroCanvas) {
        const carbPercent = Number(data.carbPercent || 0);
        const fatPercent = Number(data.fatPercent || 0);
        const proteinPercent = Number(data.proteinPercent || 0);

        const macroChartData =
            carbPercent === 0 && fatPercent === 0 && proteinPercent === 0
                ? [1, 1, 1]
                : [carbPercent, fatPercent, proteinPercent];

        new Chart(macroCanvas, {
            type: 'doughnut',
            data: {
                labels: ['탄수화물', '지방', '단백질'],
                datasets: [{
                    data: macroChartData,
                    backgroundColor: ['#4c57e8', '#f5a000', '#22c55e'],
                    borderWidth: 0,
                    cutout: '64%'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: {
                    duration: 1200,
                    easing: 'easeOutCubic',
                    animateRotate: true,
                    animateScale: false
                },
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }

    runCountAnimations();
});