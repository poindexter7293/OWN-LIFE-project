package com.ownlife.controller;

import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.AiOneLineCommentDto;
import com.ownlife.dto.LifestylePatternAnalysisDto;
import com.ownlife.dto.MyPageForm;
import com.ownlife.dto.SessionMember;
import com.ownlife.dto.WithdrawalForm;
import com.ownlife.entity.Member;
import com.ownlife.entity.SocialAccount;
import com.ownlife.service.GoogleAuthService;
import com.ownlife.service.AiOneLineCommentService;
import com.ownlife.service.KakaoAuthService;
import com.ownlife.service.LifestylePatternService;
import com.ownlife.service.MemberService;
import com.ownlife.service.NaverAuthService;
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
    private final NaverAuthService naverAuthService;
    private final LifestylePatternService lifestylePatternService;
    private final AiOneLineCommentService aiOneLineCommentService;

    @GetMapping("/mypage")
    public String myPage(@RequestParam(value = "success", defaultValue = "false") boolean success,
                         @RequestParam(value = "googleLinkStatus", required = false) String googleLinkStatus,
                         @RequestParam(value = "googleUnlinkStatus", required = false) String googleUnlinkStatus,
                         @RequestParam(value = "kakaoLinkStatus", required = false) String kakaoLinkStatus,
                         @RequestParam(value = "kakaoUnlinkStatus", required = false) String kakaoUnlinkStatus,
                         @RequestParam(value = "naverLinkStatus", required = false) String naverLinkStatus,
                         @RequestParam(value = "naverUnlinkStatus", required = false) String naverUnlinkStatus,
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
        String naverLinkErrorMessage = consumeSessionString(session, AuthController.NAVER_LINK_ERROR_MESSAGE);
        if (!model.containsAttribute("myPageForm")) {
            model.addAttribute("myPageForm", toForm(member));
        }
        if (!model.containsAttribute("withdrawalForm")) {
            model.addAttribute("withdrawalForm", new WithdrawalForm());
        }
        applyPageAttributes(
                model,
                member,
                success,
                resolveGoogleSuccessMessage(googleLinkStatus, googleUnlinkStatus),
                resolveGoogleErrorMessage(googleLinkStatus, googleUnlinkStatus),
                resolveKakaoSuccessMessage(kakaoLinkStatus, kakaoUnlinkStatus),
                resolveKakaoErrorMessage(kakaoLinkStatus, kakaoUnlinkStatus),
                resolveNaverSuccessMessage(naverLinkStatus, naverUnlinkStatus),
                resolveNaverErrorMessage(naverLinkStatus, naverUnlinkStatus, naverLinkErrorMessage)
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
            applyPageAttributes(model, member, false, null, null, null, null, null, null);
            return "main";
        }

        memberService.updateMyPageSettings(loginMember.getMemberId(), myPageForm);
        return "redirect:/mypage?success=true";
    }

    @PostMapping("/mypage/withdraw")
    public String withdrawMember(@ModelAttribute("withdrawalForm") WithdrawalForm withdrawalForm,
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

        Member member = memberOptional.get();
        if (!model.containsAttribute("myPageForm")) {
            model.addAttribute("myPageForm", toForm(member));
        }

        normalizeWithdrawalForm(withdrawalForm);
        validateWithdrawalForm(withdrawalForm, member, bindingResult);
        if (bindingResult.hasErrors()) {
            applyPageAttributes(model, member, false, null, null, null, null, null, null);
            return "main";
        }

        try {
            memberService.withdrawMember(loginMember.getMemberId(), withdrawalForm);
            session.invalidate();
            return "redirect:/login?withdrawSuccess=true";
        } catch (IllegalArgumentException exception) {
            rejectWithdrawalFailure(bindingResult, member, exception.getMessage());
            applyPageAttributes(model, member, false, null, null, null, null, null, null);
            return "main";
        }
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
            applyPageAttributes(model, member, false, null, "Google 연동 설정이 아직 완료되지 않았습니다.", null, null, null, null);
            return "main";
        }

        if (!isValidGoogleCsrf(csrfCookie, csrfToken)) {
            applyPageAttributes(model, member, false, null, "Google 연동 요청 검증에 실패했습니다. 다시 시도해 주세요.", null, null, null, null);
            return "main";
        }

        GoogleUserProfile googleUserProfile = googleAuthService.verifyCredential(credential).orElse(null);
        if (googleUserProfile == null) {
            applyPageAttributes(model, member, false, null, "Google 계정 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.", null, null, null, null);
            return "main";
        }

        try {
            memberService.linkGoogleAccount(loginMember.getMemberId(), googleUserProfile);
            return "redirect:/mypage?googleLinkStatus=success";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            applyPageAttributes(model, member, false, null, exception.getMessage(), null, null, null, null);
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

    @GetMapping("/mypage/link/naver")
    public String linkNaverAccount(HttpSession session) {
        SessionMember loginMember = getLoginMember(session);
        if (loginMember == null) {
            return "redirect:/login";
        }

        Optional<Member> memberOptional = memberService.findById(loginMember.getMemberId());
        if (memberOptional.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }

        if (!naverAuthService.isEnabled()) {
            return "redirect:/mypage?naverLinkStatus=config-error";
        }

        session.setAttribute(AuthController.PENDING_NAVER_LINK_MEMBER_ID, loginMember.getMemberId());
        String naverAuthorizationUrl = naverAuthService.prepareAuthorizationUrl(session);
        if (!StringUtils.hasText(naverAuthorizationUrl)) {
            session.removeAttribute(AuthController.PENDING_NAVER_LINK_MEMBER_ID);
            return "redirect:/mypage?naverLinkStatus=config-error";
        }

        return "redirect:" + naverAuthorizationUrl;
    }

    @PostMapping("/mypage/unlink/naver")
    public String unlinkNaverAccount(HttpSession session) {
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
            memberService.unlinkNaverAccount(loginMember.getMemberId());
            return "redirect:/mypage?naverUnlinkStatus=success";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return "redirect:/mypage?naverUnlinkStatus=" + resolveNaverUnlinkFailureStatus(exception.getMessage());
        }
    }

    private void applyPageAttributes(Model model,
                                     Member member,
                                     boolean success,
                                     String googleLinkSuccessMessage,
                                     String googleLinkErrorMessage,
                                     String kakaoLinkSuccessMessage,
                                     String kakaoLinkErrorMessage,
                                     String naverLinkSuccessMessage,
                                     String naverLinkErrorMessage) {
        Optional<SocialAccount> googleAccount = memberService.findSocialAccount(member.getMemberId(), SocialAccount.Provider.GOOGLE);
        Optional<SocialAccount> kakaoAccount = memberService.findSocialAccount(member.getMemberId(), SocialAccount.Provider.KAKAO);
        Optional<SocialAccount> naverAccount = memberService.findSocialAccount(member.getMemberId(), SocialAccount.Provider.NAVER);
        LifestylePatternAnalysisDto lifePatternAnalysis = lifestylePatternService.analyze(member.getMemberId());
        String weightGoalMessage = buildWeightGoalMessage(member);
        AiOneLineCommentDto aiOneLineComment = aiOneLineCommentService.generateComment(member, lifePatternAnalysis, weightGoalMessage);

        model.addAttribute("pageTitle", "마이페이지");
        model.addAttribute("centerFragment", "fragments/center-mypage :: centerMyPage");
        model.addAttribute("extraCssFiles", List.of("/css/mypage.css"));
        model.addAttribute("extraJsFiles", List.of("/js/mypage.js"));
        model.addAttribute("aiOneLineComment", aiOneLineComment);
        model.addAttribute("lifePatternAnalysis", lifePatternAnalysis);
        model.addAttribute("member", member);
        model.addAttribute("settingsUpdated", success);
        model.addAttribute("withdrawalRequiresPassword", StringUtils.hasText(member.getPasswordHash()));
        model.addAttribute("genderLabel", formatGender(member.getGender()));
        model.addAttribute("loginTypeLabel", formatLoginType(member.getLoginType()));
        model.addAttribute("weightGoalMessage", weightGoalMessage);
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
        model.addAttribute("naverAuthEnabled", naverAuthService.isEnabled());
        model.addAttribute("naverLinkUrl", "/mypage/link/naver");
        model.addAttribute("naverUnlinkUrl", "/mypage/unlink/naver");
        model.addAttribute("naverLinked", naverAccount.isPresent());
        model.addAttribute("naverLinkedEmail", naverAccount.map(SocialAccount::getProviderEmail).orElse(null));
        model.addAttribute("naverLinkedProviderUserId", naverAccount.map(SocialAccount::getProviderUserId).orElse(null));
        model.addAttribute("naverLinkedAt", naverAccount.map(SocialAccount::getConnectedAt).orElse(null));
        model.addAttribute("naverLastLoginAt", naverAccount.map(SocialAccount::getLastLoginAt).orElse(null));
        model.addAttribute("naverLinkSuccessMessage", naverLinkSuccessMessage);
        model.addAttribute("naverLinkErrorMessage", naverLinkErrorMessage);
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

    private void normalizeWithdrawalForm(WithdrawalForm withdrawalForm) {
        if (withdrawalForm == null) {
            return;
        }
        withdrawalForm.setConfirmationText(trimToNull(withdrawalForm.getConfirmationText()));
    }

    private void validateWithdrawalForm(WithdrawalForm withdrawalForm, Member member, BindingResult bindingResult) {
        if (withdrawalForm == null || member == null) {
            bindingResult.reject("withdrawalFailed", "회원탈퇴 요청을 처리할 수 없습니다.");
            return;
        }

        if (StringUtils.hasText(member.getPasswordHash())) {
            if (!StringUtils.hasText(withdrawalForm.getCurrentPassword())) {
                bindingResult.rejectValue("currentPassword", "required", "현재 비밀번호를 입력해 주세요.");
            }
            return;
        }

        if (!StringUtils.hasText(withdrawalForm.getConfirmationText())) {
            bindingResult.rejectValue("confirmationText", "required", "확인 문구를 입력해 주세요.");
            return;
        }
        if (!MemberService.WITHDRAWAL_CONFIRMATION_TEXT.equals(withdrawalForm.getConfirmationText())) {
            bindingResult.rejectValue("confirmationText", "mismatch", "확인 문구를 정확히 입력해 주세요.");
        }
    }

    private void rejectWithdrawalFailure(BindingResult bindingResult, Member member, String message) {
        if (StringUtils.hasText(member.getPasswordHash()) && "현재 비밀번호가 일치하지 않습니다.".equals(message)) {
            bindingResult.rejectValue("currentPassword", "mismatch", message);
            return;
        }
        if (!StringUtils.hasText(member.getPasswordHash()) && "확인 문구를 정확히 입력해 주세요.".equals(message)) {
            bindingResult.rejectValue("confirmationText", "mismatch", message);
            return;
        }
        bindingResult.reject("withdrawalFailed", message);
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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
            case "failed" -> "Google 계정 연동 중 오류가 발생했습니다. 다시 시도해 주세요.";
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
            case "failed" -> "Google 계정 연동 해제 중 오류가 발생했습니다. 다시 시도해 주세요.";
            default -> null;
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
            case "oauth-error", "failed" -> "카카오 계정 연동 중 오류가 발생했습니다. 다시 시도해 주세요.";
            default -> null;
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
            case "failed" -> "카카오 계정 연동 해제 중 오류가 발생했습니다. 다시 시도해 주세요.";
            default -> null;
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

    private String resolveNaverSuccessMessage(String naverLinkStatus, String naverUnlinkStatus) {
        String unlinkSuccessMessage = resolveNaverUnlinkSuccessMessage(naverUnlinkStatus);
        return unlinkSuccessMessage != null ? unlinkSuccessMessage : resolveNaverLinkSuccessMessage(naverLinkStatus);
    }

    private String resolveNaverErrorMessage(String naverLinkStatus, String naverUnlinkStatus, String sessionErrorMessage) {
        if (StringUtils.hasText(sessionErrorMessage)) {
            return sessionErrorMessage;
        }
        String unlinkErrorMessage = resolveNaverUnlinkErrorMessage(naverUnlinkStatus);
        return unlinkErrorMessage != null ? unlinkErrorMessage : resolveNaverLinkErrorMessage(naverLinkStatus);
    }

    private String resolveNaverLinkSuccessMessage(String naverLinkStatus) {
        if (!StringUtils.hasText(naverLinkStatus)) {
            return null;
        }
        return "success".equals(naverLinkStatus) ? "네이버 계정이 성공적으로 연동되었습니다." : null;
    }

    private String resolveNaverLinkErrorMessage(String naverLinkStatus) {
        if (!StringUtils.hasText(naverLinkStatus)) {
            return null;
        }
        return switch (naverLinkStatus) {
            case "config-error" -> "네이버 연동 설정이 아직 완료되지 않았습니다.";
            case "state-failed" -> "네이버 연동 요청 검증에 실패했습니다. 다시 시도해 주세요.";
            case "auth-failed" -> "네이버 계정 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.";
            case "access-denied" -> "네이버 연동 동의가 취소되었습니다. 다시 시도해 주세요.";
            case "already-linked" -> "이미 네이버 계정이 연동되어 있습니다.";
            case "linked-other-account" -> "이미 다른 계정에 연결된 네이버 계정입니다.";
            case "member-not-found" -> "회원 정보를 찾을 수 없어 네이버 연동을 진행할 수 없습니다.";
            case "oauth-error", "failed" -> "네이버 계정 연동 중 오류가 발생했습니다. 다시 시도해 주세요.";
            default -> null;
        };
    }

    private String resolveNaverUnlinkSuccessMessage(String naverUnlinkStatus) {
        if (!StringUtils.hasText(naverUnlinkStatus)) {
            return null;
        }
        return "success".equals(naverUnlinkStatus) ? "네이버 계정 연동이 해제되었습니다." : null;
    }

    private String resolveNaverUnlinkErrorMessage(String naverUnlinkStatus) {
        if (!StringUtils.hasText(naverUnlinkStatus)) {
            return null;
        }
        return switch (naverUnlinkStatus) {
            case "not-linked" -> "연동된 네이버 계정이 없습니다.";
            case "last-login-method" -> "마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 수단을 먼저 연결해 주세요.";
            case "failed" -> "네이버 계정 연동 해제 중 오류가 발생했습니다. 다시 시도해 주세요.";
            default -> null;
        };
    }

    private String resolveNaverUnlinkFailureStatus(String message) {
        if ("연동된 네이버 계정이 없습니다.".equals(message)) {
            return "not-linked";
        }
        if ("마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 수단을 먼저 연결해 주세요.".equals(message)) {
            return "last-login-method";
        }
        return "failed";
    }

    private String consumeSessionString(HttpSession session, String attributeName) {
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute(attributeName);
        session.removeAttribute(attributeName);
        return attribute instanceof String stringValue && StringUtils.hasText(stringValue) ? stringValue : null;
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

