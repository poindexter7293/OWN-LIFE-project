package com.ownlife.controller;

import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
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

    @BeforeEach
    void setUp() {
        memberService = new StubMemberService();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(memberService)).build();
    }

    @Test
    @DisplayName("로그인 페이지를 렌더링한다")
    void loginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("loginForm"))
                .andExpect(model().attribute("pageTitle", "로그인"))
                .andExpect(model().attribute("centerFragment", "fragments/center-login :: centerLogin"));
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

    private static class StubMemberService extends MemberService {

        private final Member member;

        StubMemberService() {
            super(null);
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
    }
}




