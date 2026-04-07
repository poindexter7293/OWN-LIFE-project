package com.ownlife.controller;

import com.ownlife.dto.PendingGoogleSignup;
import com.ownlife.dto.PendingKakaoSignup;
import com.ownlife.dto.PendingNaverSignup;
import com.ownlife.dto.SignupForm;
import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.service.GoogleAuthService;
import com.ownlife.service.KakaoAuthService;
import com.ownlife.service.MemberService;
import com.ownlife.service.NaverAuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{3,49}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final MemberService memberService;
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;
    private final NaverAuthService naverAuthService;

    @ModelAttribute("genderOptions")
    public Member.Gender[] genderOptions() {
        return Member.Gender.values();
    }

    @GetMapping("/signup")
    public String signupForm(@RequestParam(value = "success", defaultValue = "false") boolean success,
                             HttpSession session,
                             Model model) {
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupForm());
        }
        applyPageAttributes(model, success, null, session);
        return "main";
    }

    @GetMapping("/signup/google")
    public String googleSignupForm(Model model, HttpSession session) {
        PendingGoogleSignup pendingGoogleSignup = getPendingGoogleSignup(session);
        if (pendingGoogleSignup == null) {
            return "redirect:/login?googleError=signup-session-expired";
        }

        if (!model.containsAttribute("signupForm")) {
            SignupForm signupForm = new SignupForm();
            applyGooglePrefill(signupForm, pendingGoogleSignup, true);
            model.addAttribute("signupForm", signupForm);
        }
        applyPageAttributes(model, false, "google", session);
        return "main";
    }

    @GetMapping("/signup/kakao")
    public String kakaoSignupForm(Model model, HttpSession session) {
        PendingKakaoSignup pendingKakaoSignup = getPendingKakaoSignup(session);
        if (pendingKakaoSignup == null) {
            return "redirect:/login?kakaoError=signup-session-expired";
        }

        if (!model.containsAttribute("signupForm")) {
            SignupForm signupForm = new SignupForm();
            applyKakaoPrefill(signupForm, pendingKakaoSignup, true);
            model.addAttribute("signupForm", signupForm);
        }
        applyPageAttributes(model, false, "kakao", session);
        return "main";
    }

    @GetMapping("/signup/naver")
    public String naverSignupForm(Model model, HttpSession session) {
        PendingNaverSignup pendingNaverSignup = getPendingNaverSignup(session);
        if (pendingNaverSignup == null) {
            return "redirect:/login?naverError=signup-session-expired";
        }

        if (!model.containsAttribute("signupForm")) {
            SignupForm signupForm = new SignupForm();
            applyNaverPrefill(signupForm, pendingNaverSignup, true);
            model.addAttribute("signupForm", signupForm);
        }
        applyPageAttributes(model, false, "naver", session);
        return "main";
    }

    @GetMapping("/signup/check-username")
    @ResponseBody
    public Map<String, Object> checkUsername(@RequestParam("username") String username) {
        String normalizedUsername = normalizeIdentity(username);
        String validationMessage = validateUsernameValue(normalizedUsername);

        Map<String, Object> response = new LinkedHashMap<>();
        if (validationMessage != null) {
            response.put("available", false);
            response.put("valid", false);
            response.put("message", validationMessage);
            return response;
        }

        boolean exists = memberService.existsByUsername(normalizedUsername);
        response.put("available", !exists);
        response.put("valid", true);
        response.put("message", exists ? "이미 사용 중인 아이디입니다." : "사용 가능한 아이디입니다.");
        return response;
    }

    @GetMapping("/signup/check-email")
    @ResponseBody
    public Map<String, Object> checkEmail(@RequestParam("email") String email) {
        String normalizedEmail = normalizeIdentity(email);
        String validationMessage = validateEmailValue(normalizedEmail);

        Map<String, Object> response = new LinkedHashMap<>();
        if (validationMessage != null) {
            response.put("available", false);
            response.put("valid", false);
            response.put("message", validationMessage);
            return response;
        }

        boolean exists = memberService.existsByEmail(normalizedEmail);
        response.put("available", !exists);
        response.put("valid", true);
        response.put("message", exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다.");
        return response;
    }

    @GetMapping("/signup/check-nickname")
    @ResponseBody
    public Map<String, Object> checkNickname(@RequestParam("nickname") String nickname) {
        String normalizedNickname = trimToNull(nickname);
        String validationMessage = validateNicknameValue(normalizedNickname);

        Map<String, Object> response = new LinkedHashMap<>();
        if (validationMessage != null) {
            response.put("available", false);
            response.put("valid", false);
            response.put("message", validationMessage);
            return response;
        }

        boolean exists = memberService.existsByNickname(normalizedNickname);
        response.put("available", !exists);
        response.put("valid", true);
        response.put("message", exists ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다.");
        return response;
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute("signupForm") SignupForm signupForm,
                         BindingResult bindingResult,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        normalizeForm(signupForm);
        validateSignupForm(signupForm, bindingResult);

        if (bindingResult.hasErrors()) {
            applyPageAttributes(model, false, null, session);
            return "main";
        }

        memberService.register(signupForm);
        redirectAttributes.addFlashAttribute("postRedirectAlertMessage", "회원가입이 완료되었습니다.");
        return "redirect:/main";
    }

    @PostMapping("/signup/google")
    public String googleSignup(@ModelAttribute("signupForm") SignupForm signupForm,
                               BindingResult bindingResult,
                               Model model,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        PendingGoogleSignup pendingGoogleSignup = getPendingGoogleSignup(session);
        if (pendingGoogleSignup == null) {
            return "redirect:/login?googleError=signup-session-expired";
        }

        normalizeGoogleSignupForm(signupForm, pendingGoogleSignup);
        validateGoogleSignupForm(signupForm, bindingResult);

        if (bindingResult.hasErrors()) {
            applyPageAttributes(model, false, "google", session);
            return "main";
        }

        Member member;
        try {
            member = memberService.registerGoogleMember(signupForm, pendingGoogleSignup.toGoogleUserProfile());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("googleSignupFailed", exception.getMessage());
            applyPageAttributes(model, false, "google", session);
            return "main";
        }
        session.removeAttribute(AuthController.PENDING_GOOGLE_SIGNUP);
        session.setAttribute(AuthController.LOGIN_MEMBER, toSessionMember(member));
        redirectAttributes.addFlashAttribute("postRedirectAlertMessage", "회원가입이 완료되었습니다.");
        return "redirect:/main";
    }

    @PostMapping("/signup/kakao")
    public String kakaoSignup(@ModelAttribute("signupForm") SignupForm signupForm,
                              BindingResult bindingResult,
                              Model model,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        PendingKakaoSignup pendingKakaoSignup = getPendingKakaoSignup(session);
        if (pendingKakaoSignup == null) {
            return "redirect:/login?kakaoError=signup-session-expired";
        }

        normalizeKakaoSignupForm(signupForm, pendingKakaoSignup);
        validateKakaoSignupForm(signupForm, bindingResult);

        if (bindingResult.hasErrors()) {
            applyPageAttributes(model, false, "kakao", session);
            return "main";
        }

        Member member;
        try {
            member = memberService.registerKakaoMember(signupForm, pendingKakaoSignup.toKakaoUserProfile());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("kakaoSignupFailed", exception.getMessage());
            applyPageAttributes(model, false, "kakao", session);
            return "main";
        }
        session.removeAttribute(AuthController.PENDING_KAKAO_SIGNUP);
        session.setAttribute(AuthController.LOGIN_MEMBER, toSessionMember(member));
        redirectAttributes.addFlashAttribute("postRedirectAlertMessage", "회원가입이 완료되었습니다.");
        return "redirect:/main";
    }

    @PostMapping("/signup/naver")
    public String naverSignup(@ModelAttribute("signupForm") SignupForm signupForm,
                              BindingResult bindingResult,
                              Model model,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        PendingNaverSignup pendingNaverSignup = getPendingNaverSignup(session);
        if (pendingNaverSignup == null) {
            return "redirect:/login?naverError=signup-session-expired";
        }

        normalizeNaverSignupForm(signupForm, pendingNaverSignup);
        validateNaverSignupForm(signupForm, pendingNaverSignup, bindingResult);

        if (bindingResult.hasErrors()) {
            applyPageAttributes(model, false, "naver", session);
            return "main";
        }

        Member member;
        try {
            member = memberService.registerNaverMember(signupForm, pendingNaverSignup.toNaverUserProfile());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.reject("naverSignupFailed", exception.getMessage());
            applyPageAttributes(model, false, "naver", session);
            return "main";
        }
        session.removeAttribute(AuthController.PENDING_NAVER_SIGNUP);
        session.setAttribute(AuthController.LOGIN_MEMBER, toSessionMember(member));
        redirectAttributes.addFlashAttribute("postRedirectAlertMessage", "회원가입이 완료되었습니다.");
        return "redirect:/main";
    }

    private void applyPageAttributes(Model model, boolean success, String socialSignupProvider, HttpSession session) {
        boolean googleSignupMode = "google".equals(socialSignupProvider);
        boolean kakaoSignupMode = "kakao".equals(socialSignupProvider);
        boolean naverSignupMode = "naver".equals(socialSignupProvider);
        boolean socialSignupMode = googleSignupMode || kakaoSignupMode || naverSignupMode;
        String socialProviderLabel = googleSignupMode ? "Google" : (kakaoSignupMode ? "카카오" : (naverSignupMode ? "네이버" : null));
        PendingNaverSignup pendingNaverSignup = naverSignupMode ? getPendingNaverSignup(session) : null;
        boolean socialEmailReadonly = googleSignupMode
                || kakaoSignupMode
                || (naverSignupMode && pendingNaverSignup != null && StringUtils.hasText(pendingNaverSignup.getEmail()));

        model.addAttribute("pageTitle", socialSignupMode ? socialProviderLabel + " 추가 회원가입" : "회원가입");
        model.addAttribute("centerFragment", "fragments/center-signup :: centerSignup");
        model.addAttribute("extraCssFiles", List.of("/css/signup.css"));
        model.addAttribute("extraJsFiles", List.of("/js/signup.js"));
        model.addAttribute("signupSuccess", success);
        model.addAttribute("googleSignupMode", googleSignupMode);
        model.addAttribute("kakaoSignupMode", kakaoSignupMode);
        model.addAttribute("naverSignupMode", naverSignupMode);
        model.addAttribute("socialSignupMode", socialSignupMode);
        model.addAttribute("socialProviderLabel", socialProviderLabel);
        model.addAttribute("socialEmailReadonly", socialEmailReadonly);
        model.addAttribute("signupAction", googleSignupMode ? "/signup/google" : (kakaoSignupMode ? "/signup/kakao" : (naverSignupMode ? "/signup/naver" : "/signup")));
        model.addAttribute("signupHeading", socialSignupMode ? socialProviderLabel + " 계정 추가 정보 입력" : "건강한 루틴을 위한 회원가입");
        model.addAttribute("signupDescriptionText", socialSignupMode
                ? socialProviderLabel + " 인증은 완료되었어요. 닉네임, 성별, 키, 체중 같은 기본 정보를 입력하면 가입이 완료됩니다."
                : "기본 계정 정보와 기초 프로필만 먼저 입력하고, 세부 목표는 가입 후에 천천히 설정할 수 있어요.");
        model.addAttribute("signupSubmitLabel", socialSignupMode ? socialProviderLabel + " 회원가입 완료" : "회원가입 완료");
        model.addAttribute("googleAuthEnabled", googleAuthService.isEnabled());
        model.addAttribute("googleClientId", googleAuthService.getGoogleClientId());
        model.addAttribute("googleRedirectUrl", googleAuthService.getGoogleRedirectUrl());
        model.addAttribute("kakaoAuthEnabled", kakaoAuthService.isEnabled());
        model.addAttribute("kakaoLoginUrl", kakaoAuthService.prepareAuthorizationUrl(session));
        model.addAttribute("naverAuthEnabled", naverAuthService.isEnabled());
        model.addAttribute("naverLoginUrl", naverAuthService.prepareAuthorizationUrl(session));
    }

    private void normalizeForm(SignupForm signupForm) {
        signupForm.setUsername(normalizeIdentity(signupForm.getUsername()));
        signupForm.setNickname(trimToNull(signupForm.getNickname()));
        signupForm.setEmail(normalizeIdentity(signupForm.getEmail()));
    }

    private void normalizeGoogleSignupForm(SignupForm signupForm, PendingGoogleSignup pendingGoogleSignup) {
        signupForm.setUsername(null);
        signupForm.setPassword(null);
        signupForm.setPasswordConfirm(null);
        signupForm.setNickname(trimToNull(signupForm.getNickname()));
        signupForm.setEmail(normalizeIdentity(pendingGoogleSignup.getEmail()));
    }

    private void normalizeKakaoSignupForm(SignupForm signupForm, PendingKakaoSignup pendingKakaoSignup) {
        signupForm.setUsername(null);
        signupForm.setPassword(null);
        signupForm.setPasswordConfirm(null);
        signupForm.setNickname(trimToNull(signupForm.getNickname()));
        signupForm.setEmail(normalizeIdentity(pendingKakaoSignup.getEmail()));
    }

    private void normalizeNaverSignupForm(SignupForm signupForm, PendingNaverSignup pendingNaverSignup) {
        signupForm.setUsername(null);
        signupForm.setPassword(null);
        signupForm.setPasswordConfirm(null);
        signupForm.setNickname(trimToNull(signupForm.getNickname()));
        if (StringUtils.hasText(pendingNaverSignup.getEmail())) {
            signupForm.setEmail(normalizeIdentity(pendingNaverSignup.getEmail()));
            return;
        }
        signupForm.setEmail(normalizeIdentity(signupForm.getEmail()));
    }

    private void validateSignupForm(SignupForm signupForm, BindingResult bindingResult) {
        validateUsername(signupForm, bindingResult);
        validatePassword(signupForm, bindingResult);
        validateNickname(signupForm, bindingResult);
        validateEmail(signupForm, bindingResult);
        validateBirthDate(signupForm, bindingResult);
        validateDecimalRange(signupForm.getHeightCm(), "heightCm", "키는 0보다 커야 합니다.", "키는 300cm 이하로 입력해 주세요.", bindingResult, new BigDecimal("300"));
        validateDecimalRange(signupForm.getWeightKg(), "weightKg", "현재 체중은 0보다 커야 합니다.", "현재 체중은 500kg 이하로 입력해 주세요.", bindingResult, new BigDecimal("500"));
    }

    private void validateGoogleSignupForm(SignupForm signupForm, BindingResult bindingResult) {
        validateNickname(signupForm, bindingResult);
        String emailValidationMessage = validateEmailValue(signupForm.getEmail());
        if (emailValidationMessage != null) {
            bindingResult.rejectValue("email", "invalid", emailValidationMessage);
        }
        validateGenderRequired(signupForm, bindingResult);
        validateBirthDate(signupForm, bindingResult);
        validateRequiredDecimal(signupForm.getHeightCm(), "heightCm", "키를 입력해 주세요.", "키는 0보다 커야 합니다.", "키는 300cm 이하로 입력해 주세요.", bindingResult, new BigDecimal("300"));
        validateRequiredDecimal(signupForm.getWeightKg(), "weightKg", "현재 체중을 입력해 주세요.", "현재 체중은 0보다 커야 합니다.", "현재 체중은 500kg 이하로 입력해 주세요.", bindingResult, new BigDecimal("500"));
    }

    private void validateKakaoSignupForm(SignupForm signupForm, BindingResult bindingResult) {
        validateGoogleSignupForm(signupForm, bindingResult);
    }

    private void validateNaverSignupForm(SignupForm signupForm, PendingNaverSignup pendingNaverSignup, BindingResult bindingResult) {
        validateNickname(signupForm, bindingResult);

        if (pendingNaverSignup != null && StringUtils.hasText(pendingNaverSignup.getEmail())) {
            String emailValidationMessage = validateEmailValue(signupForm.getEmail());
            if (emailValidationMessage != null) {
                bindingResult.rejectValue("email", "invalid", emailValidationMessage);
            }
        } else {
            validateEmail(signupForm, bindingResult);
        }

        validateGenderRequired(signupForm, bindingResult);
        validateBirthDate(signupForm, bindingResult);
        validateRequiredDecimal(signupForm.getHeightCm(), "heightCm", "키를 입력해 주세요.", "키는 0보다 커야 합니다.", "키는 300cm 이하로 입력해 주세요.", bindingResult, new BigDecimal("300"));
        validateRequiredDecimal(signupForm.getWeightKg(), "weightKg", "현재 체중을 입력해 주세요.", "현재 체중은 0보다 커야 합니다.", "현재 체중은 500kg 이하로 입력해 주세요.", bindingResult, new BigDecimal("500"));
    }

    private void validateUsername(SignupForm signupForm, BindingResult bindingResult) {
        String username = signupForm.getUsername();
        String validationMessage = validateUsernameValue(username);
        if (validationMessage != null) {
            bindingResult.rejectValue("username", "invalid", validationMessage);
            return;
        }
        if (memberService.existsByUsername(username)) {
            bindingResult.rejectValue("username", "duplicate", "이미 사용 중인 아이디입니다.");
        }
    }

    private void validatePassword(SignupForm signupForm, BindingResult bindingResult) {
        String password = signupForm.getPassword();
        String passwordConfirm = signupForm.getPasswordConfirm();

        if (!StringUtils.hasText(password)) {
            bindingResult.rejectValue("password", "required", "비밀번호를 입력해 주세요.");
        } else if (password.length() < 8 || password.length() > 72) {
            bindingResult.rejectValue("password", "length", "비밀번호는 8자 이상 72자 이하로 입력해 주세요.");
        }

        if (!StringUtils.hasText(passwordConfirm)) {
            bindingResult.rejectValue("passwordConfirm", "required", "비밀번호 확인을 입력해 주세요.");
        } else if (StringUtils.hasText(password) && !password.equals(passwordConfirm)) {
            bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 서로 일치하지 않습니다.");
        }
    }

    private void validateNickname(SignupForm signupForm, BindingResult bindingResult) {
        String nickname = signupForm.getNickname();
        String validationMessage = validateNicknameValue(nickname);
        if (validationMessage != null) {
            bindingResult.rejectValue("nickname", "invalid", validationMessage);
            return;
        }
        if (memberService.existsByNickname(nickname)) {
            bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
        }
    }

    private String validateNicknameValue(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return "닉네임을 입력해 주세요.";
        }
        if (nickname.length() < 2 || nickname.length() > 50) {
            return "닉네임은 2자 이상 50자 이하로 입력해 주세요.";
        }
        return null;
    }

    private void validateGenderRequired(SignupForm signupForm, BindingResult bindingResult) {
        if (signupForm.getGender() == null) {
            bindingResult.rejectValue("gender", "required", "성별을 선택해 주세요.");
        }
    }

    private void validateEmail(SignupForm signupForm, BindingResult bindingResult) {
        String email = signupForm.getEmail();
        String validationMessage = validateEmailValue(email);
        if (validationMessage != null) {
            bindingResult.rejectValue("email", "invalid", validationMessage);
            return;
        }
        if (memberService.existsByEmail(email)) {
            bindingResult.rejectValue("email", "duplicate", "이미 사용 중인 이메일입니다.");
        }
    }

    private void validateBirthDate(SignupForm signupForm, BindingResult bindingResult) {
        LocalDate birthDate = signupForm.getBirthDate();
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            bindingResult.rejectValue("birthDate", "future", "생년월일은 오늘 이후 날짜를 선택할 수 없습니다.");
        }
    }

    private void validateDecimalRange(BigDecimal value,
                                      String fieldName,
                                      String underMinMessage,
                                      String overMaxMessage,
                                      BindingResult bindingResult,
                                      BigDecimal max) {
        if (value == null) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            bindingResult.rejectValue(fieldName, "min", underMinMessage);
        } else if (value.compareTo(max) > 0) {
            bindingResult.rejectValue(fieldName, "max", overMaxMessage);
        }
    }

    private void validateRequiredDecimal(BigDecimal value,
                                         String fieldName,
                                         String requiredMessage,
                                         String underMinMessage,
                                         String overMaxMessage,
                                         BindingResult bindingResult,
                                         BigDecimal max) {
        if (value == null) {
            bindingResult.rejectValue(fieldName, "required", requiredMessage);
            return;
        }
        validateDecimalRange(value, fieldName, underMinMessage, overMaxMessage, bindingResult, max);
    }

    private String validateUsernameValue(String username) {
        if (!StringUtils.hasText(username)) {
            return "아이디를 입력해 주세요.";
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "아이디는 4~50자의 영문 소문자, 숫자, ., _, - 만 사용할 수 있습니다.";
        }
        return null;
    }

    private String validateEmailValue(String email) {
        if (!StringUtils.hasText(email)) {
            return "이메일을 입력해 주세요.";
        }
        if (email.length() > 100 || !EMAIL_PATTERN.matcher(email).matches()) {
            return "올바른 이메일 형식으로 입력해 주세요.";
        }
        return null;
    }

    private String normalizeIdentity(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private PendingGoogleSignup getPendingGoogleSignup(HttpSession session) {
        Object attribute = session.getAttribute(AuthController.PENDING_GOOGLE_SIGNUP);
        return attribute instanceof PendingGoogleSignup ? (PendingGoogleSignup) attribute : null;
    }

    private PendingKakaoSignup getPendingKakaoSignup(HttpSession session) {
        Object attribute = session.getAttribute(AuthController.PENDING_KAKAO_SIGNUP);
        return attribute instanceof PendingKakaoSignup ? (PendingKakaoSignup) attribute : null;
    }

    private PendingNaverSignup getPendingNaverSignup(HttpSession session) {
        Object attribute = session.getAttribute(AuthController.PENDING_NAVER_SIGNUP);
        return attribute instanceof PendingNaverSignup ? (PendingNaverSignup) attribute : null;
    }

    private void applyGooglePrefill(SignupForm signupForm, PendingGoogleSignup pendingGoogleSignup, boolean fillNicknameIfEmpty) {
        signupForm.setEmail(normalizeIdentity(pendingGoogleSignup.getEmail()));
        if (fillNicknameIfEmpty && !StringUtils.hasText(signupForm.getNickname())) {
            signupForm.setNickname(trimToNull(pendingGoogleSignup.getName()));
        }
    }

    private void applyKakaoPrefill(SignupForm signupForm, PendingKakaoSignup pendingKakaoSignup, boolean fillNicknameIfEmpty) {
        signupForm.setEmail(normalizeIdentity(pendingKakaoSignup.getEmail()));
        if (fillNicknameIfEmpty && !StringUtils.hasText(signupForm.getNickname())) {
            signupForm.setNickname(trimToNull(pendingKakaoSignup.getNickname()));
        }
    }

    private void applyNaverPrefill(SignupForm signupForm, PendingNaverSignup pendingNaverSignup, boolean fillNicknameIfEmpty) {
        signupForm.setEmail(normalizeIdentity(pendingNaverSignup.getEmail()));
        if (fillNicknameIfEmpty && !StringUtils.hasText(signupForm.getNickname())) {
            String preferredNickname = trimToNull(pendingNaverSignup.getNickname());
            if (!StringUtils.hasText(preferredNickname)) {
                preferredNickname = trimToNull(pendingNaverSignup.getName());
            }
            signupForm.setNickname(preferredNickname);
        }
    }

    private SessionMember toSessionMember(Member member) {
        return new SessionMember(member.getMemberId(), member.getUsername(), member.getNickname(), member.getRole());
    }
}

