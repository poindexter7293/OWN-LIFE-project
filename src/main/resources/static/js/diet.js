document.addEventListener("DOMContentLoaded", function () {
    const modeRadios = document.querySelectorAll('input[name="inputMode"]');
    const selectModeFields = document.getElementById("selectModeFields");
    const customModeFields = document.getElementById("customModeFields");

    const form = document.querySelector('.diet-form');

    const foodSelect = document.getElementById("foodId");
    const selectedBaseAmountG = document.getElementById("selectedBaseAmountG");
    const selectedCaloriesKcal = document.getElementById("selectedCaloriesKcal");
    const selectedCarbG = document.getElementById("selectedCarbG");
    const selectedProteinG = document.getElementById("selectedProteinG");
    const selectedFatG = document.getElementById("selectedFatG");




    if (!modeRadios.length || !selectModeFields || !customModeFields) {
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

    modeRadios.forEach(radio => {
        radio.addEventListener("change", toggleDietInputMode);
    });

    if (foodSelect) {
        foodSelect.addEventListener("change", fillSelectedFoodInfo);
    }

    toggleDietInputMode();
    fillSelectedFoodInfo();
    renderDietIntakeChart();

    form.addEventListener('submit', function (e) {
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
        }
    });

    function renderDietIntakeChart() {
        const chartCanvas = document.getElementById("dietIntakeChart");
        if (!chartCanvas || !window.weeklyIntakeData) return;

        const { labels, calories, colors, goalKcal } = window.weeklyIntakeData;
        const ctx = chartCanvas.getContext("2d");

        new Chart(ctx, {
            type: "bar",
            data: {
                labels: labels,
                datasets: [
                    {
                        label: "섭취 칼로리",
                        data: calories,
                        backgroundColor: colors,
                        borderRadius: 8
                    },
                    {
                        type: "line",
                        label: "목표 칼로리",
                        data: labels.map(() => goalKcal),
                        borderColor: "#ffb347",
                        borderWidth: 2,
                        pointRadius: 0,
                        fill: false,
                        tension: 0
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 200,
                            callback: function(value) {
                                return value + " kcal";
                            }
                        }
                    }
                }
            }
        });
    }
});