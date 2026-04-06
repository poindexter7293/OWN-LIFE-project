let exerciseBurnedChartInstance = null;
let currentExercisePeriod = 'WEEK';

document.addEventListener('DOMContentLoaded', function () {
    initCountUp();
    initExerciseModeTabs();
    initQuickTabs();
    initExercisePeriodChart();
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

            if (targetId === 'quickAddPanel' && window.routeMapInstance) {
                setTimeout(() => {
                    window.routeMapInstance.relayout();
                    window.routeMapInstance.setCenter(new kakao.maps.LatLng(37.5665, 126.9780));
                }, 120);
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

            if (targetId === 'routeQuickForm') {
                initRouteMeasureMap();
            }
        });
    });
}

function initExercisePeriodChart() {
    if (typeof Chart === 'undefined' || !window.exercisePageData) {
        return;
    }

    const chartElement = document.getElementById('exerciseBurnedChart');
    if (!chartElement) {
        return;
    }

    const periodTabs = document.querySelectorAll('.exercise-period-tab');
    currentExercisePeriod = window.exercisePageData.initialPeriod || 'WEEK';

    periodTabs.forEach((tab) => {
        tab.addEventListener('click', function () {
            const nextPeriod = this.dataset.period;
            if (!nextPeriod || nextPeriod === currentExercisePeriod) {
                return;
            }

            setActivePeriodTab(nextPeriod);
            loadExerciseChart(nextPeriod);
        });
    });

    setActivePeriodTab(currentExercisePeriod);
    loadExerciseChart(currentExercisePeriod);
}

function setActivePeriodTab(period) {
    currentExercisePeriod = period;

    const periodTabs = document.querySelectorAll('.exercise-period-tab');
    periodTabs.forEach((tab) => {
        tab.classList.toggle('active', tab.dataset.period === period);
    });

    const rangeLabel = document.getElementById('exerciseChartRangeLabel');
    if (rangeLabel) {
        if (period === 'WEEK') {
            rangeLabel.textContent = '최근 7일 기준';
        } else if (period === 'MONTH') {
            rangeLabel.textContent = '최근 30일 기준';
        } else if (period === 'YEAR') {
            rangeLabel.textContent = '최근 12개월 기준';
        }
    }
}

async function loadExerciseChart(period) {
    const loadingElement = document.getElementById('exerciseChartLoading');
    const chartApiUrl = window.exercisePageData.chartApiUrl || '/exercise/chart';
    const selectedDate = window.exercisePageData.selectedDate;
    const chartWrap = document.querySelector('.exercise-chart-wrap');

    try {
        if (chartWrap) {
            chartWrap.classList.remove('is-ready');
        }

        setChartLoading(true);

        const url = new URL(chartApiUrl, window.location.origin);
        if (selectedDate) {
            url.searchParams.set('date', selectedDate);
        }
        url.searchParams.set('period', period);

        const response = await fetch(url.toString(), {
            method: 'GET',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        if (!response.ok) {
            throw new Error('차트 데이터를 불러오지 못했습니다.');
        }

        const chartData = await response.json();
        renderExerciseChart(chartData);
    } catch (error) {
        console.error(error);

        if (exerciseBurnedChartInstance) {
            exerciseBurnedChartInstance.destroy();
            exerciseBurnedChartInstance = null;
        }

        const canvas = document.getElementById('exerciseBurnedChart');
        const container = canvas ? canvas.parentElement : null;

        if (container) {
            let errorNode = container.querySelector('.exercise-chart-error');
            if (!errorNode) {
                errorNode = document.createElement('div');
                errorNode.className = 'exercise-chart-error';
                container.appendChild(errorNode);
            }
            errorNode.textContent = '그래프 데이터를 불러오지 못했습니다.';
        }
    } finally {
        setChartLoading(false);
    }

    function setChartLoading(isLoading) {
        if (!loadingElement) {
            return;
        }
        loadingElement.hidden = !isLoading;
    }
}

function renderExerciseChart(chartData) {
    const chartElement = document.getElementById('exerciseBurnedChart');
    if (!chartElement) {
        return;
    }

    const chartWrap = chartElement.parentElement;
    const oldError = chartWrap ? chartWrap.querySelector('.exercise-chart-error') : null;
    if (oldError) {
        oldError.remove();
    }

    const labels = Array.isArray(chartData.labels) ? chartData.labels : [];
    const burnedValues = Array.isArray(chartData.burnedValues) ? chartData.burnedValues : [];
    const goalValues = Array.isArray(chartData.goalValues) ? chartData.goalValues : [];

    if (exerciseBurnedChartInstance) {
        exerciseBurnedChartInstance.destroy();
        exerciseBurnedChartInstance = null;
    }

    const hasGoalLine = goalValues.some(value => Number(value) > 0);
    const maxBurned = burnedValues.length ? Math.max(...burnedValues) : 0;
    const maxGoal = goalValues.length ? Math.max(...goalValues) : 0;
    const suggestedMax = Math.max(maxBurned, maxGoal, 100) * 1.15;

    const totalBarDuration = 700;
    const totalLineDuration = 900;
    const barDelay = burnedValues.length ? Math.max(28, Math.floor(totalBarDuration / burnedValues.length)) : 0;
    const lineDelay = goalValues.length ? Math.max(36, Math.floor(totalLineDuration / goalValues.length)) : 0;

    const datasets = [
        {
            id: 'burnedBar',
            type: 'bar',
            label: getBurnedLabel(chartData.period),
            data: burnedValues,
            backgroundColor: burnedValues.map((_, index) => {
                return index === burnedValues.length - 1
                    ? 'rgba(34, 197, 94, 1)'
                    : 'rgba(34, 197, 94, 0.18)';
            }),
            borderRadius: 10,
            borderSkipped: false,
            barPercentage: chartData.period === 'YEAR' ? 0.5 : 0.62,
            categoryPercentage: chartData.period === 'YEAR' ? 0.62 : 0.72
        }
    ];

    if (hasGoalLine) {
        datasets.push({
            id: 'goalLine',
            type: 'line',
            label: '목표 소모 칼로리',
            data: goalValues,
            borderColor: 'rgba(249, 115, 22, 0.95)',
            backgroundColor: 'rgba(249, 115, 22, 0.12)',
            borderWidth: 2.5,
            borderDash: [6, 6],
            pointRadius: 0,
            pointHoverRadius: 4,
            pointHitRadius: 10,
            fill: false,
            tension: 0,
            stepped: 'before'
        });
    }

    exerciseBurnedChartInstance = new Chart(chartElement, {
        data: {
            labels: labels,
            datasets: datasets
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
                    display: hasGoalLine
                },
                tooltip: {
                    enabled: true,
                    callbacks: {
                        label: function (context) {
                            const datasetLabel = context.dataset.label || '';
                            const value = Number(context.raw || 0);

                            if (context.dataset.id === 'burnedBar' && currentExercisePeriod === 'YEAR') {
                                return datasetLabel + ': ' + value + ' kcal/일';
                            }

                            return datasetLabel + ': ' + value + ' kcal';
                        },
                        afterBody: function (tooltipItems) {
                            if (!hasGoalLine || !tooltipItems || !tooltipItems.length) {
                                return [];
                            }

                            const index = tooltipItems[0].dataIndex;
                            const burned = Number(burnedValues[index] || 0);
                            const goal = Number(goalValues[index] || 0);

                            if (goal <= 0) {
                                return [];
                            }

                            const rate = Math.round((burned * 100) / goal);
                            return ['목표 달성률: ' + rate + '%'];
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: '#6b7280',
                        maxRotation: 0,
                        autoSkip: true
                    }
                },
                y: {
                    beginAtZero: true,
                    suggestedMax: suggestedMax,
                    grid: {
                        color: '#eef0f5'
                    },
                    ticks: {
                        color: '#6b7280'
                    }
                }
            },
            animations: {
                x: {
                    type: 'number',
                    easing: 'linear',
                    duration: function (ctx) {
                        return ctx.dataset && ctx.dataset.id === 'goalLine' ? lineDelay : 0;
                    },
                    from: NaN,
                    delay: function (ctx) {
                        if (
                            !ctx.dataset ||
                            ctx.dataset.id !== 'goalLine' ||
                            ctx.type !== 'data' ||
                            ctx.mode !== 'default'
                        ) {
                            return 0;
                        }

                        if (ctx.xStarted) {
                            return 0;
                        }

                        ctx.xStarted = true;
                        return ctx.dataIndex * lineDelay;
                    }
                },
                y: {
                    type: 'number',
                    easing: function (ctx) {
                        return ctx.dataset && ctx.dataset.id === 'burnedBar'
                            ? 'easeOutQuart'
                            : 'linear';
                    },
                    duration: function (ctx) {
                        if (!ctx.dataset) {
                            return 0;
                        }

                        if (ctx.dataset.id === 'burnedBar') {
                            return 620;
                        }

                        if (ctx.dataset.id === 'goalLine') {
                            return 0;
                        }

                        return 0;
                    },
                    from: function (ctx) {
                        if (!ctx.dataset || !ctx.chart || !ctx.chart.scales || !ctx.chart.scales.y) {
                            return undefined;
                        }

                        if (ctx.dataset.id === 'burnedBar') {
                            return ctx.chart.scales.y.getPixelForValue(0);
                        }

                        return undefined;
                    },
                    delay: function (ctx) {
                        if (
                            !ctx.dataset ||
                            ctx.dataset.id !== 'burnedBar' ||
                            ctx.type !== 'data' ||
                            ctx.mode !== 'default'
                        ) {
                            return 0;
                        }

                        if (ctx.yStarted) {
                            return 0;
                        }

                        ctx.yStarted = true;
                        return ctx.dataIndex * barDelay;
                    }
                }
            }
        }
    });

    if (chartWrap) {
        requestAnimationFrame(() => {
            chartWrap.classList.add('is-ready');
        });
    }
}

function getBurnedLabel(period) {
    if (period === 'MONTH') {
        return '일일 소모 칼로리';
    }
    if (period === 'YEAR') {
        return '월 평균 소모 칼로리';
    }
    return '일일 소모 칼로리';
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
            const confirmed = window.confirm('마이페이지로 이동하시겠습니까?');

            if (confirmed) {
                const myPageUrl = window.exercisePageData && window.exercisePageData.myPageUrl
                    ? window.exercisePageData.myPageUrl
                    : '/mypage';

                window.location.href = myPageUrl;
            }
        });
    }
}

let routeMapInstance = null;
let routePolyline = null;
let routeMarkers = [];
let routeMarkerPositions = [];
let routeMapInitialized = false;

function initRouteMeasureMap() {
    const form = document.getElementById('routeQuickForm');
    const mapElement = document.getElementById('routeMap');
    const resetButton = document.getElementById('routeResetButton');
    const pathInput = document.getElementById('routePathPointsJson');
    const routeDistanceKmInput = document.getElementById('routeDistanceKmInput');
    const mapProviderInput = document.getElementById('routeMapProvider');

    if (!form || !mapElement || !resetButton || !routeDistanceKmInput) {
        return;
    }

    if (routeMapInitialized) {
        setTimeout(() => {
            if (routeMapInstance) {
                routeMapInstance.relayout();
                routeMapInstance.setCenter(new kakao.maps.LatLng(37.5665, 126.9780));
            }
        }, 80);
        return;
    }

    if (typeof window.kakao === 'undefined' || !window.kakao.maps) {
        mapElement.innerHTML = '<div class="exercise-route-map__fallback">Kakao 지도 스크립트가 로드되지 않았습니다.</div>';
        return;
    }

    window.kakao.maps.load(function () {
        routeMapInstance = new kakao.maps.Map(mapElement, {
            center: new kakao.maps.LatLng(37.5665, 126.9780),
            level: 5
        });

        window.routeMapInstance = routeMapInstance;

        routePolyline = new kakao.maps.Polyline({
            path: [],
            strokeWeight: 5,
            strokeColor: '#5b3df5',
            strokeOpacity: 0.85,
            strokeStyle: 'solid'
        });
        routePolyline.setMap(routeMapInstance);

        kakao.maps.event.addListener(routeMapInstance, 'click', function (mouseEvent) {
            const latLng = mouseEvent.latLng;
            routeMarkerPositions.push(latLng);

            const marker = new kakao.maps.Marker({
                position: latLng
            });

            marker.setMap(routeMapInstance);
            routeMarkers.push(marker);

            routePolyline.setPath(routeMarkerPositions);

            // 2개 이상 점일 때 자동 거리 계산
            if (routeMarkerPositions.length >= 2) {
                const meters = Math.round(routePolyline.getLength());
                const km = meters > 0 ? (meters / 1000).toFixed(2) : '';

                routeDistanceKmInput.value = km;

                const path = routeMarkerPositions.map((point) => ({
                    lat: Number(point.getLat().toFixed(7)),
                    lng: Number(point.getLng().toFixed(7))
                }));

                if (pathInput) {
                    pathInput.value = JSON.stringify(path);
                }

                if (mapProviderInput) {
                    mapProviderInput.value = 'kakao';
                }
            }
        });


        resetButton.addEventListener('click', function () {
            routeMarkerPositions.length = 0;
            routeMarkers.forEach(marker => marker.setMap(null));
            routeMarkers.length = 0;

            if (routePolyline) {
                routePolyline.setPath([]);
            }

            routeDistanceKmInput.value = '';

            if (pathInput) {
                pathInput.value = '';
            }

            if (mapProviderInput) {
                mapProviderInput.value = 'manual';
            }
        });

        form.addEventListener('submit', function (event) {
            if (!routeDistanceKmInput.value || Number(routeDistanceKmInput.value) <= 0) {
                event.preventDefault();
                alert('거리(km)를 입력해 주세요.');
                return;
            }

            if (mapProviderInput && !pathInput.value) {
                mapProviderInput.value = 'manual';
            }
        });

        routeMapInitialized = true;

        setTimeout(() => {
            if (routeMapInstance) {
                routeMapInstance.relayout();
                routeMapInstance.setCenter(new kakao.maps.LatLng(37.5665, 126.9780));
            }
        }, 80);

        function syncPreview() {
            const meters = Math.round(routePolyline.getLength());
            routeDistanceKmInput.value = meters > 0 ? (meters / 1000).toFixed(2) : '';
        }
    });
}