package com.ownlife.controller;

import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.service.GoogleAuthService;
import com.ownlife.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AuthControllerTest {

    private MockMvc mockMvc;
    private StubMemberService memberService;
    private StubGoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        memberService = new StubMemberService();
        googleAuthService = new StubGoogleAuthService();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(memberService, googleAuthService)).build();
    }

    @Test
    @DisplayName("로그인 페이지를 렌더링한다")
    void loginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("loginForm"))
                .andExpect(model().attribute("pageTitle", "로그인"))
                .andExpect(model().attribute("centerFragment", "fragments/center-login :: centerLogin"))
                .andExpect(model().attribute("googleAuthEnabled", true));
    }

    @Test
    @DisplayName("정상 로그인 시 세션에 회원 정보를 저장하고 메인으로 이동한다")
    void loginSuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login")
                        .session(session)
                        .param("username", "tester01")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));

        Object loginMember = session.getAttribute(AuthController.LOGIN_MEMBER);
        assertNotNull(loginMember);
    }

    @Test
    @DisplayName("로그인 실패 시 같은 페이지에 오류를 표시한다")
    void loginFail() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .param("username", "tester01")
                        .param("password", "WrongPassword!"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andReturn();

        Object bindingResult = result.getModelAndView().getModel().get("org.springframework.validation.BindingResult.loginForm");
        org.junit.jupiter.api.Assertions.assertNotNull(bindingResult);
    }

    @Test
    @DisplayName("로그아웃 시 세션을 비우고 로그인 페이지로 이동한다")
    void logout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.LOGIN_MEMBER, new SessionMember(1L, "tester01", "테스터", Member.Role.USER));

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logoutSuccess=true"));

        assertThrows(IllegalStateException.class, () -> session.getAttribute(AuthController.LOGIN_MEMBER));
    }

    @Test
    @DisplayName("Google 로그인 성공 시 세션에 회원 정보를 저장하고 메인으로 이동한다")
    void googleLoginSuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        memberService.googleLoginMember = memberService.member;

        mockMvc.perform(post("/login/google/auth")
                        .session(session)
                        .cookie(new org.springframework.mock.web.MockCookie("g_csrf_token", "csrf-token"))
                        .param("g_csrf_token", "csrf-token")
                        .param("credential", "valid-google-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));

        Object loginMember = session.getAttribute(AuthController.LOGIN_MEMBER);
        assertNotNull(loginMember);
    }

    @Test
    @DisplayName("신규 Google 계정은 추가정보 회원가입 페이지로 이동하고 임시 세션을 저장한다")
    void googleLoginRedirectsToGoogleSignup() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login/google/auth")
                        .session(session)
                        .cookie(new org.springframework.mock.web.MockCookie("g_csrf_token", "csrf-token"))
                        .param("g_csrf_token", "csrf-token")
                        .param("credential", "valid-google-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup/google"));

        Object pendingSignup = session.getAttribute(AuthController.PENDING_GOOGLE_SIGNUP);
        assertNotNull(pendingSignup);
    }

    @Test
    @DisplayName("Google 로그인 요청의 CSRF 검증이 실패하면 로그인 화면에 오류를 표시한다")
    void googleLoginCsrfFail() throws Exception {
        mockMvc.perform(post("/login/google/auth")
                        .cookie(new org.springframework.mock.web.MockCookie("g_csrf_token", "csrf-cookie"))
                        .param("g_csrf_token", "different-token")
                        .param("credential", "valid-google-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("googleErrorMessage", "Google 로그인 요청 검증에 실패했습니다. 다시 시도해 주세요."));
    }

    private static class StubMemberService extends MemberService {

        private final Member member;
        private Member googleLoginMember;

        StubMemberService() {
            super(null, null, null);
            member = new Member();
            member.setMemberId(1L);
            member.setUsername("tester01");
            member.setNickname("테스터");
            member.setRole(Member.Role.USER);
            member.setStatus(Member.Status.ACTIVE);
            member.setLoginType(Member.LoginType.LOCAL);
        }

        @Override
        public Optional<Member> authenticate(String username, String rawPassword) {
            if ("tester01".equals(username) && "Password123!".equals(rawPassword)) {
                return Optional.of(member);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Member> findGoogleMemberForLogin(GoogleUserProfile googleUserProfile) {
            if (googleLoginMember == null) {
                return Optional.empty();
            }
            googleLoginMember.setLoginType(Member.LoginType.GOOGLE);
            googleLoginMember.setEmail(googleUserProfile.getEmail());
            return Optional.of(googleLoginMember);
        }
    }

    private static class StubGoogleAuthService extends GoogleAuthService {

        StubGoogleAuthService() {
            super("test-client-id", "http://localhost:8081/login/google/auth");
        }

        @Override
        public Optional<GoogleUserProfile> verifyCredential(String credential) {
            if ("valid-google-token".equals(credential)) {
                return Optional.of(new GoogleUserProfile(
                        "google-subject-1",
                        "tester@gmail.com",
                        "구글테스터",
                        null,
                        true
                ));
            }
            return Optional.empty();
        }
    }
}




