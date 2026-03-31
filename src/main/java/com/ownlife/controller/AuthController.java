package com.ownlife.controller;

import com.ownlife.dto.LoginForm;
import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.service.MemberService;
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

import java.util.List;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class AuthController {

    public static final String LOGIN_MEMBER = "loginMember";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{3,49}$");

    private final MemberService memberService;

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "logoutSuccess", defaultValue = "false") boolean logoutSuccess,
                            Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        applyPageAttributes(model, logoutSuccess);
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
            applyPageAttributes(model, false);
            return "main";
        }

        return memberService.authenticate(loginForm.getUsername(), loginForm.getPassword())
                .map(member -> {
                    session.setAttribute(LOGIN_MEMBER, toSessionMember(member));
                    return "redirect:/main";
                })
                .orElseGet(() -> {
                    bindingResult.reject("loginFailed", "아이디 또는 비밀번호가 올바르지 않습니다.");
                    applyPageAttributes(model, false);
                    return "main";
                });
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logoutSuccess=true";
    }

    private void applyPageAttributes(Model model, boolean logoutSuccess) {
        model.addAttribute("pageTitle", "로그인");
        model.addAttribute("centerFragment", "fragments/center-login :: centerLogin");
        model.addAttribute("extraCssFiles", List.of("/css/login.css"));
        model.addAttribute("extraJsFiles", List.of("/js/login.js"));
        model.addAttribute("logoutSuccess", logoutSuccess);
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

    private SessionMember toSessionMember(Member member) {
        return new SessionMember(member.getMemberId(), member.getUsername(), member.getNickname(), member.getRole());
    }
}

