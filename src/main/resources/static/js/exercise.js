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
let currentLocationMarker = null;
let routeSearchMarker = null;
let geolocationWatchId = null;
let routePostcodeInstance = null;

function initRouteMeasureMap() {
    const form = document.getElementById('routeQuickForm');
    const mapElement = document.getElementById('routeMap');
    const currentLocationButton = document.getElementById('routeCurrentLocationButton');
    const searchAddressButton = document.getElementById('routeSearchAddressButton');
    const resetButton = document.getElementById('routeResetButton');
    const pathInput = document.getElementById('routePathPointsJson');
    const routeDistanceKmInput = document.getElementById('routeDistanceKmInput');
    const mapProviderInput = document.getElementById('routeMapProvider');

    const routeAddressModal = document.getElementById('routeAddressModal');
    const routeAddressModalBackdrop = document.getElementById('routeAddressModalBackdrop');
    const routeAddressModalClose = document.getElementById('routeAddressModalClose');
    const routePostcodeWrap = document.getElementById('routePostcodeWrap');
    const routeAddressSelected = document.getElementById('routeAddressSelected');

    if (!form || !mapElement || !currentLocationButton || !resetButton || !routeDistanceKmInput) {
        return;
    }

    if (routeMapInitialized) {
        setTimeout(() => {
            if (routeMapInstance) {
                routeMapInstance.relayout();
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

        moveMapToCurrentLocation(routeMapInstance);

        kakao.maps.event.addListener(routeMapInstance, 'click', function (mouseEvent) {
            const latLng = mouseEvent.latLng;
            routeMarkerPositions.push(latLng);

            const marker = new kakao.maps.Marker({
                position: latLng
            });

            marker.setMap(routeMapInstance);
            routeMarkers.push(marker);

            routePolyline.setPath(routeMarkerPositions);
            syncPreview();
        });

        currentLocationButton.addEventListener('click', function () {
            moveMapToCurrentLocation(routeMapInstance);
        });

        if (searchAddressButton) {
            searchAddressButton.addEventListener('click', function () {
                openAddressSearchModal({
                    map: routeMapInstance,
                    modal: routeAddressModal,
                    wrap: routePostcodeWrap,
                    selectedBox: routeAddressSelected
                });
            });
        }

        if (routeAddressModalClose) {
            routeAddressModalClose.addEventListener('click', closeAddressSearchModal);
        }

        if (routeAddressModalBackdrop) {
            routeAddressModalBackdrop.addEventListener('click', closeAddressSearchModal);
        }

        resetButton.addEventListener('click', function () {
            routeMarkerPositions.length = 0;
            routeMarkers.forEach(marker => marker.setMap(null));
            routeMarkers.length = 0;

            if (routePolyline) {
                routePolyline.setPath([]);
            }

            if (routeSearchMarker) {
                routeSearchMarker.setMap(null);
                routeSearchMarker = null;
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
            }
        }, 80);

        function syncPreview() {
            const meters = Math.round(routePolyline.getLength());
            routeDistanceKmInput.value = meters > 0 ? (meters / 1000).toFixed(2) : '';

            if (routeMarkerPositions.length >= 2) {
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
            } else {
                if (pathInput) {
                    pathInput.value = '';
                }

                if (mapProviderInput) {
                    mapProviderInput.value = 'manual';
                }
            }
        }
    });
}

function moveMapToCurrentLocation(map) {
    const fallbackCenter = new kakao.maps.LatLng(37.5665, 126.9780);

    if (!map) {
        return;
    }

    if (!navigator.geolocation) {
        alert('이 브라우저에서는 위치 정보를 지원하지 않습니다.');
        map.setCenter(fallbackCenter);
        return;
    }

    if (geolocationWatchId !== null) {
        navigator.geolocation.clearWatch(geolocationWatchId);
        geolocationWatchId = null;
    }

    let bestPosition = null;
    let settled = false;

    const finalizePosition = (coords) => {
        if (!coords) {
            alert('현재 위치를 가져오지 못했습니다. 위치 권한을 확인해 주세요.');
            map.setCenter(fallbackCenter);
            return;
        }

        const lat = coords.latitude;
        const lng = coords.longitude;
        const accuracy = Number(coords.accuracy || 9999);
        const currentCenter = new kakao.maps.LatLng(lat, lng);

        map.setCenter(currentCenter);
        map.setLevel(3);

        renderCurrentLocationMarker(map, currentCenter);

        if (accuracy > 300) {
            alert('현재 위치 정확도가 낮습니다. PC 환경에서는 주소 검색 기능을 권장드립니다.');
        }
    };

    const settle = (coords) => {
        if (settled) {
            return;
        }

        settled = true;

        if (geolocationWatchId !== null) {
            navigator.geolocation.clearWatch(geolocationWatchId);
            geolocationWatchId = null;
        }

        finalizePosition(coords);
    };

    navigator.geolocation.getCurrentPosition(
        function (position) {
            bestPosition = position.coords;

            if (position.coords.accuracy <= 100) {
                settle(position.coords);
            }
        },
        function () {
        },
        {
            enableHighAccuracy: true,
            timeout: 8000,
            maximumAge: 0
        }
    );

    geolocationWatchId = navigator.geolocation.watchPosition(
        function (position) {
            if (!bestPosition || position.coords.accuracy < bestPosition.accuracy) {
                bestPosition = position.coords;
            }

            if (position.coords.accuracy <= 100) {
                settle(position.coords);
            }
        },
        function () {
            if (!settled) {
                settle(bestPosition);
            }
        },
        {
            enableHighAccuracy: true,
            timeout: 15000,
            maximumAge: 0
        }
    );

    setTimeout(function () {
        if (!settled) {
            settle(bestPosition);
        }
    }, 5000);
}

function renderCurrentLocationMarker(map, position) {
    if (currentLocationMarker) {
        currentLocationMarker.setMap(null);
    }

    currentLocationMarker = new kakao.maps.Marker({
        position: position
    });

    currentLocationMarker.setMap(map);
}

function openAddressSearchModal(options) {
    const map = options?.map;
    const modal = options?.modal;
    const wrap = options?.wrap;
    const selectedBox = options?.selectedBox;

    if (!map || !modal || !wrap) {
        return;
    }

    if (typeof kakao === 'undefined' || !kakao.Postcode || !kakao.maps || !kakao.maps.services) {
        alert('주소 검색 서비스를 사용할 수 없습니다.');
        return;
    }

    modal.hidden = false;

    if (selectedBox) {
        selectedBox.textContent = '주소를 검색한 뒤 항목을 선택해 주세요.';
    }

    wrap.innerHTML = '';

    routePostcodeInstance = new kakao.Postcode({
        width: '100%',
        height: '100%',
        oncomplete: function (data) {
            const fullAddress = buildFullAddress(data);

            if (selectedBox) {
                selectedBox.textContent = '선택 주소: ' + fullAddress;
            }

            moveMapToAddress(map, fullAddress, data);
            closeAddressSearchModal();
        },
        onresize: function (size) {
            wrap.style.height = size.height + 'px';
        }
    });

    routePostcodeInstance.embed(wrap);
}

function closeAddressSearchModal() {
    const modal = document.getElementById('routeAddressModal');
    const wrap = document.getElementById('routePostcodeWrap');

    if (wrap) {
        wrap.innerHTML = '';
    }

    if (modal) {
        modal.hidden = true;
    }

    routePostcodeInstance = null;
}

function buildFullAddress(data) {
    let addr = '';
    let extraAddr = '';

    if (data.userSelectedType === 'R') {
        addr = data.roadAddress || '';
    } else {
        addr = data.jibunAddress || data.address || '';
    }

    if (data.userSelectedType === 'R') {
        if (data.bname && /[동로가]$/g.test(data.bname)) {
            extraAddr += data.bname;
        }

        if (data.buildingName && data.apartment === 'Y') {
            extraAddr += (extraAddr ? ', ' + data.buildingName : data.buildingName);
        }

        if (extraAddr) {
            addr += ' (' + extraAddr + ')';
        }
    }

    return addr || data.address || '';
}

function moveMapToAddress(map, address, postcodeData) {
    if (!map || !address) {
        return;
    }

    const geocoder = new kakao.maps.services.Geocoder();

    geocoder.addressSearch(address, function (result, status) {
        if (status !== kakao.maps.services.Status.OK || !result || !result.length) {
            alert('선택한 주소의 좌표를 찾지 못했습니다.');
            return;
        }

        const first = result[0];
        const latLng = new kakao.maps.LatLng(Number(first.y), Number(first.x));

        map.setCenter(latLng);
        map.setLevel(3);

        if (routeSearchMarker) {
            routeSearchMarker.setMap(null);
        }

        routeSearchMarker = new kakao.maps.Marker({
            position: latLng
        });

        routeSearchMarker.setMap(map);

        const infoWindow = new kakao.maps.InfoWindow({
            content:
                '<div style="padding:8px 12px;font-size:12px;line-height:1.45;">' +
                '<strong>선택한 위치</strong><br>' +
                escapeHtml(address) +
                (postcodeData && postcodeData.zonecode ? '<br>우편번호: ' + escapeHtml(postcodeData.zonecode) : '') +
                '</div>'
        });

        infoWindow.open(map, routeSearchMarker);

        setTimeout(function () {
            infoWindow.close();
        }, 2500);
    });
}

function escapeHtml(value) {
    return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}