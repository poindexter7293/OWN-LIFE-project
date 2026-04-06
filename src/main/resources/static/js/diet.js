document.addEventListener("DOMContentLoaded", function () {
    const modeRadios = document.querySelectorAll('input[name="inputMode"]');
    const selectModeFields = document.getElementById("selectModeFields");
    const customModeFields = document.getElementById("customModeFields");
    const form = document.querySelector(".diet-form");

    const foodSelect = document.getElementById("foodId");
    const selectedBaseAmountG = document.getElementById("selectedBaseAmountG");
    const selectedCaloriesKcal = document.getElementById("selectedCaloriesKcal");
    const selectedCarbG = document.getElementById("selectedCarbG");
    const selectedProteinG = document.getElementById("selectedProteinG");
    const selectedFatG = document.getElementById("selectedFatG");

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

    function renderDietIntakeChart() {
        const chartCanvas = document.getElementById("dietIntakeChart");
        if (!chartCanvas || !window.dietChartDataMap) return;

        const defaultPeriod = "day";
        const chartData = window.dietChartDataMap[defaultPeriod];
        if (!chartData) return;

        const { labels, calories, goalKcal } = chartData;
        const ctx = chartCanvas.getContext("2d");

        dietIntakeChart = new Chart(ctx, {
            type: "bar",
            data: {
                labels: labels,
                datasets: [
                    {
                        label: "섭취 칼로리",
                        data: calories,
                        backgroundColor: "#22c55e",
                        hoverBackgroundColor: "#16a34a",
                        borderRadius: 10,
                        borderSkipped: false,
                        maxBarThickness: 34
                    },
                    {
                        type: "line",
                        label: "목표 칼로리",
                        data: labels.map(() => goalKcal),
                        borderColor: "#ffb347",
                        borderWidth: 2,
                        pointRadius: 0,
                        pointHoverRadius: 0,
                        fill: false,
                        tension: 0.25
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: {
                    duration: 600,
                    easing: "easeOutCubic"
                },
                plugins: {
                    legend: {
                        display: true,
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
                    }
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
            }
        });
    }

    function updateDietChart(period) {
        if (!dietIntakeChart || !window.dietChartDataMap) return;

        const chartData = window.dietChartDataMap[period];
        if (!chartData) return;

        const { labels, calories, goalKcal } = chartData;

        dietIntakeChart.data.labels = labels;
        dietIntakeChart.data.datasets[0].data = calories;
        dietIntakeChart.data.datasets[1].data = labels.map(() => goalKcal);
        dietIntakeChart.update();

        updateDietChartTitle(period);
    }

    function updateDietChartTitle(period) {
        const titleEl = document.getElementById("dietChartTitle");
        if (!titleEl) return;

        if (period === "day") {
            titleEl.textContent = "최근 7일 섭취량";
        } else if (period === "week") {
            titleEl.textContent = "최근 1주간 섭취량";
        } else if (period === "month") {
            titleEl.textContent = "최근 1개월간 섭취량";
        } else if (period === "year") {
            titleEl.textContent = "최근 1년간 섭취량";
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

    function renderMacroRatioChart() {
        const chartCanvas = document.getElementById("macroRatioChart");
        if (!chartCanvas || !window.macroRatioData) return;

        const { carb, protein, fat } = window.macroRatioData;
        const total = carb + protein + fat;
        const ctx = chartCanvas.getContext("2d");

        const centerTextPlugin = {
            id: "centerTextPlugin",
            afterDraw(chart) {
                const { ctx } = chart;
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

        new Chart(ctx, {
            type: "doughnut",
            data: {
                labels: ["탄수화물", "단백질", "지방"],
                datasets: [{
                    data: isEmpty ? [1, 1, 1] : [carb, protein, fat],
                    backgroundColor: isEmpty
                        ? ["#eef3f9", "#eef3f9", "#eef3f9"]
                        : ["#22c55e", "#6a5cff", "#c4b5fd"],
                    borderWidth: 0,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "72%",
                animation: {
                    animateRotate: true,
                    duration: 1100,
                    easing: "easeOutCubic"
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

    modeRadios.forEach((radio) => {
        radio.addEventListener("change", toggleDietInputMode);
    });

    if (foodSelect) {
        foodSelect.addEventListener("change", fillSelectedFoodInfo);
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
            }
        }
    });

    function bindGoalCard() {
        const goalCard = document.getElementById("goal-card");

        if (!goalCard) return;

        goalCard.addEventListener("click", (e) => {
            e.stopPropagation();

            const confirmMove = confirm("마이페이지로 이동해서 목표를 설정하시겠습니까?");
            if (confirmMove) {
                window.location.href = "/mypage";
            }
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

    toggleDietInputMode();
    fillSelectedFoodInfo();
    animateCountUp();
    renderDietIntakeChart();
    renderMacroRatioChart();
    bindDietChartTabs();
    bindGoalCard();
    bindIntakeCard();
});