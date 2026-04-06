package com.ownlife.controller;

import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.MyPageForm;
import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.entity.SocialAccount;
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
    private final GoogleAuthService googleAuthService;
    private final KakaoAuthService kakaoAuthService;

    @GetMapping("/mypage")
    public String myPage(@RequestParam(value = "success", defaultValue = "false") boolean success,
                         @RequestParam(value = "googleLinkStatus", required = false) String googleLinkStatus,
                         @RequestParam(value = "googleUnlinkStatus", required = false) String googleUnlinkStatus,
                         @RequestParam(value = "kakaoLinkStatus", required = false) String kakaoLinkStatus,
                         @RequestParam(value = "kakaoUnlinkStatus", required = false) String kakaoUnlinkStatus,
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
        applyPageAttributes(
                model,
                member,
                success,
                resolveGoogleSuccessMessage(googleLinkStatus, googleUnlinkStatus),
                resolveGoogleErrorMessage(googleLinkStatus, googleUnlinkStatus),
                resolveKakaoSuccessMessage(kakaoLinkStatus, kakaoUnlinkStatus),
                resolveKakaoErrorMessage(kakaoLinkStatus, kakaoUnlinkStatus)
        );
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
            applyPageAttributes(model, member, false, null, null, null, null);
            return "main";
        }

        memberService.updateMyPageSettings(loginMember.getMemberId(), myPageForm);
        return "redirect:/mypage?success=true";
    }

    @PostMapping("/mypage/link/google")
    public String linkGoogleAccount(@RequestParam(value = "credential", required = false) String credential,
                                    @RequestParam(value = "g_csrf_token", required = false) String csrfToken,
                                    @CookieValue(value = "g_csrf_token", required = false) String csrfCookie,
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

        if (!googleAuthService.isEnabled()) {
            applyPageAttributes(model, member, false, null, "Google 연동 설정이 아직 완료되지 않았습니다.", null, null);
            return "main";
        }

        if (!isValidGoogleCsrf(csrfCookie, csrfToken)) {
            applyPageAttributes(model, member, false, null, "Google 연동 요청 검증에 실패했습니다. 다시 시도해 주세요.", null, null);
            return "main";
        }

        GoogleUserProfile googleUserProfile = googleAuthService.verifyCredential(credential).orElse(null);
        if (googleUserProfile == null) {
            applyPageAttributes(model, member, false, null, "Google 계정 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.", null, null);
            return "main";
        }

        try {
            memberService.linkGoogleAccount(loginMember.getMemberId(), googleUserProfile);
            return "redirect:/mypage?googleLinkStatus=success";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            applyPageAttributes(model, member, false, null, exception.getMessage(), null, null);
            return "main";
        }
    }

    @GetMapping("/mypage/link/kakao")
    public String linkKakaoAccount(HttpSession session) {
        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Optional<Member> memberOptional = memberService.findById(loginMember.getMemberId());
        if (memberOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        if (!kakaoAuthService.isEnabled()) {
            return "redirect:/mypage?kakaoLinkStatus=config-error";
        }

        session.setAttribute(AuthController.PENDING_KAKAO_LINK_MEMBER_ID, loginMember.getMemberId());
        String kakaoAuthorizationUrl = kakaoAuthService.prepareAuthorizationUrl(session);
        if (!StringUtils.hasText(kakaoAuthorizationUrl)) {
            session.removeAttribute(AuthController.PENDING_KAKAO_LINK_MEMBER_ID);
            return "redirect:/mypage?kakaoLinkStatus=config-error";
        }

        return "redirect:" + kakaoAuthorizationUrl;
    }

    @PostMapping("/mypage/unlink/google")
    public String unlinkGoogleAccount(HttpSession session) {
        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Optional<Member> memberOptional = memberService.findById(loginMember.getMemberId());
        if (memberOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            memberService.unlinkGoogleAccount(loginMember.getMemberId());
            return "redirect:/mypage?googleUnlinkStatus=success";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return "redirect:/mypage?googleUnlinkStatus=" + resolveGoogleUnlinkFailureStatus(exception.getMessage());
        }
    }

    @PostMapping("/mypage/unlink/kakao")
    public String unlinkKakaoAccount(HttpSession session) {
        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Optional<Member> memberOptional = memberService.findById(loginMember.getMemberId());
        if (memberOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            memberService.unlinkKakaoAccount(loginMember.getMemberId());
            return "redirect:/mypage?kakaoUnlinkStatus=success";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return "redirect:/mypage?kakaoUnlinkStatus=" + resolveKakaoUnlinkFailureStatus(exception.getMessage());
        }
    }

    private void applyPageAttributes(Model model,
                                     Member member,
                                     boolean success,
                                     String googleLinkSuccessMessage,
                                     String googleLinkErrorMessage,
                                     String kakaoLinkSuccessMessage,
                                     String kakaoLinkErrorMessage) {
        Optional<SocialAccount> googleAccount = memberService.findSocialAccount(member.getMemberId(), SocialAccount.Provider.GOOGLE);
        Optional<SocialAccount> kakaoAccount = memberService.findSocialAccount(member.getMemberId(), SocialAccount.Provider.KAKAO);

        model.addAttribute("pageTitle", "마이페이지");
        model.addAttribute("centerFragment", "fragments/center-mypage :: centerMyPage");
        model.addAttribute("extraCssFiles", List.of("/css/mypage.css"));
        model.addAttribute("member", member);
        model.addAttribute("settingsUpdated", success);
        model.addAttribute("genderLabel", formatGender(member.getGender()));
        model.addAttribute("loginTypeLabel", formatLoginType(member.getLoginType()));
        model.addAttribute("weightGoalMessage", buildWeightGoalMessage(member));
        model.addAttribute("googleAuthEnabled", googleAuthService.isEnabled());
        model.addAttribute("googleClientId", googleAuthService.getGoogleClientId());
        model.addAttribute("googleLinkUrl", "/mypage/link/google");
        model.addAttribute("googleUnlinkUrl", "/mypage/unlink/google");
        model.addAttribute("googleLinked", googleAccount.isPresent());
        model.addAttribute("googleLinkedEmail", googleAccount.map(SocialAccount::getProviderEmail).orElse(null));
        model.addAttribute("googleLinkedAt", googleAccount.map(SocialAccount::getConnectedAt).orElse(null));
        model.addAttribute("googleLastLoginAt", googleAccount.map(SocialAccount::getLastLoginAt).orElse(null));
        model.addAttribute("googleLinkSuccessMessage", googleLinkSuccessMessage);
        model.addAttribute("googleLinkErrorMessage", googleLinkErrorMessage);
        model.addAttribute("kakaoAuthEnabled", kakaoAuthService.isEnabled());
        model.addAttribute("kakaoLinkUrl", "/mypage/link/kakao");
        model.addAttribute("kakaoUnlinkUrl", "/mypage/unlink/kakao");
        model.addAttribute("kakaoLinked", kakaoAccount.isPresent());
        model.addAttribute("kakaoLinkedEmail", kakaoAccount.map(SocialAccount::getProviderEmail).orElse(null));
        model.addAttribute("kakaoLinkedAt", kakaoAccount.map(SocialAccount::getConnectedAt).orElse(null));
        model.addAttribute("kakaoLastLoginAt", kakaoAccount.map(SocialAccount::getLastLoginAt).orElse(null));
        model.addAttribute("kakaoLinkSuccessMessage", kakaoLinkSuccessMessage);
        model.addAttribute("kakaoLinkErrorMessage", kakaoLinkErrorMessage);
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

    private boolean isValidGoogleCsrf(String csrfCookie, String csrfToken) {
        return StringUtils.hasText(csrfCookie) && csrfCookie.equals(csrfToken);
    }

    private String resolveGoogleSuccessMessage(String googleLinkStatus, String googleUnlinkStatus) {
        String unlinkSuccessMessage = resolveGoogleUnlinkSuccessMessage(googleUnlinkStatus);
        return unlinkSuccessMessage != null ? unlinkSuccessMessage : resolveGoogleLinkSuccessMessage(googleLinkStatus);
    }

    private String resolveGoogleErrorMessage(String googleLinkStatus, String googleUnlinkStatus) {
        String unlinkErrorMessage = resolveGoogleUnlinkErrorMessage(googleUnlinkStatus);
        return unlinkErrorMessage != null ? unlinkErrorMessage : resolveGoogleLinkErrorMessage(googleLinkStatus);
    }

    private String resolveGoogleLinkSuccessMessage(String googleLinkStatus) {
        if (!StringUtils.hasText(googleLinkStatus)) {
            return null;
        }
        return "success".equals(googleLinkStatus) ? "Google 계정이 성공적으로 연동되었습니다." : null;
    }

    private String resolveGoogleLinkErrorMessage(String googleLinkStatus) {
        if (!StringUtils.hasText(googleLinkStatus)) {
            return null;
        }
        return switch (googleLinkStatus) {
            case "already-linked" -> "이미 Google 계정이 연동되어 있습니다.";
            case "linked-other-account" -> "이미 다른 계정에 연결된 Google 계정입니다.";
            default -> null;
        };
    }

    private String resolveGoogleUnlinkSuccessMessage(String googleUnlinkStatus) {
        if (!StringUtils.hasText(googleUnlinkStatus)) {
            return null;
        }
        return "success".equals(googleUnlinkStatus) ? "Google 계정 연동이 해제되었습니다." : null;
    }

    private String resolveGoogleUnlinkErrorMessage(String googleUnlinkStatus) {
        if (!StringUtils.hasText(googleUnlinkStatus)) {
            return null;
        }
        return switch (googleUnlinkStatus) {
            case "not-linked" -> "연동된 Google 계정이 없습니다.";
            case "last-login-method" -> "마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 수단을 먼저 연결해 주세요.";
            default -> "Google 계정 연동 해제 중 오류가 발생했습니다. 다시 시도해 주세요.";
        };
    }

    private String resolveGoogleUnlinkFailureStatus(String message) {
        if ("연동된 Google 계정이 없습니다.".equals(message)) {
            return "not-linked";
        }
        if ("마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 수단을 먼저 연결해 주세요.".equals(message)) {
            return "last-login-method";
        }
        return "failed";
    }

    private String resolveKakaoSuccessMessage(String kakaoLinkStatus, String kakaoUnlinkStatus) {
        String unlinkSuccessMessage = resolveKakaoUnlinkSuccessMessage(kakaoUnlinkStatus);
        return unlinkSuccessMessage != null ? unlinkSuccessMessage : resolveKakaoLinkSuccessMessage(kakaoLinkStatus);
    }

    private String resolveKakaoErrorMessage(String kakaoLinkStatus, String kakaoUnlinkStatus) {
        String unlinkErrorMessage = resolveKakaoUnlinkErrorMessage(kakaoUnlinkStatus);
        return unlinkErrorMessage != null ? unlinkErrorMessage : resolveKakaoLinkErrorMessage(kakaoLinkStatus);
    }

    private String resolveKakaoLinkSuccessMessage(String kakaoLinkStatus) {
        if (!StringUtils.hasText(kakaoLinkStatus)) {
            return null;
        }
        return "success".equals(kakaoLinkStatus) ? "카카오 계정이 성공적으로 연동되었습니다." : null;
    }

    private String resolveKakaoLinkErrorMessage(String kakaoLinkStatus) {
        if (!StringUtils.hasText(kakaoLinkStatus)) {
            return null;
        }
        return switch (kakaoLinkStatus) {
            case "config-error" -> "카카오 연동 설정이 아직 완료되지 않았습니다.";
            case "state-failed" -> "카카오 연동 요청 검증에 실패했습니다. 다시 시도해 주세요.";
            case "auth-failed" -> "카카오 계정 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.";
            case "access-denied" -> "카카오 연동 동의가 취소되었습니다. 다시 시도해 주세요.";
            case "already-linked" -> "이미 카카오 계정이 연동되어 있습니다.";
            case "linked-other-account" -> "이미 다른 계정에 연결된 카카오 계정입니다.";
            case "member-not-found" -> "회원 정보를 찾을 수 없어 카카오 연동을 진행할 수 없습니다.";
            default -> "카카오 계정 연동 중 오류가 발생했습니다. 다시 시도해 주세요.";
        };
    }

    private String resolveKakaoUnlinkSuccessMessage(String kakaoUnlinkStatus) {
        if (!StringUtils.hasText(kakaoUnlinkStatus)) {
            return null;
        }
        return "success".equals(kakaoUnlinkStatus) ? "카카오 계정 연동이 해제되었습니다." : null;
    }

    private String resolveKakaoUnlinkErrorMessage(String kakaoUnlinkStatus) {
        if (!StringUtils.hasText(kakaoUnlinkStatus)) {
            return null;
        }
        return switch (kakaoUnlinkStatus) {
            case "not-linked" -> "연동된 카카오 계정이 없습니다.";
            case "last-login-method" -> "마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 수단을 먼저 연결해 주세요.";
            default -> "카카오 계정 연동 해제 중 오류가 발생했습니다. 다시 시도해 주세요.";
        };
    }

    private String resolveKakaoUnlinkFailureStatus(String message) {
        if ("연동된 카카오 계정이 없습니다.".equals(message)) {
            return "not-linked";
        }
        if ("마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 수단을 먼저 연결해 주세요.".equals(message)) {
            return "last-login-method";
        }
        return "failed";
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

