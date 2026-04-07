document.addEventListener('DOMContentLoaded', function () {
    const protectedLinks = document.querySelectorAll('.header-wrap [data-login-required="true"]');

    if (!protectedLinks.length) {
        return;
    }

    protectedLinks.forEach(function (link) {
        link.addEventListener('click', function (event) {
            if (window.isLoginMember) {
                return;
            }

            event.preventDefault();
            event.stopPropagation();
            event.stopImmediatePropagation();

            alert('로그인이 필요한 서비스입니다.');
            return false;
        }, true);
    });
});