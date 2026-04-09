document.addEventListener('DOMContentLoaded', function () {
    const showAuthFlowAlert = () => {
        const currentUrl = new URL(window.location.href);
        const params = currentUrl.searchParams;
        const consumedParams = [];
        let message = window.postRedirectAlertMessage || null;

        if (params.get('googleLinkStatus') === 'success') {
            message = 'Google 계정이 연동되었습니다.';
            consumedParams.push('googleLinkStatus');
        }

        if (params.get('kakaoLinkStatus') === 'success') {
            message = '카카오 계정이 연동되었습니다.';
            consumedParams.push('kakaoLinkStatus');
        }

        if (params.get('naverLinkStatus') === 'success') {
            message = '네이버 계정이 연동되었습니다.';
            consumedParams.push('naverLinkStatus');
        }

        if (message) {
            alert(message);
        }

        if (consumedParams.length > 0) {
            consumedParams.forEach((key) => params.delete(key));
            const cleanedQuery = params.toString();
            const cleanedUrl = `${currentUrl.pathname}${cleanedQuery ? `?${cleanedQuery}` : ''}${currentUrl.hash}`;
            window.history.replaceState({}, document.title, cleanedUrl);
        }
    };

    const setupDashboardCardLinks = () => {
        const moveWithConfirm = (card) => {
            if (!card) return;

            const href = card.dataset.href;
            const confirmMessage = card.dataset.confirmMessage || '해당 페이지로 이동하시겠습니까?';

            if (!href) return;

            const ok = window.confirm(confirmMessage);
            if (ok) {
                window.location.href = href;
            }
        };

        document.addEventListener('click', (event) => {
            const card = event.target.closest('[data-dashboard-link]');
            if (!card) return;

            moveWithConfirm(card);
        });

        document.addEventListener('keydown', (event) => {
            const card = event.target.closest('[data-dashboard-link]');
            if (!card) return;

            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                moveWithConfirm(card);
            }
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

    const setupAiCoach = () => {
        const aiCoachWrap = document.getElementById('aiCoachWrap');
        const aiCoachFab = document.getElementById('aiCoachFab');
        const aiCoachPanel = document.getElementById('aiCoachPanel');
        const aiCoachClose = document.getElementById('aiCoachClose');
        const aiCoachResult = document.getElementById('aiCoachResult');
        const quickButtons = document.querySelectorAll('[data-ai-type]');

        if (!aiCoachWrap || !aiCoachFab || !aiCoachPanel || !aiCoachClose) {
            return;
        }

        // 패널 열기
        aiCoachFab.addEventListener('click', function () {
            aiCoachPanel.classList.toggle('hidden');
        });

        // 닫기
        aiCoachClose.addEventListener('click', function () {
            aiCoachPanel.classList.add('hidden');
        });

        // 바깥 클릭 시 닫기
        document.addEventListener('click', function (e) {
            if (!aiCoachWrap.contains(e.target)) {
                aiCoachPanel.classList.add('hidden');
            }
        });

        // ⭐ 여기부터 핵심 (API 연결)
        quickButtons.forEach((button) => {
            button.addEventListener('click', async function () {
                const type = button.dataset.aiType;

                if (!aiCoachResult) return;

                aiCoachResult.innerHTML = `<p class="ai-coach-loading">분석 중...</p>`;

                try {
                    const response = await fetch(`/api/ai-coach/recommend?type=${type}`);
                    const data = await response.json();

                    let html = `
                    <div class="ai-coach-result-box">
                        <h4>${data.title}</h4>
                        <p class="ai-coach-result-summary">${data.summary}</p>
                        <ul>
                `;

                    if (data.messages && data.messages.length > 0) {
                        data.messages.forEach((message) => {
                            html += `<li>${message}</li>`;
                        });
                    }

                    html += `
                        </ul>
                    </div>
                `;

                    aiCoachResult.innerHTML = html;

                } catch (error) {
                    aiCoachResult.innerHTML = `
                    <div class="ai-coach-result-box">
                        <h4>오류 발생</h4>
                        <p class="ai-coach-result-summary">AI 추천을 불러오는 중 문제가 발생했습니다.</p>
                    </div>
                `;
                    console.error(error);
                }
            });
        });
    };

    showAuthFlowAlert();
    setupDashboardCardLinks();
    setupFocusFromQuery();
    setupAiCoach();

    if (typeof Chart === 'undefined') {
        return;
    }

    const data = window.dashboardData || {};

    const weightCanvas = document.getElementById('weightChart');
    const burnedCanvas = document.getElementById('burnedProgressChart');
    const intakeCanvas = document.getElementById('intakeProgressChart');
    const macroCanvas = document.getElementById('macroChart');

    const hasDashboardDom = !!(weightCanvas || burnedCanvas || intakeCanvas || macroCanvas);

    if (!hasDashboardDom) {
        return;
    }

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

    const burnedTargetCalories = Number(
        data.burnedTargetCalories ??
        data.goalBurnedKcal ??
        data.targetBurnedCalories ??
        0
    );

    const intakeTargetCalories = Number(
        data.intakeTargetCalories ??
        data.goalEatKcal ??
        data.targetIntakeCalories ??
        0
    );

    const burnedPercent = Number(data.burnedPercent ?? 0);
    const intakePercent = Number(data.intakePercent ?? 0);

    if (burnedCanvas && burnedTargetCalories > 0) {
        createProgressDoughnut(burnedCanvas, burnedPercent, 'burned');
    }

    if (intakeCanvas && intakeTargetCalories > 0) {
        createProgressDoughnut(intakeCanvas, intakePercent, 'intake');
    }

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