package com.ownlife.controller;

import com.ownlife.dto.SignupForm;
import com.ownlife.entity.Member;
import com.ownlife.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
        mockMvc = MockMvcBuilders.standaloneSetup(new MemberController(memberService)).build();
    }

    @Test
    @DisplayName("회원가입 페이지를 렌더링한다")
    void signupPage() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("signupForm"))
                .andExpect(model().attribute("pageTitle", "회원가입"))
                .andExpect(model().attribute("centerFragment", "fragments/center-signup :: centerSignup"));
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

    private static class StubMemberService extends MemberService {

        private String duplicateUsername;
        private String duplicateEmail;
        private int registerCallCount;

        StubMemberService() {
            super(null);
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
        public Member register(SignupForm signupForm) {
            registerCallCount++;
            return new Member();
        }
    }
}


