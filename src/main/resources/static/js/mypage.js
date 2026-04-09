document.addEventListener('DOMContentLoaded', function () {
    initializeStatBoxFocus();
    initializeAiCommentCard();

    function initializeStatBoxFocus() {
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
    }

    function initializeAiCommentCard() {
        const card = document.querySelector('.mypage-ai-comment-card[data-ai-comment-endpoint]');
        if (!card) {
            return;
        }

        const endpoint = card.dataset.aiCommentEndpoint;
        const refreshEndpoint = card.dataset.aiCommentRefreshEndpoint;
        const badge = card.querySelector('[data-ai-comment-badge]');
        const body = card.querySelector('[data-ai-comment-body]');
        const message = card.querySelector('[data-ai-comment-message]');
        const detail = card.querySelector('[data-ai-comment-detail]');
        const meta = card.querySelector('[data-ai-comment-meta]');
        const refreshButton = card.querySelector('[data-ai-comment-refresh-button]');
        const retryButton = card.querySelector('[data-ai-comment-retry]');
        const toneClassPrefix = 'mypage-ai-comment-card__body--';

        if (!endpoint || !badge || !body || !message || !detail || !meta) {
            return;
        }

        const resetToneClasses = () => {
            Array.from(body.classList)
                .filter(function (className) {
                    return className.indexOf(toneClassPrefix) === 0;
                })
                .forEach(function (className) {
                    body.classList.remove(className);
                });
        };

        const applyTone = (tone) => {
            resetToneClasses();
            body.classList.add(tone ? toneClassPrefix + tone : toneClassPrefix + 'muted');
        };

        const setBadge = (label, isFallback, isLoading) => {
            badge.textContent = label;
            badge.classList.toggle('mypage-ai-comment-card__badge--fallback', Boolean(isFallback));
            badge.classList.toggle('mypage-ai-comment-card__badge--loading', Boolean(isLoading));
        };

        const updateRefreshControls = (comment, isLoading) => {
            const remaining = comment && Number.isInteger(comment.remainingRefreshCount)
                ? comment.remainingRefreshCount
                : 5;
            const limit = comment && Number.isInteger(comment.dailyRefreshLimit)
                ? comment.dailyRefreshLimit
                : 5;
            const refreshAllowed = comment ? Boolean(comment.refreshAllowed) : true;

            meta.textContent = '오늘 남은 새 코멘트 횟수: ' + remaining + ' / ' + limit;
            if (refreshButton) {
                refreshButton.disabled = isLoading || !refreshAllowed;
                refreshButton.textContent = refreshAllowed ? '새 코멘트 받기' : '오늘 횟수 소진';
            }
        };

        const renderLoading = () => {
            applyTone('muted');
            setBadge('불러오는 중', false, true);
            message.textContent = 'AI 한 줄 코멘트를 가져오는 중이에요...';
            detail.textContent = '마이페이지는 먼저 표시되고, AI 코멘트는 잠시 후 자동으로 불러옵니다.';
            updateRefreshControls(null, true);
            if (retryButton) {
                retryButton.hidden = true;
            }
        };

        const renderComment = (comment) => {
            applyTone(comment.tone || 'muted');
            setBadge(comment.badgeLabel || (comment.fallback ? '기본 코멘트' : 'AI 코멘트'), comment.fallback, false);
            message.textContent = comment.message || 'AI 코멘트를 준비했어요.';
            detail.textContent = comment.detail || '최근 기록을 바탕으로 요약한 코멘트예요.';
            updateRefreshControls(comment, false);
            if (retryButton) {
                retryButton.hidden = true;
            }
        };

        const renderUnauthorized = () => {
            applyTone('muted');
            setBadge('로그인 필요', true, false);
            message.textContent = '세션이 만료되어 AI 코멘트를 불러올 수 없어요.';
            detail.textContent = '다시 로그인한 뒤 마이페이지에 들어오면 코멘트를 확인할 수 있어요.';
            meta.textContent = '로그인 후 다시 시도해 주세요.';
            if (refreshButton) {
                refreshButton.disabled = true;
                refreshButton.textContent = '새 코멘트 받기';
            }
            if (retryButton) {
                retryButton.hidden = true;
            }
        };

        const renderError = () => {
            applyTone('muted');
            setBadge('재시도 가능', true, false);
            message.textContent = 'AI 코멘트를 아직 불러오지 못했어요.';
            detail.textContent = '잠시 후 다시 시도하면 코멘트를 가져올 수 있어요.';
            meta.textContent = '네트워크 상태를 확인한 뒤 다시 시도해 주세요.';
            if (refreshButton) {
                refreshButton.disabled = false;
                refreshButton.textContent = '새 코멘트 받기';
            }
            if (retryButton) {
                retryButton.hidden = false;
            }
        };

        const requestComment = async (url, method) => {
            renderLoading();

            try {
                const response = await fetch(url, {
                    method: method,
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    credentials: 'same-origin'
                });

                if (response.status === 401) {
                    renderUnauthorized();
                    return;
                }

                if (!response.ok) {
                    renderError();
                    return;
                }

                const comment = await response.json();
                renderComment(comment);
            } catch (error) {
                renderError();
            }
        };

        const loadComment = async () => requestComment(endpoint, 'GET');
        const refreshComment = async () => {
            if (!refreshEndpoint) {
                renderError();
                return;
            }
            return requestComment(refreshEndpoint, 'POST');
        };

        if (retryButton) {
            retryButton.addEventListener('click', function () {
                loadComment();
            });
        }

        if (refreshButton) {
            refreshButton.addEventListener('click', function () {
                if (refreshButton.disabled) {
                    return;
                }
                refreshComment();
            });
        }

        window.setTimeout(function () {
            loadComment();
        }, 60);
    }
});

