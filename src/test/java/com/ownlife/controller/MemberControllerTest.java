package com.ownlife.controller;

import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.PendingGoogleSignup;
import com.ownlife.dto.SignupForm;
import com.ownlife.entity.Member;
import com.ownlife.service.GoogleAuthService;
import com.ownlife.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MemberControllerTest {

    private MockMvc mockMvc;

    private StubMemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new StubMemberService();
        mockMvc = MockMvcBuilders.standaloneSetup(new MemberController(memberService, new GoogleAuthService("test-client-id", "http://localhost:8081/login/google/auth"))).build();
    }

    @Test
    @DisplayName("회원가입 페이지를 렌더링한다")
    void signupPage() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("signupForm"))
                .andExpect(model().attribute("pageTitle", "회원가입"))
                .andExpect(model().attribute("centerFragment", "fragments/center-signup :: centerSignup"))
                .andExpect(model().attribute("googleAuthEnabled", true));
    }

    @Test
    @DisplayName("유효한 회원가입 요청은 저장 후 성공 화면으로 리다이렉트한다")
    void signupSuccess() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("username", "honggildong")
                        .param("password", "Password123!")
                        .param("passwordConfirm", "Password123!")
                        .param("nickname", "길동이")
                        .param("email", "hong@example.com")
                        .param("gender", "M")
                        .param("birthDate", "2000-01-01")
                        .param("heightCm", "175.5")
                        .param("weightKg", "72.3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.registerCallCount);
    }

    @Test
    @DisplayName("중복 아이디는 회원가입 화면에 오류와 함께 다시 표시한다")
    void signupDuplicateUsername() throws Exception {
        memberService.duplicateUsername = "honggildong";

        mockMvc.perform(post("/signup")
                        .param("username", "honggildong")
                        .param("password", "Password123!")
                        .param("passwordConfirm", "Password123!")
                        .param("nickname", "길동이")
                        .param("email", "hong@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "username"));

        org.junit.jupiter.api.Assertions.assertEquals(0, memberService.registerCallCount);
    }

    @Test
    @DisplayName("중복 닉네임은 회원가입 화면에 오류와 함께 다시 표시한다")
    void signupDuplicateNickname() throws Exception {
        memberService.duplicateNickname = "길동이";

        mockMvc.perform(post("/signup")
                        .param("username", "honggildong")
                        .param("password", "Password123!")
                        .param("passwordConfirm", "Password123!")
                        .param("nickname", "길동이")
                        .param("email", "hong@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "nickname"));

        org.junit.jupiter.api.Assertions.assertEquals(0, memberService.registerCallCount);
    }

    @Test
    @DisplayName("아이디 중복 확인 API는 사용 가능 여부를 반환한다")
    void checkUsernameAvailability() throws Exception {
        mockMvc.perform(get("/signup/check-username").param("username", "newuser01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.available").value(true));

        memberService.duplicateUsername = "takenuser";

        mockMvc.perform(get("/signup/check-username").param("username", "takenuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("이메일 중복 확인 API는 형식 오류도 함께 반환한다")
    void checkEmailAvailability() throws Exception {
        mockMvc.perform(get("/signup/check-email").param("email", "wrong-email"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("닉네임 중복 확인 API는 사용 가능 여부를 반환한다")
    void checkNicknameAvailability() throws Exception {
        mockMvc.perform(get("/signup/check-nickname").param("nickname", "새닉네임"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.available").value(true));

        memberService.duplicateNickname = "중복닉네임";

        mockMvc.perform(get("/signup/check-nickname").param("nickname", "중복닉네임"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("목표 관련 값 없이도 회원가입할 수 있다")
    void signupWithoutGoalFields() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("username", "skyblue01")
                        .param("password", "Password123!")
                        .param("passwordConfirm", "Password123!")
                        .param("nickname", "하늘")
                        .param("email", "sky@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.registerCallCount);
    }

    @Test
    @DisplayName("Google 추가정보 회원가입 페이지는 세션의 이메일과 닉네임을 미리 채워 보여준다")
    void googleSignupPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.PENDING_GOOGLE_SIGNUP, new PendingGoogleSignup("google-subject-1", "google@example.com", "구글닉네임", null));

        mockMvc.perform(get("/signup/google").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("googleSignupMode", true))
                .andExpect(model().attribute("signupAction", "/signup/google"))
                .andExpect(model().attributeExists("signupForm"));
    }

    @Test
    @DisplayName("Google 추가정보 회원가입 완료 시 회원을 생성하고 로그인 세션을 저장한다")
    void googleSignupSuccess() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.PENDING_GOOGLE_SIGNUP, new PendingGoogleSignup("google-subject-1", "google@example.com", "구글닉네임", null));

        mockMvc.perform(post("/signup/google")
                        .session(session)
                        .param("nickname", "헬스입문")
                        .param("email", "tampered@example.com")
                        .param("gender", "F")
                        .param("heightCm", "165.2")
                        .param("weightKg", "58.3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.googleRegisterCallCount);
        org.junit.jupiter.api.Assertions.assertNotNull(session.getAttribute(AuthController.LOGIN_MEMBER));
    }

    private static class StubMemberService extends MemberService {

        private String duplicateUsername;
        private String duplicateNickname;
        private String duplicateEmail;
        private int registerCallCount;
        private int googleRegisterCallCount;

        StubMemberService() {
            super(null, null, null);
        }

        @Override
        public boolean existsByUsername(String username) {
            return username != null && username.equals(duplicateUsername);
        }

        @Override
        public boolean existsByEmail(String email) {
            return email != null && email.equals(duplicateEmail);
        }

        @Override
        public boolean existsByNickname(String nickname) {
            return nickname != null && nickname.equals(duplicateNickname);
        }

        @Override
        public Member register(SignupForm signupForm) {
            registerCallCount++;
            return new Member();
        }

        @Override
        public Member registerGoogleMember(SignupForm signupForm, GoogleUserProfile googleUserProfile) {
            googleRegisterCallCount++;
            Member member = new Member();
            member.setMemberId(99L);
            member.setUsername("google_generated_99");
            member.setNickname(signupForm.getNickname());
            member.setRole(Member.Role.USER);
            member.setEmail(googleUserProfile.getEmail());
            member.setLoginType(Member.LoginType.GOOGLE);
            return member;
        }
    }
}


