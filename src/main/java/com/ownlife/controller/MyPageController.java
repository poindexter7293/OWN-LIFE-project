package com.ownlife.controller;

import com.ownlife.dto.MyPageForm;
import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("500");
    private static final int MAX_CALORIES = 10_000;

    private final MemberService memberService;

    @GetMapping("/mypage")
    public String myPage(@RequestParam(value = "success", defaultValue = "false") boolean success,
                         Model model,
                         HttpSession session) {
        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Optional<Member> memberOptional = memberService.findById(loginMember.getMemberId());
        if (memberOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        Member member = memberOptional.get();
        if (!model.containsAttribute("myPageForm")) {
            model.addAttribute("myPageForm", toForm(member));
        }
        applyPageAttributes(model, member, success);
        return "main";
    }

    @PostMapping("/mypage")
    public String updateMyPage(@ModelAttribute("myPageForm") MyPageForm myPageForm,
                               BindingResult bindingResult,
                               Model model,
                               HttpSession session) {
        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Optional<Member> memberOptional = memberService.findById(loginMember.getMemberId());
        if (memberOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        validateMyPageForm(myPageForm, bindingResult);
        Member member = memberOptional.get();
        if (bindingResult.hasErrors()) {
            applyPageAttributes(model, member, false);
            return "main";
        }

        memberService.updateMyPageSettings(loginMember.getMemberId(), myPageForm);
        return "redirect:/mypage?success=true";
    }

    private void applyPageAttributes(Model model, Member member, boolean success) {
        model.addAttribute("pageTitle", "마이페이지");
        model.addAttribute("centerFragment", "fragments/center-mypage :: centerMyPage");
        model.addAttribute("extraCssFiles", List.of("/css/mypage.css"));
        model.addAttribute("member", member);
        model.addAttribute("settingsUpdated", success);
        model.addAttribute("genderLabel", formatGender(member.getGender()));
        model.addAttribute("loginTypeLabel", formatLoginType(member.getLoginType()));
        model.addAttribute("weightGoalMessage", buildWeightGoalMessage(member));
    }

    private MyPageForm toForm(Member member) {
        MyPageForm myPageForm = new MyPageForm();
        myPageForm.setWeightKg(member.getWeightKg());
        myPageForm.setGoalWeight(member.getGoalWeight());
        myPageForm.setGoalEatKcal(member.getGoalEatKcal());
        myPageForm.setGoalBurnedKcal(member.getGoalBurnedKcal());
        return myPageForm;
    }

    private void validateMyPageForm(MyPageForm myPageForm, BindingResult bindingResult) {
        validateWeightField(myPageForm.getWeightKg(), "weightKg", "현재 체중은 0보다 커야 합니다.", bindingResult);
        validateWeightField(myPageForm.getGoalWeight(), "goalWeight", "목표 체중은 0보다 커야 합니다.", bindingResult);
        validateCalories(myPageForm.getGoalEatKcal(), "goalEatKcal", "목표 섭취 칼로리는 0보다 커야 합니다.", bindingResult);
        validateCalories(myPageForm.getGoalBurnedKcal(), "goalBurnedKcal", "목표 소모 칼로리는 0보다 커야 합니다.", bindingResult);
    }

    private void validateWeightField(BigDecimal value,
                                     String fieldName,
                                     String minMessage,
                                     BindingResult bindingResult) {
        if (value == null) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            bindingResult.rejectValue(fieldName, "min", minMessage);
            return;
        }
        if (value.compareTo(MAX_WEIGHT_KG) > 0) {
            bindingResult.rejectValue(fieldName, "max", "체중은 500kg 이하로 입력해 주세요.");
        }
    }

    private void validateCalories(Integer value,
                                  String fieldName,
                                  String minMessage,
                                  BindingResult bindingResult) {
        if (value == null) {
            return;
        }
        if (value <= 0) {
            bindingResult.rejectValue(fieldName, "min", minMessage);
            return;
        }
        if (value > MAX_CALORIES) {
            bindingResult.rejectValue(fieldName, "max", "칼로리 목표는 10,000kcal 이하로 입력해 주세요.");
        }
    }

    private SessionMember getLoginMember(HttpSession session) {
        Object sessionMember = session.getAttribute(AuthController.LOGIN_MEMBER);
        return sessionMember instanceof SessionMember ? (SessionMember) sessionMember : null;
    }

    private String formatGender(Member.Gender gender) {
        return gender == null ? "미설정" : gender.getLabel();
    }

    private String formatLoginType(Member.LoginType loginType) {
        if (loginType == null) {
            return "미확인";
        }
        return switch (loginType) {
            case LOCAL -> "일반 계정";
            case NAVER -> "네이버";
            case KAKAO -> "카카오";
            case GOOGLE -> "구글";
        };
    }

    private String buildWeightGoalMessage(Member member) {
        if (member.getWeightKg() == null || member.getGoalWeight() == null) {
            return "현재 체중과 목표 체중을 모두 입력하면 진행 상황을 한눈에 볼 수 있어요.";
        }

        int compareResult = member.getWeightKg().compareTo(member.getGoalWeight());
        if (compareResult == 0) {
            return "현재 체중이 목표 체중과 같아요. 지금 루틴을 꾸준히 유지해 보세요.";
        }

        BigDecimal difference = member.getWeightKg().subtract(member.getGoalWeight()).abs();
        String formattedDifference = difference.stripTrailingZeros().toPlainString();
        if (compareResult > 0) {
            return String.format(Locale.ROOT, "목표 체중까지 %skg 감량이 필요해요.", formattedDifference);
        }
        return String.format(Locale.ROOT, "목표 체중까지 %skg 증량이 필요해요.", formattedDifference);
    }
}

