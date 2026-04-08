document.addEventListener('DOMContentLoaded', function () {
    const statBoxes = document.querySelectorAll('.mypage-stat-box[data-focus-target]');

    if (statBoxes.length === 0) {
        return;
    }

    const activeClassName = 'is-focus-target-active';
    let activeField = null;
    let activeFieldTimer = null;

    const clearActiveField = () => {
        if (activeField) {
            activeField.classList.remove(activeClassName);
            activeField = null;
        }

        if (activeFieldTimer) {
            window.clearTimeout(activeFieldTimer);
            activeFieldTimer = null;
        }
    };

    const focusTargetInput = (targetId) => {
        if (!targetId) {
            return;
        }

        const input = document.getElementById(targetId);
        if (!input) {
            return;
        }

        const field = input.closest('.mypage-form-field');
        clearActiveField();

        if (field) {
            activeField = field;
            field.classList.add(activeClassName);
            activeFieldTimer = window.setTimeout(clearActiveField, 1600);
        }

        input.scrollIntoView({
            behavior: 'smooth',
            block: 'center'
        });

        window.setTimeout(function () {
            input.focus({ preventScroll: true });

            if (typeof input.select === 'function' && !input.readOnly && !input.disabled) {
                input.select();
            }
        }, 220);
    };

    statBoxes.forEach(function (statBox) {
        const targetId = statBox.dataset.focusTarget;
        if (!targetId) {
            return;
        }

        statBox.addEventListener('click', function () {
            focusTargetInput(targetId);
        });

        statBox.addEventListener('keydown', function (event) {
            if (event.key !== 'Enter' && event.key !== ' ') {
                return;
            }

            event.preventDefault();
            focusTargetInput(targetId);
        });
    });
});

