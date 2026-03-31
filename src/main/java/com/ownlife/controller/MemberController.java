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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z][가-힣a-zA-Z\\s]{1,49}$");
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
        return "redirect:/signup?success=true";
    }

    private void applyPageAttributes(Model model, boolean success) {
        model.addAttribute("pageTitle", "회원가입");
        model.addAttribute("centerFragment", "fragments/center-signup :: centerSignup");
        model.addAttribute("extraCssFiles", List.of("/css/signup.css"));
        model.addAttribute("extraJsFiles", List.of("/js/signup.js"));
        model.addAttribute("signupSuccess", success);
    }

    private void normalizeForm(SignupForm signupForm) {
        signupForm.setUsername(trimToNull(signupForm.getUsername()));
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
        if (!StringUtils.hasText(username)) {
            bindingResult.rejectValue("username", "required", "이름을 입력해 주세요.");
            return;
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            bindingResult.rejectValue("username", "invalid", "이름은 2~50자의 한글 또는 영문으로 입력해 주세요.");
            return;
        }
        if (memberService.existsByUsername(username)) {
            bindingResult.rejectValue("username", "duplicate", "이미 사용 중인 이름입니다.");
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
        if (!StringUtils.hasText(email)) {
            bindingResult.rejectValue("email", "required", "이메일을 입력해 주세요.");
            return;
        }
        if (email.length() > 100 || !EMAIL_PATTERN.matcher(email).matches()) {
            bindingResult.rejectValue("email", "invalid", "올바른 이메일 형식으로 입력해 주세요.");
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

