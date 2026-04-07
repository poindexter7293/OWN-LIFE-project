package com.ownlife.controller;

import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminControllerTest {

    private MockMvc mockMvc;
    private StubMemberService memberService;
    private MockHttpSession adminSession;
    private MockHttpSession userSession;

    @BeforeEach
    void setUp() {
        memberService = new StubMemberService();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(memberService)).build();

        adminSession = new MockHttpSession();
        adminSession.setAttribute(AuthController.LOGIN_MEMBER, new SessionMember(99L, "admin01", "관리자", Member.Role.ADMIN));

        userSession = new MockHttpSession();
        userSession.setAttribute(AuthController.LOGIN_MEMBER, new SessionMember(1L, "user01", "일반회원", Member.Role.USER));
    }

    @Test
    @DisplayName("관리자는 회원 관리 페이지를 조회할 수 있다")
    void memberManagementPage() throws Exception {
        mockMvc.perform(get("/admin/members").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("pageTitle", "회원 관리"))
                .andExpect(model().attribute("centerFragment", "fragments/center-admin-members :: centerAdminMembers"))
                .andExpect(model().attributeExists("members"));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자는 관리자 페이지 접근 시 로그인 페이지로 이동한다")
    void adminPageRequiresLogin() throws Exception {
        mockMvc.perform(get("/admin/members"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("일반 회원은 관리자 페이지 접근 시 메인 페이지로 이동한다")
    void nonAdminCannotAccessAdminPage() throws Exception {
        mockMvc.perform(get("/admin/members").session(userSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));
    }

    @Test
    @DisplayName("관리자는 회원 상태를 변경할 수 있다")
    void adminCanChangeMemberStatus() throws Exception {
        mockMvc.perform(post("/admin/members/status")
                        .session(adminSession)
                        .param("memberId", "1")
                        .param("status", "SUSPENDED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/members?statusUpdate=success"));

        org.junit.jupiter.api.Assertions.assertEquals(1, memberService.changeStatusCallCount);
        org.junit.jupiter.api.Assertions.assertEquals(Member.Status.SUSPENDED, memberService.member.getStatus());
    }

    @Test
    @DisplayName("관리자는 본인 계정 상태를 변경할 수 없다")
    void adminCannotChangeOwnStatus() throws Exception {
        mockMvc.perform(post("/admin/members/status")
                        .session(adminSession)
                        .param("memberId", "99")
                        .param("status", "INACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/members?statusError=self"));
    }

    private static class StubMemberService extends MemberService {

        private final Member member;
        private int changeStatusCallCount;

        StubMemberService() {
            super(null, null, null);
            member = new Member();
            member.setMemberId(1L);
            member.setUsername("member01");
            member.setNickname("회원1");
            member.setEmail("member01@example.com");
            member.setRole(Member.Role.USER);
            member.setStatus(Member.Status.ACTIVE);
            member.setLoginType(Member.LoginType.LOCAL);
            member.setCreatedAt(LocalDateTime.now());
        }

        @Override
        public List<Member> findMembersForAdmin(String keyword, Member.Status status) {
            return List.of(member);
        }

        @Override
        public Member changeMemberStatusByAdmin(Long adminMemberId, Long targetMemberId, Member.Status nextStatus) {
            if (adminMemberId != null && adminMemberId.equals(targetMemberId)) {
                throw new IllegalStateException("본인 계정 상태는 변경할 수 없습니다.");
            }
            changeStatusCallCount++;
            member.setStatus(nextStatus);
            return member;
        }
    }
}

