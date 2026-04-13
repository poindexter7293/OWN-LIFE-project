document.addEventListener("DOMContentLoaded", function () {
    const modeRadios = document.querySelectorAll('input[name="inputMode"]');
    const selectModeFields = document.getElementById("selectModeFields");
    const customModeFields = document.getElementById("customModeFields");
    const form = document.querySelector(".diet-form");

    const foodSelect = document.getElementById("foodId");
    const foodSearchKeyword = document.getElementById("foodSearchKeyword");
    const foodSearchResultText = document.getElementById("foodSearchResultText");

    const selectedBaseAmountG = document.getElementById("selectedBaseAmountG");
    const selectedCaloriesKcal = document.getElementById("selectedCaloriesKcal");
    const selectedCarbG = document.getElementById("selectedCarbG");
    const selectedProteinG = document.getElementById("selectedProteinG");
    const selectedFatG = document.getElementById("selectedFatG");

    const addSelectedFoodBtn = document.getElementById("addSelectedFoodBtn");
    const selectedFoodList = document.getElementById("selectedFoodList");
    const selectedFoodCount = document.getElementById("selectedFoodCount");
    const selectedFoodsJson = document.getElementById("selectedFoodsJson");

    let selectedFoods = [];
    let dietIntakeChart = null;
    let macroRatioChart = null;
    let lineBlinkFrameId = null;

    if (!modeRadios.length || !selectModeFields || !customModeFields || !form) {
        return;
    }

    function toggleDietInputMode() {
        const selectedMode = document.querySelector('input[name="inputMode"]:checked')?.value;

        if (selectedMode === "custom") {
            selectModeFields.style.display = "none";
            customModeFields.style.display = "block";
        } else {
            selectModeFields.style.display = "block";
            customModeFields.style.display = "none";
        }
    }

    function fillSelectedFoodInfo() {
        if (!foodSelect) return;

        const selectedOption = foodSelect.options[foodSelect.selectedIndex];

        if (!selectedOption || !selectedOption.value) {
            selectedBaseAmountG.value = "";
            selectedCaloriesKcal.value = "";
            selectedCarbG.value = "";
            selectedProteinG.value = "";
            selectedFatG.value = "";
            return;
        }

        selectedBaseAmountG.value = selectedOption.dataset.baseAmount || "";
        selectedCaloriesKcal.value = selectedOption.dataset.calories || "";
        selectedCarbG.value = selectedOption.dataset.carb || "";
        selectedProteinG.value = selectedOption.dataset.protein || "";
        selectedFatG.value = selectedOption.dataset.fat || "";
    }

    function animateCountUp() {
        const counters = document.querySelectorAll(".count-up");

        counters.forEach((counter) => {
            const target = parseFloat(counter.dataset.value || "0");
            const duration = 900;
            const startTime = performance.now();

            function update(currentTime) {
                const elapsed = currentTime - startTime;
                const progress = Math.min(elapsed / duration, 1);
                const easeOut = 1 - Math.pow(1 - progress, 3);
                const currentValue = target * easeOut;

                counter.textContent = currentValue.toFixed(1);

                if (progress < 1) {
                    requestAnimationFrame(update);
                } else {
                    counter.textContent = target.toFixed(1);
                }
            }

            requestAnimationFrame(update);
        });
    }

    function getBarColors(calories, goalSeries) {
        return calories.map((value, index) => {
            const goal = Number(goalSeries[index] || 0);
            return goal > 0 && Number(value) > goal ? "#ef4444" : "#22c55e";
        });
    }

    function getBarHoverColors(calories, goalSeries) {
        return calories.map((value, index) => {
            const goal = Number(goalSeries[index] || 0);
            return goal > 0 && Number(value) > goal ? "#dc2626" : "#16a34a";
        });
    }

    /**
     * 막대만 올라오게 하고, 선형은 생성 시 애니메이션을 완전히 제거
     */
    function createBarOnlyAnimations() {
        return {
            y: {
                easing: "easeOutCubic",
                duration: 850,
                from(ctx) {
                    if (ctx.type !== "data") return;

                    const dataset = ctx.chart.data.datasets[ctx.datasetIndex];
                    if (dataset.type === "line") {
                        return ctx.chart.scales.y.getPixelForValue(ctx.parsed.y);
                    }

                    return ctx.chart.scales.y.getPixelForValue(0);
                },
                delay(ctx) {
                    if (ctx.type !== "data") return 0;

                    const dataset = ctx.chart.data.datasets[ctx.datasetIndex];
                    if (dataset.type === "line") {
                        return 0;
                    }

                    if (ctx.yStarted) return 0;
                    ctx.yStarted = true;
                    return ctx.dataIndex * 90;
                },
                duration(ctx) {
                    if (ctx.type !== "data") return 0;

                    const dataset = ctx.chart.data.datasets[ctx.datasetIndex];
                    return dataset.type === "line" ? 0 : 850;
                }
            },
            x: {
                duration(ctx) {
                    if (ctx.type !== "data") return 0;

                    const dataset = ctx.chart.data.datasets[ctx.datasetIndex];
                    return dataset.type === "line" ? 0 : 0;
                }
            },
            base: {
                easing: "easeOutCubic",
                from(ctx) {
                    if (ctx.type !== "data") return;

                    const dataset = ctx.chart.data.datasets[ctx.datasetIndex];
                    if (dataset.type === "line") {
                        return ctx.chart.scales.y.getPixelForValue(ctx.parsed.y);
                    }

                    return ctx.chart.scales.y.getPixelForValue(0);
                },
                delay(ctx) {
                    if (ctx.type !== "data") return 0;

                    const dataset = ctx.chart.data.datasets[ctx.datasetIndex];
                    if (dataset.type === "line") {
                        return 0;
                    }

                    if (ctx.baseStarted) return 0;
                    ctx.baseStarted = true;
                    return ctx.dataIndex * 90;
                },
                duration(ctx) {
                    if (ctx.type !== "data") return 0;

                    const dataset = ctx.chart.data.datasets[ctx.datasetIndex];
                    return dataset.type === "line" ? 0 : 850;
                }
            }
        };
    }
    function createLineRevealBlinkPlugin() {
        return {
            id: "lineRevealBlinkPlugin",
            afterDatasetsDraw(chart, args, pluginOptions) {
                if (!pluginOptions || !pluginOptions.enabled) return;

                const datasetIndex = chart.data.datasets.findIndex(dataset => dataset.type === "line");
                if (datasetIndex === -1) return;

                const meta = chart.getDatasetMeta(datasetIndex);
                if (!meta || meta.hidden || !meta.dataset) return;

                const ctx = chart.ctx;
                const area = chart.chartArea;
                const now = performance.now();

                if (!chart.$lineRevealBlinkState) {
                    chart.$lineRevealBlinkState = {
                        start: now
                    };
                }

                const state = chart.$lineRevealBlinkState;
                const duration = pluginOptions.duration || 1200;
                const elapsed = now - state.start;
                const progress = Math.min(elapsed / duration, 1);

                const eased = 1 - Math.pow(1 - progress, 3);

                const revealX = area.left + (area.right - area.left) * eased;

                // 1~2번만 약하게 반짝이도록 조정
                const blinkCycles = pluginOptions.blinkCycles || 2;
                const blinkStrength = pluginOptions.blinkStrength || 0.22;

                let alpha = 1;
                if (progress < 1) {
                    alpha = 1 - (Math.sin(progress * Math.PI * blinkCycles) ** 2) * blinkStrength;
                }

                ctx.save();
                ctx.beginPath();
                ctx.rect(area.left, area.top, revealX - area.left, area.bottom - area.top);
                ctx.clip();

                ctx.globalAlpha = alpha;

                meta.dataset.draw(ctx);

                if (Array.isArray(meta.data)) {
                    meta.data.forEach(point => {
                        if (point.x <= revealX) {
                            point.draw(ctx);
                        }
                    });
                }

                ctx.restore();

                if (progress < 1) {
                    requestAnimationFrame(() => chart.draw());
                }
            }
        };
    }




    function renderDietIntakeChart(period = "day") {
        const chartCanvas = document.getElementById("dietIntakeChart");
        if (!chartCanvas || !window.dietChartDataMap) return;

        const chartData = window.dietChartDataMap[period];
        if (!chartData) return;

        const { labels, calories, goalKcal, goalKcalSeries } = chartData;

        const resolvedGoalSeries = Array.isArray(goalKcalSeries) && goalKcalSeries.length === labels.length
            ? goalKcalSeries
            : labels.map(() => goalKcal);

        const ctx = chartCanvas.getContext("2d");

        const barColors = getBarColors(calories, resolvedGoalSeries);
        const barHoverColors = getBarHoverColors(calories, resolvedGoalSeries);

        if (dietIntakeChart) {
            dietIntakeChart.destroy();
            dietIntakeChart = null;
        }

        const lineRevealBlinkPlugin = createLineRevealBlinkPlugin();

        dietIntakeChart = new Chart(ctx, {
            type: "bar",
            data: {
                labels,
                datasets: [
                    {
                        label: "섭취 칼로리",
                        data: calories,
                        backgroundColor: barColors,
                        hoverBackgroundColor: barHoverColors,
                        borderRadius: 10,
                        borderSkipped: false,
                        maxBarThickness: 34,
                        order: 2
                    },
                    {
                        type: "line",
                        label: "목표 칼로리",
                        data: resolvedGoalSeries,
                        borderColor: "#f59e0b",
                        borderWidth: 3,
                        borderDash: [8, 6],
                        borderCapStyle: "butt",
                        borderJoinStyle: "miter",
                        pointRadius: 0,
                        pointHoverRadius: 0,
                        pointHitRadius: 8,
                        pointBackgroundColor: "#f59e0b",
                        pointBorderColor: "#f59e0b",
                        fill: false,
                        tension: 0,
                        stepped: "after",
                        order: 1,
                        animations: {
                            x: { duration: 0 },
                            y: { duration: 0 }
                        }
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animations: createBarOnlyAnimations(),
                plugins: {
                    legend: {
                        display: false,
                        labels: {
                            usePointStyle: true,
                            boxWidth: 10,
                            font: {
                                size: 12,
                                weight: "600"
                            }
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (context) {
                                return `${context.dataset.label}: ${context.parsed.y} kcal`;
                            }
                        }
                    },
                    lineRevealBlinkPlugin: {
                        enabled: true,
                        duration: 1250,
                        blinkCycles: 2,
                        blinkStrength: 0.18
                    }
                },
                interaction: {
                    mode: "index",
                    intersect: false
                },
                scales: {
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            color: "#6f7d95"
                        }
                    },
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 200,
                            color: "#6f7d95",
                            callback: function (value) {
                                return value + " kcal";
                            }
                        },
                        grid: {
                            color: "rgba(111, 125, 149, 0.12)"
                        }
                    }
                }
            },
            plugins: [lineRevealBlinkPlugin]
        });
    }

    function updateDietChart(period) {
        if (!window.dietChartDataMap || !window.dietChartDataMap[period]) return;

        renderDietIntakeChart(period);
        updateDietChartTitle(period);
        updateDietChartSubText(period);
    }

    function updateDietChartTitle(period) {
        const titleEl = document.getElementById("dietChartTitle");
        if (!titleEl) return;

        if (period === "day") {
            titleEl.textContent = "최근 7일 섭취량";
        } else if (period === "week") {
            titleEl.textContent = "주간 평균 섭취량";
        } else if (period === "month") {
            titleEl.textContent = "월간 평균 섭취량";
        } else if (period === "year") {
            titleEl.textContent = "연간 평균 섭취량";
        }
    }

    function updateDietChartSubText(period) {
        const subTextEl = document.getElementById("dietChartSubText");
        if (!subTextEl) return;

        if (period === "day") {
            subTextEl.textContent = "선택한 기간 기준 일별 섭취 칼로리를 보여줍니다.";
        } else if (period === "week") {
            subTextEl.textContent = "각 주차별 기록일 기준 평균 섭취 칼로리를 보여줍니다.";
        } else if (period === "month") {
            subTextEl.textContent = "각 월별 기록일 기준 평균 섭취 칼로리를 보여줍니다.";
        } else if (period === "year") {
            subTextEl.textContent = "각 연도별 기록일 기준 평균 섭취 칼로리를 보여줍니다.";
        }
    }

    function bindDietChartTabs() {
        const tabButtons = document.querySelectorAll(".chart-tab");
        if (!tabButtons.length) return;

        tabButtons.forEach((button) => {
            button.addEventListener("click", function () {
                const period = this.dataset.period;

                tabButtons.forEach((btn) => btn.classList.remove("active"));
                this.classList.add("active");

                updateDietChart(period);
            });
        });
    }

    function renderMacroRatioChart(data = window.macroRatioData) {
        const chartCanvas = document.getElementById("macroRatioChart");
        if (!chartCanvas) return;

        const safeData = data || { carb: 0, protein: 0, fat: 0 };

        const carb = Number(safeData.carb || 0);
        const protein = Number(safeData.protein || 0);
        const fat = Number(safeData.fat || 0);
        const total = carb + protein + fat;
        const ctx = chartCanvas.getContext("2d");

        const centerTextPlugin = {
            id: "centerTextPlugin",
            afterDraw(chart) {
                const meta = chart.getDatasetMeta(0);
                if (!meta || !meta.data || !meta.data.length) return;

                const x = meta.data[0].x;
                const y = meta.data[0].y;

                ctx.save();
                ctx.textAlign = "center";
                ctx.textBaseline = "middle";

                ctx.fillStyle = "#6f7d95";
                ctx.font = "600 13px sans-serif";
                ctx.fillText("총 섭취", x, y - 12);

                ctx.fillStyle = "#203864";
                ctx.font = "800 24px sans-serif";
                ctx.fillText(`${total.toFixed(1)}g`, x, y + 14);

                ctx.restore();
            }
        };

        const isEmpty = total === 0;
        const chartValues = isEmpty ? [1, 1, 1] : [carb, protein, fat];
        const chartColors = isEmpty
            ? ["#eef3f9", "#eef3f9", "#eef3f9"]
            : ["#4c57e8", "#22c55e", "#f5a000"];

        if (macroRatioChart) {
            macroRatioChart.destroy();
            macroRatioChart = null;
        }

        macroRatioChart = new Chart(ctx, {
            type: "doughnut",
            data: {
                labels: ["탄수화물", "단백질", "지방"],
                datasets: [{
                    data: chartValues,
                    backgroundColor: chartColors,
                    borderWidth: 0,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "72%",
                rotation: -90,
                animation: {
                    duration: 1200,
                    easing: "easeOutCubic",
                    animateRotate: true,
                    animateScale: false
                },
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        enabled: !isEmpty,
                        callbacks: {
                            label: function (context) {
                                return `${context.label}: ${context.parsed.toFixed(1)}g`;
                            }
                        }
                    }
                }
            },
            plugins: [centerTextPlugin]
        });
    }

    function filterFoodOptions() {
        if (!foodSelect || !foodSearchKeyword) return;

        const keyword = foodSearchKeyword.value.trim().toLowerCase();
        const options = Array.from(foodSelect.options);

        let visibleCount = 0;
        let firstMatchedOption = null;

        options.forEach((option, index) => {
            if (index === 0) {
                option.hidden = false;
                return;
            }

            const foodName = (option.textContent || "").toLowerCase();
            const isMatch = keyword === "" || foodName.includes(keyword);

            option.hidden = !isMatch;

            if (isMatch) {
                visibleCount++;
                if (!firstMatchedOption) {
                    firstMatchedOption = option;
                }
            }
        });

        if (keyword !== "") {
            if (visibleCount > 0 && firstMatchedOption) {
                foodSelect.value = firstMatchedOption.value;
                fillSelectedFoodInfo();
                if (foodSearchResultText) {
                    foodSearchResultText.textContent = `${visibleCount}개 검색됨`;
                }
            } else {
                foodSelect.value = "";
                fillSelectedFoodInfo();
                if (foodSearchResultText) {
                    foodSearchResultText.textContent = "검색 결과가 없습니다.";
                }
            }
        } else {
            foodSelect.value = "";
            fillSelectedFoodInfo();
            if (foodSearchResultText) {
                foodSearchResultText.textContent = "검색어를 입력하면 목록이 줄어듭니다.";
            }
        }
    }

    function getSelectedFoodData() {
        if (!foodSelect) return null;

        const selectedOption = foodSelect.options[foodSelect.selectedIndex];
        if (!selectedOption || !selectedOption.value) return null;

        return {
            foodId: selectedOption.value,
            foodName: selectedOption.textContent.trim(),
            baseAmountG: parseFloat(selectedOption.dataset.baseAmount || "0"),
            caloriesKcal: parseFloat(selectedOption.dataset.calories || "0"),
            carbG: parseFloat(selectedOption.dataset.carb || "0"),
            proteinG: parseFloat(selectedOption.dataset.protein || "0"),
            fatG: parseFloat(selectedOption.dataset.fat || "0"),
            count: 1.0
        };
    }

    function renderSelectedFoods() {
        if (!selectedFoodList || !selectedFoodCount || !selectedFoodsJson) return;

        selectedFoodCount.textContent = `${selectedFoods.length}개`;
        selectedFoodsJson.value = JSON.stringify(selectedFoods);

        if (selectedFoods.length === 0) {
            selectedFoodList.innerHTML = `<div class="selected-food-empty">아직 담긴 음식이 없습니다.</div>`;
            return;
        }

        selectedFoodList.innerHTML = selectedFoods.map((food, index) => {
            const totalKcal = (food.caloriesKcal * food.count).toFixed(1);

            return `
            <div class="selected-food-item">
                <div class="selected-food-name">${food.foodName}</div>
                <input type="number"
                       min="0.5"
                       step="0.5"
                       value="${food.count}"
                       class="selected-food-amount"
                       data-index="${index}">
                <div class="selected-food-kcal">${totalKcal} kcal</div>
                <button type="button"
                        class="remove-selected-food-btn"
                        data-remove-index="${index}">삭제</button>
            </div>
        `;
        }).join("");
    }

    function addSelectedFood() {
        const selectedFood = getSelectedFoodData();

        if (!selectedFood) {
            alert("음식을 먼저 선택해 주세요.");
            return;
        }

        const existsIndex = selectedFoods.findIndex(food => String(food.foodId) === String(selectedFood.foodId));

        if (existsIndex !== -1) {
            selectedFoods[existsIndex].count += 0.5;
        } else {
            selectedFoods.push(selectedFood);
        }

        renderSelectedFoods();
    }

    function bindSelectedFoodEvents() {
        if (!selectedFoodList) return;

        selectedFoodList.addEventListener("click", function (e) {
            const removeIndex = e.target.dataset.removeIndex;
            if (removeIndex === undefined) return;

            selectedFoods.splice(Number(removeIndex), 1);
            renderSelectedFoods();
        });

        selectedFoodList.addEventListener("input", function (e) {
            if (!e.target.classList.contains("selected-food-amount")) return;

            const index = Number(e.target.dataset.index);
            const value = parseFloat(e.target.value || "1");

            selectedFoods[index].count = value > 0 ? value : 1.0;
            renderSelectedFoods();
        });
    }

    function bindGoalCard() {
        const goalCard = document.getElementById("goal-card");
        if (!goalCard) return;

        const moveToGoalSetting = function (event) {
            if (event) {
                event.preventDefault();
                event.stopPropagation();
            }

            const confirmMove = window.confirm("목표 섭취 칼로리를 수정하시겠습니까?");
            if (!confirmMove) {
                return;
            }

            const myPageUrl = goalCard.dataset.mypageUrl || "/mypage";
            const targetUrl = new URL(myPageUrl, window.location.origin);
            targetUrl.searchParams.set("focus", "goalEatKcal");

            window.location.href = targetUrl.toString();
        };

        goalCard.addEventListener("click", moveToGoalSetting);
        goalCard.addEventListener("keydown", function (event) {
            if (event.key !== "Enter" && event.key !== " ") return;
            moveToGoalSetting(event);
        });
    }

    function bindIntakeCard() {
        const intakeCard = document.getElementById("intake-card");
        const inputBox = document.getElementById("diet-input-box");

        if (!intakeCard || !inputBox) return;

        intakeCard.addEventListener("click", (e) => {
            e.stopPropagation();
            inputBox.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        });
    }

    function bindMealGroupDeleteButtons() {
        document.addEventListener("click", async function (e) {
            const btn = e.target.closest(".meal-group-delete-btn");
            if (!btn) return;

            const mealType = btn.dataset.mealType;
            const date = btn.dataset.date;

            const ok = confirm("이 식사 항목 전체를 삭제하시겠습니까?");
            if (!ok) return;

            try {
                const response = await fetch("/diet/delete-group", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    body: new URLSearchParams({
                        mealType: mealType,
                        date: date
                    })
                });

                if (!response.ok) {
                    alert("삭제 중 오류가 발생했습니다.");
                    return;
                }

                const data = await response.json();
                refreshDietUI(data);

            } catch (error) {
                console.error(error);
                alert("삭제 중 오류가 발생했습니다.");
            }
        });
    }

    function renderGoalSummary(goalSummary) {
        const goalEl = document.querySelector(".kcal-goal");
        const intakeEl = document.querySelector(".kcal-intake");
        const remainingEl = document.querySelector(".kcal-remaining");

        if (!goalEl || !intakeEl || !remainingEl || !goalSummary) return;

        goalEl.dataset.value = Number(goalSummary.goalKcal || 0).toFixed(1);
        intakeEl.dataset.value = Number(goalSummary.totalKcal || 0).toFixed(1);
        remainingEl.dataset.value = Number(goalSummary.remainingKcal || 0).toFixed(1);

        if (Number(goalSummary.exceededKcal || 0) > 0) {
            remainingEl.classList.add("kcal-over");
        } else {
            remainingEl.classList.remove("kcal-over");
        }

        animateCountUp();
    }

    function renderMacroSummary(dailySummary) {
        if (!dailySummary) return;

        const macroItems = document.querySelectorAll(".macro-legend .macro-item strong");
        if (macroItems.length < 3) return;

        macroItems[0].textContent = `${Number(dailySummary.carb || 0).toFixed(1)}g`;
        macroItems[1].textContent = `${Number(dailySummary.protein || 0).toFixed(1)}g`;
        macroItems[2].textContent = `${Number(dailySummary.fat || 0).toFixed(1)}g`;
    }

    function renderMealGroups(mealGroups, selectedDate) {
        const mealSummaryList = document.getElementById("mealSummaryList");
        const mealSummaryEmpty = document.getElementById("mealSummaryEmpty");
        const listContainer = mealSummaryList
            ? mealSummaryList.parentElement
            : mealSummaryEmpty
                ? mealSummaryEmpty.parentElement
                : null;

        if (!listContainer) return;

        if (mealSummaryList) {
            mealSummaryList.remove();
        }

        if (mealSummaryEmpty) {
            mealSummaryEmpty.style.display = "none";
        }

        if (!mealGroups || mealGroups.length === 0) {
            if (mealSummaryEmpty) {
                mealSummaryEmpty.style.display = "block";
            } else {
                const emptyBox = document.createElement("div");
                emptyBox.className = "empty-box";
                emptyBox.id = "mealSummaryEmpty";
                emptyBox.textContent = "등록된 식단이 없습니다.";
                listContainer.appendChild(emptyBox);
            }
            return;
        }

        const listEl = document.createElement("div");
        listEl.className = "meal-summary-list";
        listEl.id = "mealSummaryList";

        listEl.innerHTML = mealGroups.map(group => {
            const foodNames = (group.items || []).map(item => item.foodName).join(", ");
            const subtotalKcal = Number(group.subtotalKcal || 0).toFixed(1);

            return `
            <div class="meal-summary-item">
                <div class="meal-summary-line">
                    <span class="meal-summary-type">${group.mealTypeLabel}:</span>
                    <span class="meal-summary-foods">${foodNames}</span>
                    <span class="meal-summary-total">
                        소계:
                        <span>${subtotalKcal}</span>
                        kcal
                    </span>
                    <button type="button"
                            class="meal-group-delete-btn"
                            data-meal-type="${group.mealType}"
                            data-date="${selectedDate}">
                        삭제
                    </button>
                </div>
            </div>
        `;
        }).join("");

        listContainer.appendChild(listEl);
    }

    function refreshChartData(charts, dailySummary) {
        if (charts) {
            window.dietChartDataMap = {
                day: charts.day || { labels: [], calories: [], goalKcal: 0, goalKcalSeries: [] },
                week: charts.week || { labels: [], calories: [], goalKcal: 0, goalKcalSeries: [] },
                month: charts.month || { labels: [], calories: [], goalKcal: 0, goalKcalSeries: [] },
                year: charts.year || { labels: [], calories: [], goalKcal: 0, goalKcalSeries: [] }
            };
        }

        if (dailySummary) {
            window.macroRatioData = {
                carb: Number(dailySummary.carb || 0),
                protein: Number(dailySummary.protein || 0),
                fat: Number(dailySummary.fat || 0)
            };
        }
    }

    function getActiveChartPeriod() {
        const activeTab = document.querySelector(".chart-tab.active");
        return activeTab ? activeTab.dataset.period : "day";
    }

    function refreshDietUI(data) {
        if (!data) return;

        renderGoalSummary(data.goalSummary);
        renderMacroSummary(data.dailySummary);
        renderMealGroups(data.mealGroups, data.selectedDate);
        refreshChartData(data.charts, data.dailySummary);

        const activePeriod = getActiveChartPeriod();
        updateDietChart(activePeriod);
        renderMacroRatioChart(window.macroRatioData);
    }

    modeRadios.forEach((radio) => {
        radio.addEventListener("change", toggleDietInputMode);
    });

    if (foodSelect) {
        foodSelect.addEventListener("change", fillSelectedFoodInfo);
    }

    if (foodSearchKeyword) {
        foodSearchKeyword.addEventListener("input", filterFoodOptions);
    }

    if (addSelectedFoodBtn) {
        addSelectedFoodBtn.addEventListener("click", addSelectedFood);
    }

    form.addEventListener("submit", function (e) {
        const mode = document.querySelector('input[name="inputMode"]:checked')?.value;

        if (mode === "custom") {
            const name = document.getElementById("customFoodName").value.trim();
            const base = document.getElementById("customBaseAmountG").value;

            if (!name) {
                alert("음식명을 입력해 주세요.");
                e.preventDefault();
                return;
            }

            if (!base || base <= 0) {
                alert("기준량을 올바르게 입력해 주세요.");
                e.preventDefault();
                return;
            }
        } else {
            if (selectedFoods.length === 0) {
                alert("담긴 음식이 없습니다. 음식을 선택한 뒤 '선택 음식 담기'를 눌러주세요.");
                e.preventDefault();
                return;
            }
        }
    });

    toggleDietInputMode();
    fillSelectedFoodInfo();
    filterFoodOptions();
    renderSelectedFoods();
    bindSelectedFoodEvents();
    animateCountUp();
    renderDietIntakeChart("day");
    renderMacroRatioChart(window.macroRatioData);
    bindDietChartTabs();
    bindGoalCard();
    bindIntakeCard();
    bindMealGroupDeleteButtons();
    updateDietChartTitle("day");
    updateDietChartSubText("day");
});