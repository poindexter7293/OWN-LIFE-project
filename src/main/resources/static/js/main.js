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
        const aiCoachChat = document.getElementById('aiCoachChat');
        const quickButtons = document.querySelectorAll('[data-ai-type]');

        if (!aiCoachWrap || !aiCoachFab || !aiCoachPanel || !aiCoachClose) {
            return;
        }

        aiCoachFab.addEventListener('click', function () {
            aiCoachPanel.classList.toggle('hidden');
        });

        aiCoachClose.addEventListener('click', function () {
            aiCoachPanel.classList.add('hidden');
        });

        document.addEventListener('click', function (e) {
            if (!aiCoachWrap.contains(e.target)) {
                aiCoachPanel.classList.add('hidden');
            }
        });

        quickButtons.forEach((button) => {
            button.addEventListener('click', async function () {
                const type = button.dataset.aiType;
                const label = button.textContent.trim();

                if (!aiCoachChat) {
                    return;
                }

                const userBubble = document.createElement('div');
                userBubble.className = 'ai-chat-bubble user';
                userBubble.innerHTML = `
                    <div class="ai-chat-name">나</div>
                    <div class="ai-chat-text">${label}</div>
                `;
                aiCoachChat.appendChild(userBubble);

                const loadingBubble = document.createElement('div');
                loadingBubble.className = 'ai-chat-bubble ai';
                loadingBubble.innerHTML = `
                    <div class="ai-chat-name">OWN 트레이너</div>
                    <div class="ai-chat-text">기록을 바탕으로 정리하고 있어요...</div>
                `;
                aiCoachChat.appendChild(loadingBubble);

                aiCoachChat.scrollTop = aiCoachChat.scrollHeight;

                try {
                    const response = await fetch(`/api/ai-coach/recommend?type=${type}`);
                    const payload = await response.json();

                    let listHtml = '';
                    if (payload.messages && payload.messages.length > 0) {
                        listHtml = '<ul class="ai-chat-list">';
                        payload.messages.forEach((message) => {
                            listHtml += `<li>${message}</li>`;
                        });
                        listHtml += '</ul>';
                    }

                    loadingBubble.innerHTML = `
                        <div class="ai-chat-name">OWN 트레이너</div>
                        <div class="ai-chat-text">
                            <strong>${payload.title}</strong><br>
                            ${payload.summary}
                            ${listHtml}
                        </div>
                    `;
                } catch (error) {
                    console.error('AI coach fetch error:', error);
                    loadingBubble.innerHTML = `
                        <div class="ai-chat-name">OWN 트레이너</div>
                        <div class="ai-chat-text">추천을 불러오는 중 문제가 발생했습니다.</div>
                    `;
                }

                aiCoachChat.scrollTop = aiCoachChat.scrollHeight;
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
    const summaryMixedCanvas = document.getElementById('summaryMixedChart');
    const burnedCanvas = document.getElementById('burnedProgressChart');
    const intakeCanvas = document.getElementById('intakeProgressChart');
    const macroCanvas = document.getElementById('macroChart');

    const hasDashboardDom = !!(weightCanvas || summaryMixedCanvas || burnedCanvas || intakeCanvas || macroCanvas);

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

    const createLegacyWeightChart = () => {
        if (!weightCanvas || summaryMixedCanvas) return;

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
    };

    const setupSummaryMixedChart = () => {
        if (!summaryMixedCanvas) return;

        const summaryChartData = data.summaryChart || {};
        let currentRange = 'week';

        const titleEl = document.getElementById('summaryMixedChartTitle');
        const rangeButtons = document.querySelectorAll('[data-summary-range]');
        const toggleInputs = document.querySelectorAll('[data-series-key]');

        const seriesState = {
            weightKg: true,
            totalEatKcal: true,
            totalBurnedKcal: true,
            totalCarbG: false,
            totalProteinG: false,
            totalFatG: false,
            goalEatKcal: true,
            goalBurnedKcal: true,
            goalWeight: true
        };

        const rangeLabelMap = {
            week: '주간',
            month: '월간',
            year: '연간'
        };

        const datasetDefs = [
            {
                key: 'weightKg',
                label: '체중',
                type: 'line',
                yAxisID: 'yWeight',
                borderColor: '#111111',
                backgroundColor: '#111111',
                pointBackgroundColor: '#111111',
                pointBorderColor: '#111111',
                pointRadius: 3,
                pointHoverRadius: 5,
                borderWidth: 2,
                fill: false,
                tension: 0.28,
                order: 2
            },
            {
                key: 'totalEatKcal',
                label: '섭취칼로리',
                type: 'bar',
                yAxisID: 'yKcal',
                backgroundColor: 'rgba(245, 160, 0, 0.72)',
                borderColor: '#f5a000',
                order: 4
            },
            {
                key: 'totalBurnedKcal',
                label: '소모칼로리',
                type: 'bar',
                yAxisID: 'yKcal',
                backgroundColor: 'rgba(91, 61, 245, 0.72)',
                borderColor: '#5b3df5',
                order: 4
            },
            {
                key: 'totalCarbG',
                label: '탄수화물',
                type: 'bar',
                yAxisID: 'yGram',
                backgroundColor: 'rgba(76, 87, 232, 0.72)',
                borderColor: '#4c57e8',
                order: 4
            },
            {
                key: 'totalProteinG',
                label: '단백질',
                type: 'bar',
                yAxisID: 'yGram',
                backgroundColor: 'rgba(34, 197, 94, 0.72)',
                borderColor: '#22c55e',
                order: 4
            },
            {
                key: 'totalFatG',
                label: '지방',
                type: 'bar',
                yAxisID: 'yGram',
                backgroundColor: 'rgba(245, 160, 0, 0.72)',
                borderColor: '#f5a000',
                order: 4
            },
            {
                key: 'goalEatKcal',
                label: '목표섭취',
                type: 'line',
                yAxisID: 'yKcal',
                borderColor: '#f5a000',
                backgroundColor: '#f5a000',
                stepped: true,
                pointRadius: 0,
                pointHoverRadius: 3,
                borderWidth: 2,
                fill: false,
                tension: 0,
                order: 1
            },
            {
                key: 'goalBurnedKcal',
                label: '목표소모',
                type: 'line',
                yAxisID: 'yKcal',
                borderColor: '#5b3df5',
                backgroundColor: '#5b3df5',
                stepped: true,
                pointRadius: 0,
                pointHoverRadius: 3,
                borderWidth: 2,
                fill: false,
                tension: 0,
                order: 1
            },
            {
                key: 'goalWeight',
                label: '목표체중',
                type: 'line',
                yAxisID: 'yWeight',
                borderColor: '#ef4444',
                backgroundColor: '#ef4444',
                stepped: true,
                pointRadius: 0,
                pointHoverRadius: 3,
                borderWidth: 2,
                fill: false,
                tension: 0,
                order: 2
            }
        ];

        const getRangePayload = (rangeKey) => {
            return summaryChartData[rangeKey] || {
                labels: [],
                weightKg: [],
                totalEatKcal: [],
                totalBurnedKcal: [],
                totalCarbG: [],
                totalProteinG: [],
                totalFatG: [],
                goalEatKcal: [],
                goalBurnedKcal: [],
                goalWeight: []
            };
        };

        const buildDatasets = (rangeKey) => {
            const payload = getRangePayload(rangeKey);

            return datasetDefs.map((def) => ({
                ...def,
                data: payload[def.key] || [],
                hidden: !seriesState[def.key],
                borderRadius: def.type === 'bar' ? 6 : 0,
                borderSkipped: def.type === 'bar' ? false : undefined,
                maxBarThickness: def.type === 'bar'
                    ? (rangeKey === 'year' ? 30 : 22)
                    : undefined
            }));
        };

        const chart = new Chart(summaryMixedCanvas, {
            data: {
                labels: getRangePayload(currentRange).labels || [],
                datasets: buildDatasets(currentRange)
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        enabled: true
                    }
                },
                scales: {
                    x: {
                        stacked: false,
                        grid: {
                            color: '#f3f4f6'
                        },
                        ticks: {
                            color: '#9ca3af'
                        }
                    },
                    yKcal: {
                        type: 'linear',
                        position: 'left',
                        grid: {
                            color: '#eef2f7'
                        },
                        ticks: {
                            color: '#6b7280'
                        },
                        title: {
                            display: true,
                            text: 'kcal'
                        }
                    },
                    yGram: {
                        type: 'linear',
                        position: 'right',
                        display: false,
                        grid: {
                            drawOnChartArea: false
                        }
                    },
                    yWeight: {
                        type: 'linear',
                        position: 'right',
                        offset: true,
                        grid: {
                            drawOnChartArea: false
                        },
                        ticks: {
                            color: '#6b7280'
                        },
                        title: {
                            display: true,
                            text: 'kg'
                        }
                    }
                }
            }
        });

        const render = () => {
            const payload = getRangePayload(currentRange);

            chart.data.labels = payload.labels || [];
            chart.data.datasets = buildDatasets(currentRange);
            chart.update();

            if (titleEl) {
                titleEl.textContent = `${rangeLabelMap[currentRange]} 통합 그래프`;
            }

            rangeButtons.forEach((btn) => {
                btn.classList.toggle('is-active', btn.dataset.summaryRange === currentRange);
            });
        };

        rangeButtons.forEach((btn) => {
            btn.addEventListener('click', () => {
                currentRange = btn.dataset.summaryRange;
                render();
            });
        });

        toggleInputs.forEach((input) => {
            const key = input.dataset.seriesKey;
            if (!(key in seriesState)) return;

            input.checked = seriesState[key];

            input.addEventListener('change', () => {
                seriesState[key] = input.checked;
                render();
            });
        });

        render();
    };

    createLegacyWeightChart();
    setupSummaryMixedChart();

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