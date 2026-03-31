document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('[data-login-form]');
    if (!form) {
        return;
    }

    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const submitButton = form.querySelector('button[type="submit"]');
    const usernamePattern = /^[a-z0-9][a-z0-9._-]{3,49}$/;

    const setFeedback = function (fieldName, message, state) {
        const feedback = form.querySelector('[data-login-feedback="' + fieldName + '"]');
        if (!feedback) {
            return;
        }

        feedback.textContent = message || '';
        feedback.classList.remove('is-match', 'is-mismatch');
        if (state === 'success') {
            feedback.classList.add('is-match');
        } else if (state === 'error') {
            feedback.classList.add('is-mismatch');
        }
    };

    const setSubmitDisabled = function (disabled) {
        if (!submitButton) {
            return;
        }
        submitButton.disabled = disabled;
        submitButton.style.opacity = disabled ? '0.7' : '1';
        submitButton.style.cursor = disabled ? 'not-allowed' : 'pointer';
    };

    const validateUsername = function () {
        if (!usernameInput) {
            return true;
        }

        usernameInput.value = usernameInput.value.trim().toLowerCase();
        if (!usernameInput.value) {
            setFeedback('username', '아이디를 입력해 주세요.', 'error');
            return false;
        }
        if (!usernamePattern.test(usernameInput.value)) {
            setFeedback('username', '아이디 형식을 다시 확인해 주세요.', 'error');
            return false;
        }
        setFeedback('username', '', '');
        return true;
    };

    const validatePassword = function () {
        if (!passwordInput) {
            return true;
        }

        if (!passwordInput.value) {
            setFeedback('password', '비밀번호를 입력해 주세요.', 'error');
            return false;
        }
        if (passwordInput.value.length < 8 || passwordInput.value.length > 72) {
            setFeedback('password', '비밀번호 형식을 다시 확인해 주세요.', 'error');
            return false;
        }
        setFeedback('password', '', '');
        return true;
    };

    if (usernameInput) {
        usernameInput.addEventListener('blur', validateUsername);
        usernameInput.addEventListener('input', function () {
            setFeedback('username', '', '');
        });
    }

    if (passwordInput) {
        passwordInput.addEventListener('input', function () {
            setFeedback('password', '', '');
        });
    }

    form.addEventListener('submit', function (event) {
        const valid = validateUsername() && validatePassword();
        if (!valid) {
            event.preventDefault();
            return;
        }

        setSubmitDisabled(true);
    });
});

