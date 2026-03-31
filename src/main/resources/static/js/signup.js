document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('[data-signup-form]');
    if (!form) {
        return;
    }

    const usernameInput = document.getElementById('username');
    const emailInput = document.getElementById('email');
    const birthDateInput = document.getElementById('birthDate');
    const passwordInput = document.getElementById('password');
    const passwordConfirmInput = document.getElementById('passwordConfirm');
    const passwordMatchHint = document.querySelector('[data-password-match]');

    if (birthDateInput) {
        birthDateInput.max = new Date().toISOString().split('T')[0];
    }

    const trimValue = function (input) {
        if (!input) {
            return;
        }
        input.value = input.value.trim();
    };

    if (usernameInput) {
        usernameInput.addEventListener('blur', function () {
            trimValue(usernameInput);
        });
    }

    if (emailInput) {
        emailInput.addEventListener('blur', function () {
            emailInput.value = emailInput.value.trim().toLowerCase();
        });
    }

    const updatePasswordMatchHint = function () {
        if (!passwordMatchHint || !passwordInput || !passwordConfirmInput) {
            return;
        }

        passwordMatchHint.classList.remove('is-match', 'is-mismatch');

        if (!passwordInput.value && !passwordConfirmInput.value) {
            passwordMatchHint.textContent = '두 비밀번호가 일치하는지 확인해 주세요.';
            return;
        }

        if (passwordInput.value && passwordInput.value === passwordConfirmInput.value) {
            passwordMatchHint.textContent = '비밀번호가 일치합니다.';
            passwordMatchHint.classList.add('is-match');
            return;
        }

        passwordMatchHint.textContent = '비밀번호가 아직 일치하지 않습니다.';
        passwordMatchHint.classList.add('is-mismatch');
    };

    if (passwordInput && passwordConfirmInput) {
        passwordInput.addEventListener('input', updatePasswordMatchHint);
        passwordConfirmInput.addEventListener('input', updatePasswordMatchHint);
        updatePasswordMatchHint();
    }
});

