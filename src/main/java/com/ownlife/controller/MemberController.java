package com.ownlife.controller;

import com.ownlife.dto.SignupForm;
import com.ownlife.entity.Member;
import com.ownlife.service.MemberService;
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

    @ModelAttribute("genderOptions")
    public Member.Gender[] genderOptions() {
        return Member.Gender.values();
    }

    @GetMapping("/signup")
    public String signupForm(@RequestParam(value = "success", defaultValue = "false") boolean success,
                             Model model) {
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupForm());
        }
        applyPageAttributes(model, success);
        return "main";
    }

    @GetMapping("/signup/check-username")
    @ResponseBody
    public Map<String, Object> checkUsername(@RequestParam String username) {
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
    public Map<String, Object> checkEmail(@RequestParam String email) {
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

    @PostMapping("/signup")
    public String signup(@ModelAttribute("signupForm") SignupForm signupForm,
                         BindingResult bindingResult,
                         Model model) {
        normalizeForm(signupForm);
        validateSignupForm(signupForm, bindingResult);

        if (bindingResult.hasErrors()) {
            applyPageAttributes(model, false);
            return "main";
        }

        memberService.register(signupForm);
        return "redirect:/main";
    }

    private void applyPageAttributes(Model model, boolean success) {
        model.addAttribute("pageTitle", "회원가입");
        model.addAttribute("centerFragment", "fragments/center-signup :: centerSignup");
        model.addAttribute("extraCssFiles", List.of("/css/signup.css"));
        model.addAttribute("extraJsFiles", List.of("/js/signup.js"));
        model.addAttribute("signupSuccess", success);
    }

    private void normalizeForm(SignupForm signupForm) {
        signupForm.setUsername(normalizeIdentity(signupForm.getUsername()));
        signupForm.setNickname(trimToNull(signupForm.getNickname()));
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
        if (!StringUtils.hasText(nickname)) {
            bindingResult.rejectValue("nickname", "required", "닉네임을 입력해 주세요.");
            return;
        }
        if (nickname.length() < 2 || nickname.length() > 50) {
            bindingResult.rejectValue("nickname", "length", "닉네임은 2자 이상 50자 이하로 입력해 주세요.");
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
}

