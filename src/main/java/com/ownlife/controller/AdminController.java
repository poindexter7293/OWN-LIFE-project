package com.ownlife.controller;

import com.ownlife.dto.SessionMember;
import com.ownlife.entity.Member;
import com.ownlife.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final MemberService memberService;

    @GetMapping({"/admin", "/admin/"})
    public String adminRoot(HttpSession session) {
        SessionMember adminMember = getAdminMember(session);
        if (adminMember == null) {
            return handleUnauthorizedAccess(session);
        }

        return "redirect:/admin/members";
    }

    @GetMapping("/admin/members")
    public String memberManagement(@RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "status", required = false) Member.Status status,
                                   @RequestParam(value = "statusUpdate", required = false) String statusUpdate,
                                   @RequestParam(value = "statusError", required = false) String statusError,
                                   HttpSession session,
                                   Model model) {
        SessionMember adminMember = getAdminMember(session);
        if (adminMember == null) {
            return handleUnauthorizedAccess(session);
        }

        model.addAttribute("pageTitle", "회원 관리");
        model.addAttribute("centerFragment", "fragments/center-admin-members :: centerAdminMembers");
        model.addAttribute("extraCssFiles", List.of("/css/admin.css"));
        model.addAttribute("members", memberService.findMembersForAdmin(keyword, status));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("manageableStatuses", manageableStatuses());
        model.addAttribute("memberStatusUpdateSuccessMessage", resolveStatusUpdateSuccessMessage(statusUpdate));
        model.addAttribute("memberStatusUpdateErrorMessage", resolveStatusUpdateErrorMessage(statusError));
        return "main";
    }

    @PostMapping("/admin/members/status")
    public String changeMemberStatus(@RequestParam("memberId") Long memberId,
                                     @RequestParam("status") Member.Status status,
                                     HttpSession session) {
        SessionMember adminMember = getAdminMember(session);
        if (adminMember == null) {
            return handleUnauthorizedAccess(session);
        }

        try {
            memberService.changeMemberStatusByAdmin(adminMember.getMemberId(), memberId, status);
            return "redirect:/admin/members?statusUpdate=success";
        } catch (IllegalArgumentException exception) {
            return "redirect:/admin/members?statusError=invalid-request";
        } catch (IllegalStateException exception) {
            return "redirect:/admin/members?statusError=" + resolveStatusErrorCode(exception.getMessage());
        }
    }

    private SessionMember getAdminMember(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object sessionMember = session.getAttribute(AuthController.LOGIN_MEMBER);
        if (!(sessionMember instanceof SessionMember member)) {
            return null;
        }
        return member.getRole() == Member.Role.ADMIN ? member : null;
    }

    private String handleUnauthorizedAccess(HttpSession session) {
        Object sessionMember = session == null ? null : session.getAttribute(AuthController.LOGIN_MEMBER);
        return sessionMember == null ? "redirect:/login" : "redirect:/main";
    }

    private List<Member.Status> manageableStatuses() {
        return Arrays.stream(Member.Status.values())
                .filter(status -> status != Member.Status.DELETED)
                .toList();
    }

    private String resolveStatusUpdateSuccessMessage(String statusUpdate) {
        return "success".equals(statusUpdate) ? "회원 상태가 업데이트되었습니다." : null;
    }

    private String resolveStatusUpdateErrorMessage(String statusError) {
        if (!StringUtils.hasText(statusError)) {
            return null;
        }
        return switch (statusError) {
            case "self" -> "본인 계정 상태는 변경할 수 없습니다.";
            case "deleted" -> "탈퇴한 회원 상태는 변경할 수 없습니다.";
            case "invalid-request" -> "회원 상태 변경 요청이 올바르지 않습니다.";
            default -> "회원 상태 변경 중 오류가 발생했습니다. 다시 시도해 주세요.";
        };
    }

    private String resolveStatusErrorCode(String message) {
        if ("본인 계정 상태는 변경할 수 없습니다.".equals(message)) {
            return "self";
        }
        if ("탈퇴한 회원 상태는 변경할 수 없습니다.".equals(message)) {
            return "deleted";
        }
        return "failed";
    }
}

