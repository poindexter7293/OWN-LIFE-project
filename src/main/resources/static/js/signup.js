document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('[data-signup-form]');
    if (!form) {
        return;
    }

    const usernameInput = document.getElementById('username');
    const nicknameInput = document.getElementById('nickname');
    const emailInput = document.getElementById('email');
    const birthDateInput = document.getElementById('birthDate');
    const heightInput = document.getElementById('heightCm');
    const weightInput = document.getElementById('weightKg');
    const passwordInput = document.getElementById('password');
    const passwordConfirmInput = document.getElementById('passwordConfirm');
    const passwordMatchHint = document.querySelector('[data-password-match]');
    const submitButton = form.querySelector('button[type="submit"]');
    const socialSignupMode = form.getAttribute('data-social-signup-mode') === 'true';

    const usernamePattern = /^[a-z0-9][a-z0-9._-]{3,49}$/;
    const emailPattern = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    const availabilityState = {
        username: { value: null, available: false, pending: false },
        nickname: { value: null, available: false, pending: false },
        email: { value: null, available: false, pending: false }
    };

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
            usernameInput.value = usernameInput.value.trim().toLowerCase();
            validateUsernameField(true);
        });

        usernameInput.addEventListener('input', function () {
            availabilityState.username = { value: null, available: false, pending: false };
            validateUsernameField(false);
        });
    }

    if (emailInput && !socialSignupMode) {
        emailInput.addEventListener('blur', function () {
            emailInput.value = emailInput.value.trim().toLowerCase();
            validateEmailField(true);
        });

        emailInput.addEventListener('input', function () {
            availabilityState.email = { value: null, available: false, pending: false };
            validateEmailField(false);
        });
    }

    if (nicknameInput) {
        nicknameInput.addEventListener('input', function () {
            availabilityState.nickname = { value: null, available: false, pending: false };
            validateNicknameField(false);
        });
        nicknameInput.addEventListener('blur', function () {
            trimValue(nicknameInput);
            validateNicknameField(true);
        });
    }

    if (birthDateInput) {
        birthDateInput.addEventListener('change', function () {
            validateBirthDateField();
        });
    }

    if (heightInput) {
        heightInput.addEventListener('input', function () {
            validateNumberField(heightInput, 'heightCm', 300, '키는 0보다 커야 합니다.', '키는 300cm 이하로 입력해 주세요.');
        });
    }

    if (weightInput) {
        weightInput.addEventListener('input', function () {
            validateNumberField(weightInput, 'weightKg', 500, '현재 체중은 0보다 커야 합니다.', '현재 체중은 500kg 이하로 입력해 주세요.');
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

    const setFeedback = function (fieldName, message, state) {
        const feedback = form.querySelector('[data-feedback="' + fieldName + '"]');
        if (!feedback) {
            return;
        }

        feedback.textContent = message || '';
        feedback.classList.remove('is-match', 'is-mismatch');

        if (!message) {
            return;
        }

        if (state === 'success') {
            feedback.classList.add('is-match');
        } else if (state === 'error' || state === 'checking') {
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

    const validateUsernameField = function (checkAvailability) {
        if (!usernameInput) {
            return true;
        }

        const value = usernameInput.value.trim().toLowerCase();
        usernameInput.value = value;

        if (!value) {
            setFeedback('username', '아이디를 입력해 주세요.', 'error');
            return false;
        }

        if (!usernamePattern.test(value)) {
            setFeedback('username', '아이디는 4~50자의 영문 소문자, 숫자, ., _, - 만 사용할 수 있습니다.', 'error');
            return false;
        }

        if (!checkAvailability) {
            setFeedback('username', '형식이 올바릅니다. 입력을 마치면 중복을 확인합니다.', 'success');
            return true;
        }

        if (availabilityState.username.value === value && !availabilityState.username.pending) {
            setFeedback('username', availabilityState.username.available ? '사용 가능한 아이디입니다.' : '이미 사용 중인 아이디입니다.', availabilityState.username.available ? 'success' : 'error');
            return availabilityState.username.available;
        }

        availabilityState.username.pending = true;
        setFeedback('username', '아이디 중복을 확인하는 중입니다...', 'checking');
        setSubmitDisabled(true);

        fetch('/signup/check-username?username=' + encodeURIComponent(value), {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                availabilityState.username = {
                    value: value,
                    available: !!data.available,
                    pending: false
                };
                setFeedback('username', data.message, data.available ? 'success' : 'error');
            })
            .catch(function () {
                availabilityState.username = { value: value, available: false, pending: false };
                setFeedback('username', '아이디 확인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
            })
            .finally(function () {
                setSubmitDisabled(false);
            });

        return true;
    };

    const validateNicknameField = function (checkAvailability) {
        if (!nicknameInput) {
            return true;
        }

        const value = nicknameInput.value.trim();
        nicknameInput.value = value;
        if (!value) {
            setFeedback('nickname', '닉네임을 입력해 주세요.', 'error');
            return false;
        }
        if (value.length < 2 || value.length > 50) {
            setFeedback('nickname', '닉네임은 2자 이상 50자 이하로 입력해 주세요.', 'error');
            return false;
        }

        if (!checkAvailability) {
            setFeedback('nickname', '형식이 올바릅니다. 입력을 마치면 중복을 확인합니다.', 'success');
            return true;
        }

        if (availabilityState.nickname.value === value && !availabilityState.nickname.pending) {
            setFeedback('nickname', availabilityState.nickname.available ? '사용 가능한 닉네임입니다.' : '이미 사용 중인 닉네임입니다.', availabilityState.nickname.available ? 'success' : 'error');
            return availabilityState.nickname.available;
        }

        availabilityState.nickname.pending = true;
        setFeedback('nickname', '닉네임 중복을 확인하는 중입니다...', 'checking');
        setSubmitDisabled(true);

        fetch('/signup/check-nickname?nickname=' + encodeURIComponent(value), {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                availabilityState.nickname = {
                    value: value,
                    available: !!data.available,
                    pending: false
                };
                setFeedback('nickname', data.message, data.available ? 'success' : 'error');
            })
            .catch(function () {
                availabilityState.nickname = { value: value, available: false, pending: false };
                setFeedback('nickname', '닉네임 확인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
            })
            .finally(function () {
                setSubmitDisabled(false);
            });

        return true;
    };

    const validateEmailField = function (checkAvailability) {
        if (!emailInput) {
            return true;
        }

        const value = emailInput.value.trim().toLowerCase();
        emailInput.value = value;

        if (!value) {
            setFeedback('email', '이메일을 입력해 주세요.', 'error');
            return false;
        }
        if (value.length > 100 || !emailPattern.test(value)) {
            setFeedback('email', '올바른 이메일 형식으로 입력해 주세요.', 'error');
            return false;
        }

        if (!checkAvailability) {
            setFeedback('email', '형식이 올바릅니다. 입력을 마치면 중복을 확인합니다.', 'success');
            return true;
        }

        if (availabilityState.email.value === value && !availabilityState.email.pending) {
            setFeedback('email', availabilityState.email.available ? '사용 가능한 이메일입니다.' : '이미 사용 중인 이메일입니다.', availabilityState.email.available ? 'success' : 'error');
            return availabilityState.email.available;
        }

        availabilityState.email.pending = true;
        setFeedback('email', '이메일 중복을 확인하는 중입니다...', 'checking');
        setSubmitDisabled(true);

        fetch('/signup/check-email?email=' + encodeURIComponent(value), {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        })
            .then(function (response) {
                return response.json();
            })
            .then(function (data) {
                availabilityState.email = {
                    value: value,
                    available: !!data.available,
                    pending: false
                };
                setFeedback('email', data.message, data.available ? 'success' : 'error');
            })
            .catch(function () {
                availabilityState.email = { value: value, available: false, pending: false };
                setFeedback('email', '이메일 확인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'error');
            })
            .finally(function () {
                setSubmitDisabled(false);
            });

        return true;
    };

    const validatePasswordField = function () {
        if (!passwordInput) {
            return true;
        }

        const value = passwordInput.value;
        if (!value) {
            setFeedback('password', '비밀번호를 입력해 주세요.', 'error');
            return false;
        }
        if (value.length < 8 || value.length > 72) {
            setFeedback('password', '비밀번호는 8자 이상 72자 이하로 입력해 주세요.', 'error');
            return false;
        }

        setFeedback('password', '비밀번호 길이가 적절합니다.', 'success');
        return true;
    };

    const validateBirthDateField = function () {
        if (!birthDateInput || !birthDateInput.value) {
            setFeedback('birthDate', '', '');
            return true;
        }

        const selectedDate = new Date(birthDateInput.value + 'T00:00:00');
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        if (selectedDate > today) {
            setFeedback('birthDate', '생년월일은 오늘 이후 날짜를 선택할 수 없습니다.', 'error');
            return false;
        }

        setFeedback('birthDate', '', '');
        return true;
    };

    const validateNumberField = function (input, fieldName, max, minMessage, maxMessage) {
        if (!input || !input.value) {
            setFeedback(fieldName, '', '');
            return true;
        }

        const value = Number(input.value);
        if (Number.isNaN(value) || value <= 0) {
            setFeedback(fieldName, minMessage, 'error');
            return false;
        }
        if (value > max) {
            setFeedback(fieldName, maxMessage, 'error');
            return false;
        }

        setFeedback(fieldName, '', '');
        return true;
    };

    const waitForAvailabilityCheck = function (fieldName) {
        return new Promise(function (resolve) {
            const tick = function () {
                if (!availabilityState[fieldName].pending) {
                    resolve(availabilityState[fieldName].available);
                    return;
                }
                window.setTimeout(tick, 50);
            };
            tick();
        });
    };

    if (passwordInput && passwordConfirmInput) {
        passwordInput.addEventListener('input', updatePasswordMatchHint);
        passwordConfirmInput.addEventListener('input', updatePasswordMatchHint);
        passwordInput.addEventListener('input', validatePasswordField);
        updatePasswordMatchHint();
    }

    form.addEventListener('submit', async function (event) {
        const localValid = [
            socialSignupMode ? true : validateUsernameField(false),
            validateNicknameField(false),
            socialSignupMode ? true : validateEmailField(false),
            socialSignupMode ? true : validatePasswordField(),
            validateBirthDateField(),
            validateNumberField(heightInput, 'heightCm', 300, '키는 0보다 커야 합니다.', '키는 300cm 이하로 입력해 주세요.'),
            validateNumberField(weightInput, 'weightKg', 500, '현재 체중은 0보다 커야 합니다.', '현재 체중은 500kg 이하로 입력해 주세요.')
        ].every(Boolean);

        updatePasswordMatchHint();
        if (!localValid || (!socialSignupMode && passwordInput && passwordConfirmInput && passwordInput.value !== passwordConfirmInput.value)) {
            event.preventDefault();
            return;
        }

        event.preventDefault();
        setSubmitDisabled(true);

        validateNicknameField(true);
        if (!socialSignupMode) {
            validateUsernameField(true);
            validateEmailField(true);
        }

        const checks = socialSignupMode
            ? [waitForAvailabilityCheck('nickname')]
            : [
                waitForAvailabilityCheck('username'),
                waitForAvailabilityCheck('nickname'),
                waitForAvailabilityCheck('email')
            ];

        const results = await Promise.all(checks);

        setSubmitDisabled(false);
        if (results.every(Boolean)) {
            form.submit();
        }
    });
});

