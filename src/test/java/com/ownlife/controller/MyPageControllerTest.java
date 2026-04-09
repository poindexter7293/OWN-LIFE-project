package com.ownlife.controller;

import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.AiOneLineCommentDto;
import com.ownlife.dto.LifestyleInsightDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MyPageControllerTest {

    private MockMvc mockMvc;
    private StubMemberService memberService;
    private StubAiOneLineCommentService aiOneLineCommentService;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        memberService = new StubMemberService();
        StubGoogleAuthService googleAuthService = new StubGoogleAuthService();
        StubKakaoAuthService kakaoAuthService = new StubKakaoAuthService();
        StubNaverAuthService naverAuthService = new StubNaverAuthService();
        StubLifestylePatternService lifestylePatternService = new StubLifestylePatternService();
        aiOneLineCommentService = new StubAiOneLineCommentService();
        mockMvc = MockMvcBuilders.standaloneSetup(new MyPageController(memberService, googleAuthService, kakaoAuthService, naverAuthService, lifestylePatternService, aiOneLineCommentService)).build();
        session = new MockHttpSession();
        session.setAttribute(AuthController.LOGIN_MEMBER, new SessionMember(1L, "tester01", "테스터", Member.Role.USER));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 마이페이지에 접근하면 로그인 페이지로 이동한다")
    void myPageRequiresLogin() throws Exception {
        mockMvc.perform(get("/mypage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("로그인한 사용자는 마이페이지를 렌더링할 수 있다")
    void myPageView() throws Exception {
        mockMvc.perform(get("/mypage").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("myPageForm"))
                .andExpect(model().attribute("aiCommentEndpoint", "/mypage/ai-comment"))
                .andExpect(model().attributeExists("lifePatternAnalysis"))
                .andExpect(model().attribute("pageTitle", "마이페이지"))
                .andExpect(model().attribute("centerFragment", "fragments/center-mypage :: centerMyPage"))
                .andExpect(model().attribute("googleLinked", false))
                .andExpect(model().attribute("kakaoLinked", false))
                .andExpect(model().attribute("naverLinked", false));

        org.junit.jupiter.api.Assertions.assertEquals(0, aiOneLineCommentService.generateCallCount);
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 마이페이지 AI 코멘트 조회를 요청하면 401을 반환한다")
    void myPageAiCommentRequiresLogin() throws Exception {
        mockMvc.perform(get("/mypage/ai-comment"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("마이페이지 AI 코멘트는 별도 JSON 응답으로 조회한다")
    void myPageAiCommentReturnsJson() throws Exception {
        mockMvc.perform(get("/mypage/ai-comment").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("최근 기록 흐름이 안정적이에요. 지금 페이스를 유지해 보세요."))
                .andExpect(jsonPath("$.detail").value("테스트용 AI 코멘트"))
                .andExpect(jsonPath("$.badgeLabel").value("AI 코멘트"))
                .andExpect(jsonPath("$.fallback").value(false));

        org.junit.jupiter.api.Assertions.assertEquals(1, aiOneLineCommentService.generateCallCount);
    }

    @Test
    @DisplayName("마이페이지 AI 코멘트가 fallback이면 JSON 응답에서도 구분할 수 있다")
    void myPageAiCommentReturnsFallbackJson() throws Exception {
        aiOneLineCommentService.response = AiOneLineCommentDto.builder()
                .message("최근 기록을 바탕으로 먼저 준비한 코멘트예요.")
                .detail("최근 기록 기반 코멘트")
                .tone("muted")
                .badgeLabel("기본 코멘트")
                .fallback(true)
                .build();

        mockMvc.perform(get("/mypage/ai-comment").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.badgeLabel").value("기본 코멘트"))
                .andExpect(jsonPath("$.fallback").value(true));
    }

    @Test
    @DisplayName("소셜 계정 연동 성공 상태에서는 성공 메시지만 표시하고 에러 메시지는 표시하지 않는다")
    void myPageShowsOnlySuccessMessageForLinkSuccessStatuses() throws Exception {
        mockMvc.perform(get("/mypage")
                        .session(session)
                        .param("googleLinkStatus", "success")
                        .param("kakaoLinkStatus", "success")
                        .param("naverLinkStatus", "success"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("googleLinkSuccessMessage", "Google 계정이 성공적으로 연동되었습니다."))
                .andExpect(model().attribute("googleLinkErrorMessage", org.hamcrest.Matchers.nullValue()))
                .andExpect(model().attribute("kakaoLinkSuccessMessage", "카카오 계정이 성공적으로 연동되었습니다."))
                .andExpect(model().attribute("kakaoLinkErrorMessage", org.hamcrest.Matchers.nullValue()))
                .andExpect(model().attribute("naverLinkSuccessMessage", "네이버 계정이 성공적으로 연동되었습니다."))
                .andExpect(model().attribute("naverLinkErrorMessage", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("소셜 계정 연동해제 성공 상태에서는 성공 메시지만 표시하고 에러 메시지는 표시하지 않는다")
    void myPageShowsOnlySuccessMessageForUnlinkSuccessStatuses() throws Exception {
        mockMvc.perform(get("/mypage")
                        .session(session)
                        .param("googleUnlinkStatus", "success")
                        .param("kakaoUnlinkStatus", "success")
                        .param("naverUnlinkStatus", "success"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("googleLinkSuccessMessage", "Google 계정 연동이 해제되었습니다."))
                .andExpect(model().attribute("googleLinkErrorMessage", org.hamcrest.Matchers.nullValue()))
                .andExpect(model().attribute("kakaoLinkSuccessMessage", "카카오 계정 연동이 해제되었습니다."))
                .andExpect(model().attribute("kakaoLinkErrorMessage", org.hamcrest.Matchers.nullValue()))
                .andExpect(model().attribute("naverLinkSuccessMessage", "네이버 계정 연동이 해제되었습니다."))
                .andExpect(model().attribute("naverLinkErrorMessage", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("유효한 마이페이지 설정 수정은 저장 후 성공 메시지와 함께 리다이렉트한다")
    void updateMyPageSuccess() throws Exception {
        mockMvc.perform(post("/mypage")
                        .session(session)
                        .param("weightKg", "68.4")
                        .param("goalWeight", "63.0")
                        .param("goalEatKcal", "1800")
                        .param("goalBurnedKcal", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage?success=true"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.updateCallCount);
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("68.4"), memberService.member.getWeightKg());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("63.0"), memberService.member.getGoalWeight());
        org.junit.jupiter.api.Assertions.assertEquals(1800, memberService.member.getGoalEatKcal());
        org.junit.jupiter.api.Assertions.assertEquals(500, memberService.member.getGoalBurnedKcal());
    }

    @Test
    @DisplayName("잘못된 마이페이지 설정 입력은 오류와 함께 같은 화면을 다시 보여준다")
    void updateMyPageValidationFail() throws Exception {
        mockMvc.perform(post("/mypage")
                        .session(session)
                        .param("weightKg", "-1")
                        .param("goalWeight", "0")
                        .param("goalEatKcal", "-10")
                        .param("goalBurnedKcal", "10001"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeHasFieldErrors("myPageForm", "weightKg", "goalWeight", "goalEatKcal", "goalBurnedKcal"));

        org.junit.jupiter.api.Assertions.assertEquals(0, memberService.updateCallCount);
    }

    @Test
    @DisplayName("마이페이지에서 Google 연동 성공 시 성공 메시지와 함께 리다이렉트한다")
    void linkGoogleAccountSuccess() throws Exception {
        mockMvc.perform(post("/mypage/link/google")
                        .session(session)
                        .cookie(new org.springframework.mock.web.MockCookie("g_csrf_token", "csrf-token"))
                        .param("g_csrf_token", "csrf-token")
                        .param("credential", "valid-google-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage?googleLinkStatus=success"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.linkGoogleCallCount);
    }

    @Test
    @DisplayName("마이페이지에서 이미 연동된 Google 계정이면 안내 메시지를 보여준다")
    void linkGoogleAccountAlreadyLinked() throws Exception {
        memberService.googleLinked = true;

        mockMvc.perform(post("/mypage/link/google")
                        .session(session)
                        .cookie(new org.springframework.mock.web.MockCookie("g_csrf_token", "csrf-token"))
                        .param("g_csrf_token", "csrf-token")
                        .param("credential", "valid-google-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("googleLinkErrorMessage", "이미 Google 계정이 연동되어 있습니다."));
    }

    @Test
    @DisplayName("마이페이지에서 Google 연동해제 성공 시 성공 메시지와 함께 리다이렉트한다")
    void unlinkGoogleAccountSuccess() throws Exception {
        memberService.googleLinked = true;

        mockMvc.perform(post("/mypage/unlink/google").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage?googleUnlinkStatus=success"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.unlinkGoogleCallCount);
    }

    @Test
    @DisplayName("마이페이지에서 마지막 로그인 수단인 Google 계정은 연동해제할 수 없다")
    void unlinkGoogleAccountFailsWhenLastLoginMethod() throws Exception {
        memberService.googleLinked = true;
        memberService.failGoogleUnlinkWithLastLoginMethod = true;

        mockMvc.perform(post("/mypage/unlink/google").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage?googleUnlinkStatus=last-login-method"));
    }

    @Test
    @DisplayName("마이페이지에서 카카오 연동을 시작하면 카카오 인증 페이지로 이동한다")
    void linkKakaoAccountRedirectsToAuthorizationPage() throws Exception {
        mockMvc.perform(get("/mypage/link/kakao").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=test-kakao-key&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Flogin%2Fkakao%2Fauth&scope=account_email+profile_nickname+profile_image&state=test-kakao-state"));

        org.junit.jupiter.api.Assertions.assertEquals(1L, session.getAttribute(AuthController.PENDING_KAKAO_LINK_MEMBER_ID));
    }

    @Test
    @DisplayName("마이페이지에서 카카오 연동해제 성공 시 성공 메시지와 함께 리다이렉트한다")
    void unlinkKakaoAccountSuccess() throws Exception {
        memberService.kakaoLinked = true;

        mockMvc.perform(post("/mypage/unlink/kakao").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage?kakaoUnlinkStatus=success"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.unlinkKakaoCallCount);
    }

    @Test
    @DisplayName("마이페이지에서 마지막 로그인 수단인 카카오 계정은 연동해제할 수 없다")
    void unlinkKakaoAccountFailsWhenLastLoginMethod() throws Exception {
        memberService.kakaoLinked = true;
        memberService.failKakaoUnlinkWithLastLoginMethod = true;

        mockMvc.perform(post("/mypage/unlink/kakao").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage?kakaoUnlinkStatus=last-login-method"));
    }

    @Test
    @DisplayName("마이페이지에서 네이버 연동을 시작하면 네이버 인증 페이지로 이동한다")
    void linkNaverAccountRedirectsToAuthorizationPage() throws Exception {
        mockMvc.perform(get("/mypage/link/naver").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=test-naver-id&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Flogin%2Fnaver%2Fauth&state=test-naver-state"));

        org.junit.jupiter.api.Assertions.assertEquals(1L, session.getAttribute(AuthController.PENDING_NAVER_LINK_MEMBER_ID));
    }

    @Test
    @DisplayName("마이페이지에서 네이버 연동해제 성공 시 성공 메시지와 함께 리다이렉트한다")
    void unlinkNaverAccountSuccess() throws Exception {
        memberService.naverLinked = true;

        mockMvc.perform(post("/mypage/unlink/naver").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage?naverUnlinkStatus=success"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.unlinkNaverCallCount);
    }

    @Test
    @DisplayName("로컬 계정 회원탈퇴가 완료되면 로그인 페이지로 이동하고 세션이 종료된다")
    void withdrawLocalMemberSuccess() throws Exception {
        mockMvc.perform(post("/mypage/withdraw")
                        .session(session)
                        .param("currentPassword", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?withdrawSuccess=true"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.withdrawCallCount);
        org.junit.jupiter.api.Assertions.assertEquals(Member.Status.DELETED, memberService.member.getStatus());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> session.getAttribute(AuthController.LOGIN_MEMBER));
    }

    @Test
    @DisplayName("로컬 계정 회원탈퇴 시 비밀번호가 틀리면 같은 화면에 오류를 표시한다")
    void withdrawLocalMemberFailsWithWrongPassword() throws Exception {
        mockMvc.perform(post("/mypage/withdraw")
                        .session(session)
                        .param("currentPassword", "WrongPassword!"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeHasFieldErrors("withdrawalForm", "currentPassword"));
    }

    @Test
    @DisplayName("소셜 전용 계정 회원탈퇴는 확인 문구 입력으로 진행할 수 있다")
    void withdrawSocialOnlyMemberWithConfirmationText() throws Exception {
        memberService.member.setPasswordHash(null);
        memberService.member.setLoginType(Member.LoginType.NAVER);

        mockMvc.perform(post("/mypage/withdraw")
                        .session(session)
                        .param("confirmationText", MemberService.WITHDRAWAL_CONFIRMATION_TEXT))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?withdrawSuccess=true"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.withdrawCallCount);
    }

    private static class StubMemberService extends MemberService {

        private final Member member;
        private int updateCallCount;
        private int linkGoogleCallCount;
        private int unlinkGoogleCallCount;
        private int unlinkKakaoCallCount;
        private int unlinkNaverCallCount;
        private int withdrawCallCount;
        private boolean googleLinked;
        private boolean kakaoLinked;
        private boolean naverLinked;
        private boolean failGoogleUnlinkWithLastLoginMethod;
        private boolean failKakaoUnlinkWithLastLoginMethod;

        StubMemberService() {
            super(null, null, null);
            member = new Member();
            member.setMemberId(1L);
            member.setUsername("tester01");
            member.setNickname("테스터");
            member.setEmail("tester@example.com");
            member.setGender(Member.Gender.M);
            member.setHeightCm(new BigDecimal("175.5"));
            member.setWeightKg(new BigDecimal("72.0"));
            member.setGoalWeight(new BigDecimal("68.0"));
            member.setGoalEatKcal(2200);
            member.setGoalBurnedKcal(500);
            member.setRole(Member.Role.USER);
            member.setStatus(Member.Status.ACTIVE);
            member.setLoginType(Member.LoginType.LOCAL);
            member.setPasswordHash("stored-password");
        }

        @Override
        public Optional<Member> findById(Long memberId) {
            return memberId != null && memberId.equals(member.getMemberId()) ? Optional.of(member) : Optional.empty();
        }

        @Override
        public Optional<SocialAccount> findSocialAccount(Long memberId, SocialAccount.Provider provider) {
            if (googleLinked && memberId != null && memberId.equals(member.getMemberId()) && provider == SocialAccount.Provider.GOOGLE) {
                SocialAccount socialAccount = new SocialAccount();
                socialAccount.setMember(member);
                socialAccount.setProvider(SocialAccount.Provider.GOOGLE);
                socialAccount.setProviderEmail("linked@gmail.com");
                return Optional.of(socialAccount);
            }
            if (kakaoLinked && memberId != null && memberId.equals(member.getMemberId()) && provider == SocialAccount.Provider.KAKAO) {
                SocialAccount socialAccount = new SocialAccount();
                socialAccount.setMember(member);
                socialAccount.setProvider(SocialAccount.Provider.KAKAO);
                socialAccount.setProviderEmail("linked@kakao.com");
                return Optional.of(socialAccount);
            }
            if (naverLinked && memberId != null && memberId.equals(member.getMemberId()) && provider == SocialAccount.Provider.NAVER) {
                SocialAccount socialAccount = new SocialAccount();
                socialAccount.setMember(member);
                socialAccount.setProvider(SocialAccount.Provider.NAVER);
                socialAccount.setProviderEmail("linked@naver.com");
                return Optional.of(socialAccount);
            }
            return Optional.empty();
        }

        @Override
        public Member updateMyPageSettings(Long memberId, MyPageForm myPageForm) {
            updateCallCount++;
            member.setWeightKg(myPageForm.getWeightKg());
            member.setGoalWeight(myPageForm.getGoalWeight());
            member.setGoalEatKcal(myPageForm.getGoalEatKcal());
            member.setGoalBurnedKcal(myPageForm.getGoalBurnedKcal());
            return member;
        }

        @Override
        public Member linkGoogleAccount(Long memberId, GoogleUserProfile googleUserProfile) {
            if (googleLinked) {
                throw new IllegalStateException("이미 Google 계정이 연동되어 있습니다.");
            }
            linkGoogleCallCount++;
            googleLinked = true;
            return member;
        }

        @Override
        public Member unlinkGoogleAccount(Long memberId) {
            if (!googleLinked) {
                throw new IllegalStateException("연동된 Google 계정이 없습니다.");
            }
            if (failGoogleUnlinkWithLastLoginMethod) {
                throw new IllegalStateException("마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 수단을 먼저 연결해 주세요.");
            }
            unlinkGoogleCallCount++;
            googleLinked = false;
            return member;
        }

        @Override
        public Member unlinkKakaoAccount(Long memberId) {
            if (!kakaoLinked) {
                throw new IllegalStateException("연동된 카카오 계정이 없습니다.");
            }
            if (failKakaoUnlinkWithLastLoginMethod) {
                throw new IllegalStateException("마지막 로그인 수단은 해제할 수 없습니다. 다른 로그인 수단을 먼저 연결해 주세요.");
            }
            unlinkKakaoCallCount++;
            kakaoLinked = false;
            return member;
        }

        @Override
        public Member unlinkNaverAccount(Long memberId) {
            if (!naverLinked) {
                throw new IllegalStateException("연동된 네이버 계정이 없습니다.");
            }
            unlinkNaverCallCount++;
            naverLinked = false;
            return member;
        }

        @Override
        public Member withdrawMember(Long memberId, WithdrawalForm withdrawalForm) {
            if (memberId == null || !memberId.equals(member.getMemberId())) {
                throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
            }
            if (member.getPasswordHash() != null) {
                if (!"Password123!".equals(withdrawalForm.getCurrentPassword())) {
                    throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
                }
            } else if (!MemberService.WITHDRAWAL_CONFIRMATION_TEXT.equals(withdrawalForm.getConfirmationText())) {
                throw new IllegalArgumentException("확인 문구를 정확히 입력해 주세요.");
            }
            withdrawCallCount++;
            member.setStatus(Member.Status.DELETED);
            member.setPasswordHash(null);
            return member;
        }
    }

    private static class StubGoogleAuthService extends GoogleAuthService {

        StubGoogleAuthService() {
            super("test-client-id", "http://localhost:8081/mypage/link/google");
        }

        @Override
        public Optional<GoogleUserProfile> verifyCredential(String credential) {
            if ("valid-google-token".equals(credential)) {
                return Optional.of(new GoogleUserProfile(
                        "google-mypage-link",
                        "linked@gmail.com",
                        "구글연동",
                        null,
                        true
                ));
            }
            return Optional.empty();
        }
    }

    private static class StubKakaoAuthService extends KakaoAuthService {

        StubKakaoAuthService() {
            super("test-kakao-key", "http://localhost:8081/login/kakao/auth");
        }

        @Override
        public String prepareAuthorizationUrl(jakarta.servlet.http.HttpSession session) {
            session.setAttribute("kakaoOauthState", "test-kakao-state");
            return "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=test-kakao-key&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Flogin%2Fkakao%2Fauth&scope=account_email+profile_nickname+profile_image&state=test-kakao-state";
        }
    }

    private static class StubNaverAuthService extends NaverAuthService {

        StubNaverAuthService() {
            super("test-naver-id", "test-naver-secret", "http://localhost:8081/login/naver/auth");
        }

        @Override
        public String prepareAuthorizationUrl(jakarta.servlet.http.HttpSession session) {
            session.setAttribute("naverOauthState", "test-naver-state");
            return "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=test-naver-id&redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Flogin%2Fnaver%2Fauth&state=test-naver-state";
        }
    }

    private static class StubLifestylePatternService extends LifestylePatternService {

        StubLifestylePatternService() {
            super(null, null);
        }

        @Override
        public LifestylePatternAnalysisDto analyze(Long memberId) {
            return LifestylePatternAnalysisDto.builder()
                    .periodLabel("최근 28일 기준")
                    .title("주말 몰아형 루틴이 눈에 띄어요")
                    .description("운동과 식단 기록을 바탕으로 생활 패턴을 분석했어요.")
                    .insights(java.util.List.of(
                            LifestyleInsightDto.builder()
                                    .title("주말 몰아 운동형")
                                    .description("주말 활동량이 평일보다 높아요.")
                                    .tone("weekend")
                                    .build()
                    ))
                    .build();
        }
    }

    private static class StubAiOneLineCommentService implements AiOneLineCommentService {

        private int generateCallCount;
        private AiOneLineCommentDto response = AiOneLineCommentDto.builder()
                .message("최근 기록 흐름이 안정적이에요. 지금 페이스를 유지해 보세요.")
                .detail("테스트용 AI 코멘트")
                .tone("balance")
                .badgeLabel("AI 코멘트")
                .fallback(false)
                .build();

        @Override
        public AiOneLineCommentDto generateComment(Member member, LifestylePatternAnalysisDto lifestylePatternAnalysis, String weightGoalMessage) {
            generateCallCount++;
            return response;
        }
    }
}

