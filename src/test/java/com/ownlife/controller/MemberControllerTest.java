package com.ownlife.controller;

import com.ownlife.entity.Member;
import com.ownlife.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

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
        given(memberService.existsByUsername(anyString())).willReturn(false);
        given(memberService.existsByEmail(anyString())).willReturn(false);
        given(memberService.register(any())).willReturn(new Member());

        mockMvc.perform(post("/signup")
                        .param("username", "홍길동")
                        .param("password", "Password123!")
                        .param("passwordConfirm", "Password123!")
                        .param("nickname", "길동이")
                        .param("email", "hong@example.com")
                        .param("gender", "M")
                        .param("birthDate", "2000-01-01")
                        .param("heightCm", "175.5")
                        .param("weightKg", "72.3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup?success=true"));

        verify(memberService).register(any());
    }

    @Test
    @DisplayName("중복 이름은 회원가입 화면에 오류와 함께 다시 표시한다")
    void signupDuplicateUsername() throws Exception {
        given(memberService.existsByUsername("홍길동")).willReturn(true);

        mockMvc.perform(post("/signup")
                        .param("username", "홍길동")
                        .param("password", "Password123!")
                        .param("passwordConfirm", "Password123!")
                        .param("nickname", "길동이")
                        .param("email", "hong@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeHasFieldErrors("signupForm", "username"));

        verify(memberService, never()).register(any());
    }

    @Test
    @DisplayName("목표 관련 값 없이도 회원가입할 수 있다")
    void signupWithoutGoalFields() throws Exception {
        given(memberService.existsByUsername(anyString())).willReturn(false);
        given(memberService.existsByEmail(anyString())).willReturn(false);
        given(memberService.register(any())).willReturn(new Member());

        mockMvc.perform(post("/signup")
                        .param("username", "김하늘")
                        .param("password", "Password123!")
                        .param("passwordConfirm", "Password123!")
                        .param("nickname", "하늘")
                        .param("email", "sky@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/signup?success=true"));

        verify(memberService).register(any());
    }
}


