package com.ownlife.controller;

import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.MyPageForm;
import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.entity.SocialAccount;
import com.ownlife.service.GoogleAuthService;
import com.ownlife.service.MemberService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MyPageControllerTest {

    private MockMvc mockMvc;
    private StubMemberService memberService;
    private StubGoogleAuthService googleAuthService;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        memberService = new StubMemberService();
        googleAuthService = new StubGoogleAuthService();
        mockMvc = MockMvcBuilders.standaloneSetup(new MyPageController(memberService, googleAuthService)).build();
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
                .andExpect(model().attribute("pageTitle", "마이페이지"))
                .andExpect(model().attribute("centerFragment", "fragments/center-mypage :: centerMyPage"))
                .andExpect(model().attribute("googleLinked", false));
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

    private static class StubMemberService extends MemberService {

        private final Member member;
        private int updateCallCount;
        private int linkGoogleCallCount;
        private boolean googleLinked;

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
}

