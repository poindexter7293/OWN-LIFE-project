package com.ownlife.controller;

import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.KakaoUserProfile;
import com.ownlife.dto.LoginForm;
import com.ownlife.dto.PendingKakaoSignup;
import com.ownlife.dto.PendingGoogleSignup;
import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.service.GoogleAuthService;
import com.ownlife.service.KakaoAuthService;
import com.ownlife.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class AuthController {

    public static final String LOGIN_MEMBER = "loginMember";
    public static final String PENDING_GOOGLE_SIGNUP = "pendingGoogleSignup";
    public static final String PENDING_KAKAO_SIGNUP = "pendingKakaoSignup";
    public static final String PENDING_KAKAO_LINK_MEMBER_ID = "pendingKakaoLinkMemberId";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{3,49}$");

    private final MemberService memberService;
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "logoutSuccess", defaultValue = "false") boolean logoutSuccess,
                            @RequestParam(value = "googleError", required = false) String googleError,
                             @RequestParam(value = "kakaoError", required = false) String kakaoError,
                             HttpSession session,
                            Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        applyPageAttributes(model, logoutSuccess, resolveGoogleErrorMessage(googleError), resolveKakaoErrorMessage(kakaoError), session);
        return "main";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute("loginForm") LoginForm loginForm,
                        BindingResult bindingResult,
                        Model model,
                        HttpSession session) {
        normalizeForm(loginForm);
        validateLoginForm(loginForm, bindingResult);

        if (bindingResult.hasErrors()) {
            applyPageAttributes(model, false, null, null, session);
            return "main";
        }

        return memberService.authenticate(loginForm.getUsername(), loginForm.getPassword())
                .map(member -> {
                    session.removeAttribute(PENDING_GOOGLE_SIGNUP);
                    session.setAttribute(LOGIN_MEMBER, toSessionMember(member));
                    return "redirect:/main";
                })
                .orElseGet(() -> {
                    bindingResult.reject("loginFailed", "아이디 또는 비밀번호가 올바르지 않습니다.");
                    applyPageAttributes(model, false, null, null, session);
                    return "main";
                });
    }

    @PostMapping("/login/google/auth")
    public String googleLogin(@RequestParam(value = "credential", required = false) String credential,
                              @RequestParam(value = "g_csrf_token", required = false) String csrfToken,
                              @CookieValue(value = "g_csrf_token", required = false) String csrfCookie,
                              Model model,
                              HttpSession session) {
        if (!googleAuthService.isEnabled()) {
            return renderGoogleLoginError(model, session, "Google 로그인 설정이 아직 완료되지 않았습니다.");
        }

        if (!isValidGoogleCsrf(csrfCookie, csrfToken)) {
            return renderGoogleLoginError(model, session, "Google 로그인 요청 검증에 실패했습니다. 다시 시도해 주세요.");
        }

        GoogleUserProfile googleUserProfile = googleAuthService.verifyCredential(credential)
                .orElse(null);
        if (googleUserProfile == null) {
            return renderGoogleLoginError(model, session, "Google 계정 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        try {
            return memberService.findGoogleMemberForLogin(googleUserProfile)
                    .map(member -> {
                        session.removeAttribute(PENDING_GOOGLE_SIGNUP);
                        session.setAttribute(LOGIN_MEMBER, toSessionMember(member));
                        return "redirect:/main";
                    })
                    .orElseGet(() -> {
                        session.setAttribute(PENDING_GOOGLE_SIGNUP, PendingGoogleSignup.from(googleUserProfile));
                        return "redirect:/signup/google";
                    });
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return renderGoogleLoginError(model, session, exception.getMessage());
        }
    }

    @GetMapping("/login/kakao/auth")
    public String kakaoLogin(@RequestParam(value = "code", required = false) String code,
                             @RequestParam(value = "state", required = false) String state,
                             @RequestParam(value = "error", required = false) String error,
                             @RequestParam(value = "error_description", required = false) String errorDescription,
                             Model model,
                             HttpSession session) {
        Long pendingKakaoLinkMemberId = getPendingKakaoLinkMemberId(session);

        if (!kakaoAuthService.isEnabled()) {
            if (pendingKakaoLinkMemberId != null) {
                clearPendingKakaoLink(session);
                return "redirect:/mypage?kakaoLinkStatus=config-error";
            }
            return renderKakaoLoginError(model, session, "카카오 로그인 설정이 아직 완료되지 않았습니다.");
        }

        if (StringUtils.hasText(error)) {
            if (pendingKakaoLinkMemberId != null) {
                clearPendingKakaoLink(session);
                return "redirect:/mypage?kakaoLinkStatus=" + resolveKakaoLinkErrorStatus(error);
            }
            return renderKakaoLoginError(model, session, resolveKakaoOauthErrorMessage(error, errorDescription));
        }

        if (!kakaoAuthService.isValidState(session, state)) {
            if (pendingKakaoLinkMemberId != null) {
                clearPendingKakaoLink(session);
                return "redirect:/mypage?kakaoLinkStatus=state-failed";
            }
            return renderKakaoLoginError(model, session, "카카오 로그인 요청 검증에 실패했습니다. 다시 시도해 주세요.");
        }

        KakaoUserProfile kakaoUserProfile = kakaoAuthService.requestUserProfile(code).orElse(null);
        if (kakaoUserProfile == null) {
            if (pendingKakaoLinkMemberId != null) {
                clearPendingKakaoLink(session);
                return "redirect:/mypage?kakaoLinkStatus=auth-failed";
            }
            return renderKakaoLoginError(model, session, "카카오 계정 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }

        if (pendingKakaoLinkMemberId != null) {
            clearPendingKakaoLink(session);
            try {
                memberService.linkKakaoAccount(pendingKakaoLinkMemberId, kakaoUserProfile);
                return "redirect:/mypage?kakaoLinkStatus=success";
            } catch (IllegalArgumentException exception) {
                return "redirect:/mypage?kakaoLinkStatus=member-not-found";
            } catch (IllegalStateException exception) {
                return "redirect:/mypage?kakaoLinkStatus=" + resolveKakaoLinkFailureStatus(exception.getMessage());
            }
        }

        try {
            return memberService.findKakaoMemberForLogin(kakaoUserProfile)
                    .map(member -> {
                        session.removeAttribute(PENDING_KAKAO_SIGNUP);
                        session.setAttribute(LOGIN_MEMBER, toSessionMember(member));
                        return "redirect:/main";
                    })
                    .orElseGet(() -> {
                        session.setAttribute(PENDING_KAKAO_SIGNUP, PendingKakaoSignup.from(kakaoUserProfile));
                        return "redirect:/signup/kakao";
                    });
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return renderKakaoLoginError(model, session, exception.getMessage());
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logoutSuccess=true";
    }

    private String renderGoogleLoginError(Model model, HttpSession session, String errorMessage) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        applyPageAttributes(model, false, errorMessage, null, session);
        return "main";
    }

    private String renderKakaoLoginError(Model model, HttpSession session, String errorMessage) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        applyPageAttributes(model, false, null, errorMessage, session);
        return "main";
    }

    private void applyPageAttributes(Model model,
                                     boolean logoutSuccess,
                                     String googleErrorMessage,
                                     String kakaoErrorMessage,
                                     HttpSession session) {
        model.addAttribute("pageTitle", "로그인");
        model.addAttribute("centerFragment", "fragments/center-login :: centerLogin");
        model.addAttribute("extraCssFiles", List.of("/css/login.css"));
        model.addAttribute("extraJsFiles", List.of("/js/login.js"));
        model.addAttribute("logoutSuccess", logoutSuccess);
        model.addAttribute("googleAuthEnabled", googleAuthService.isEnabled());
        model.addAttribute("googleClientId", googleAuthService.getGoogleClientId());
        model.addAttribute("googleRedirectUrl", googleAuthService.getGoogleRedirectUrl());
        model.addAttribute("kakaoAuthEnabled", kakaoAuthService.isEnabled());
        model.addAttribute("kakaoLoginUrl", kakaoAuthService.prepareAuthorizationUrl(session));
        model.addAttribute("googleErrorMessage", googleErrorMessage);
        model.addAttribute("kakaoErrorMessage", kakaoErrorMessage);
    }

    private void normalizeForm(LoginForm loginForm) {
        loginForm.setUsername(normalizeUsername(loginForm.getUsername()));
    }

    private void validateLoginForm(LoginForm loginForm, BindingResult bindingResult) {
        validateUsername(loginForm.getUsername(), bindingResult);
        validatePassword(loginForm.getPassword(), bindingResult);
    }

    private void validateUsername(String username, BindingResult bindingResult) {
        if (!StringUtils.hasText(username)) {
            bindingResult.rejectValue("username", "required", "아이디를 입력해 주세요.");
            return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            bindingResult.rejectValue("username", "invalid", "아이디 형식을 다시 확인해 주세요.");
        }
    }

    private void validatePassword(String password, BindingResult bindingResult) {
        if (!StringUtils.hasText(password)) {
            bindingResult.rejectValue("password", "required", "비밀번호를 입력해 주세요.");
            return;
        }
        if (password.length() < 8 || password.length() > 72) {
            bindingResult.rejectValue("password", "length", "비밀번호 형식을 다시 확인해 주세요.");
        }
    }

    private String normalizeUsername(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private boolean isValidGoogleCsrf(String csrfCookie, String csrfToken) {
        return StringUtils.hasText(csrfCookie) && csrfCookie.equals(csrfToken);
    }

    private String resolveGoogleErrorMessage(String googleError) {
        if (!StringUtils.hasText(googleError)) {
            return null;
        }
        return switch (googleError) {
            case "signup-session-expired" -> "Google 추가 회원가입 세션이 만료되었습니다. 다시 로그인해 주세요.";
            default -> googleError;
        };
    }

    private String resolveKakaoErrorMessage(String kakaoError) {
        if (!StringUtils.hasText(kakaoError)) {
            return null;
        }
        return switch (kakaoError) {
            case "signup-session-expired" -> "카카오 추가 회원가입 세션이 만료되었습니다. 다시 로그인해 주세요.";
            default -> kakaoError;
        };
    }

    private String resolveKakaoOauthErrorMessage(String error, String errorDescription) {
        if (!StringUtils.hasText(error)) {
            return null;
        }

        if ("access_denied".equals(error)) {
            return "카카오 로그인 동의가 취소되었습니다. 다시 시도해 주세요.";
        }
        if (StringUtils.hasText(errorDescription)) {
            return errorDescription;
        }
        return "카카오 로그인 중 오류가 발생했습니다. 다시 시도해 주세요.";
    }

    private Long getPendingKakaoLinkMemberId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute(PENDING_KAKAO_LINK_MEMBER_ID);
        return attribute instanceof Long longValue ? longValue : null;
    }

    private void clearPendingKakaoLink(HttpSession session) {
        if (session != null) {
            session.removeAttribute(PENDING_KAKAO_LINK_MEMBER_ID);
        }
    }

    private String resolveKakaoLinkErrorStatus(String error) {
        if ("access_denied".equals(error)) {
            return "access-denied";
        }
        return "oauth-error";
    }

    private String resolveKakaoLinkFailureStatus(String message) {
        if ("이미 카카오 계정이 연동되어 있습니다.".equals(message)) {
            return "already-linked";
        }
        if ("이미 다른 계정에 연결된 카카오 계정입니다.".equals(message)) {
            return "linked-other-account";
        }
        return "failed";
    }

    private SessionMember toSessionMember(Member member) {
        return new SessionMember(member.getMemberId(), member.getUsername(), member.getNickname(), member.getRole());
    }
}

