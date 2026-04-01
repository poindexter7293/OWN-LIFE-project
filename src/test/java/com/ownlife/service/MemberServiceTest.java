package com.ownlife.service;

import com.ownlife.dto.MyPageForm;
import com.ownlife.dto.SignupForm;
import com.ownlife.entity.Member;
import com.ownlife.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberServiceTest {

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        InMemoryMemberRepositoryHandler handler = new InMemoryMemberRepositoryHandler();
        MemberRepository repository = (MemberRepository) Proxy.newProxyInstance(
                MemberRepository.class.getClassLoader(),
                new Class[]{MemberRepository.class},
                handler
        );
        memberService = new MemberService(repository);
    }

    @Test
    @DisplayName("회원가입 시 비밀번호를 해시 저장하고 정상 비밀번호로 로그인할 수 있다")
    void registerAndAuthenticate() {
        SignupForm signupForm = new SignupForm();
        signupForm.setUsername("Tester01");
        signupForm.setPassword("Password123!");
        signupForm.setNickname("테스터");
        signupForm.setEmail("tester@example.com");

        Member savedMember = memberService.register(signupForm);

        assertNotNull(savedMember.getPasswordHash());
        assertTrue(savedMember.getPasswordHash().startsWith("pbkdf2$"));
        assertTrue(memberService.authenticate("tester01", "Password123!").isPresent());
        assertFalse(memberService.authenticate("tester01", "WrongPassword!").isPresent());
    }

    @Test
    @DisplayName("비활성 회원은 로그인할 수 없다")
    void inactiveMemberCannotLogin() {
        SignupForm signupForm = new SignupForm();
        signupForm.setUsername("inactive01");
        signupForm.setPassword("Password123!");
        signupForm.setNickname("비활성유저");
        signupForm.setEmail("inactive@example.com");

        Member member = memberService.register(signupForm);
        member.setStatus(Member.Status.INACTIVE);

        assertFalse(memberService.authenticate("inactive01", "Password123!").isPresent());
    }

    @Test
    @DisplayName("마이페이지 설정 수정 시 현재 체중과 목표 정보를 저장한다")
    void updateMyPageSettings() {
        SignupForm signupForm = new SignupForm();
        signupForm.setUsername("mypage01");
        signupForm.setPassword("Password123!");
        signupForm.setNickname("마이페이지유저");
        signupForm.setEmail("mypage@example.com");

        Member savedMember = memberService.register(signupForm);

        MyPageForm myPageForm = new MyPageForm();
        myPageForm.setWeightKg(new BigDecimal("68.4"));
        myPageForm.setGoalWeight(new BigDecimal("63.0"));
        myPageForm.setGoalEatKcal(1800);
        myPageForm.setGoalBurnedKcal(500);

        Member updatedMember = memberService.updateMyPageSettings(savedMember.getMemberId(), myPageForm);

        assertTrue(memberService.findById(savedMember.getMemberId()).isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("68.4"), updatedMember.getWeightKg());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("63.0"), updatedMember.getGoalWeight());
        org.junit.jupiter.api.Assertions.assertEquals(1800, updatedMember.getGoalEatKcal());
        org.junit.jupiter.api.Assertions.assertEquals(500, updatedMember.getGoalBurnedKcal());
    }

    private static class InMemoryMemberRepositoryHandler implements InvocationHandler {

        private final Map<String, Member> storage = new HashMap<>();
        private final AtomicLong sequence = new AtomicLong(1L);

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();

            return switch (methodName) {
                case "existsByUsername" -> storage.containsKey(((String) args[0]).toLowerCase());
                case "existsByEmail" -> storage.values().stream().anyMatch(member -> member.getEmail() != null && member.getEmail().equalsIgnoreCase((String) args[0]));
                case "findByUsername" -> Optional.ofNullable(storage.get(((String) args[0]).toLowerCase()));
                case "findById" -> storage.values().stream().filter(member -> member.getMemberId().equals(args[0])).findFirst();
                case "saveAndFlush", "save" -> saveMember((Member) args[0]);
                case "toString" -> "InMemoryMemberRepository";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("지원하지 않는 메서드: " + methodName);
            };
        }

        private Member saveMember(Member member) {
            if (member.getMemberId() == null) {
                member.setMemberId(sequence.getAndIncrement());
            }
            storage.put(member.getUsername().toLowerCase(), member);
            return member;
        }
    }
}

