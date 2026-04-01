package com.ownlife.service;

import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.MyPageForm;
import com.ownlife.dto.SignupForm;
import com.ownlife.entity.Member;
import com.ownlife.entity.MemberGoalHistory;
import com.ownlife.entity.SocialAccount;
import com.ownlife.repository.MemberGoalHistoryRepository;
import com.ownlife.repository.MemberRepository;
import com.ownlife.repository.SocialAccountRepository;
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
    private InMemoryMemberGoalHistoryRepositoryHandler goalHistoryHandler;
    private InMemorySocialAccountRepositoryHandler socialAccountHandler;

    @BeforeEach
    void setUp() {
        InMemoryMemberRepositoryHandler memberHandler = new InMemoryMemberRepositoryHandler();
        MemberRepository memberRepository = (MemberRepository) Proxy.newProxyInstance(
                MemberRepository.class.getClassLoader(),
                new Class[]{MemberRepository.class},
                memberHandler
        );
        socialAccountHandler = new InMemorySocialAccountRepositoryHandler();
        SocialAccountRepository socialAccountRepository = (SocialAccountRepository) Proxy.newProxyInstance(
                SocialAccountRepository.class.getClassLoader(),
                new Class[]{SocialAccountRepository.class},
                socialAccountHandler
        );
        goalHistoryHandler = new InMemoryMemberGoalHistoryRepositoryHandler();
        MemberGoalHistoryRepository memberGoalHistoryRepository = (MemberGoalHistoryRepository) Proxy.newProxyInstance(
                MemberGoalHistoryRepository.class.getClassLoader(),
                new Class[]{MemberGoalHistoryRepository.class},
                goalHistoryHandler
        );
        memberService = new MemberService(memberRepository, memberGoalHistoryRepository, socialAccountRepository);
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
        signupForm.setWeightKg(new BigDecimal("72.0"));

        Member savedMember = memberService.register(signupForm);
        savedMember.setGoalWeight(new BigDecimal("68.0"));
        savedMember.setGoalEatKcal(2200);
        savedMember.setGoalBurnedKcal(500);

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
        org.junit.jupiter.api.Assertions.assertEquals(1, goalHistoryHandler.storage.size());

        MemberGoalHistory memberGoalHistory = goalHistoryHandler.storage.values().iterator().next();
        org.junit.jupiter.api.Assertions.assertEquals(savedMember.getMemberId(), memberGoalHistory.getMember().getMemberId());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("68.0"), memberGoalHistory.getGoalWeight());
        org.junit.jupiter.api.Assertions.assertEquals(2200, memberGoalHistory.getGoalEatKcal());
        org.junit.jupiter.api.Assertions.assertEquals(500, memberGoalHistory.getGoalBurnedKcal());
    }

    @Test
    @DisplayName("체중만 수정하면 목표 이력은 저장하지 않는다")
    void updateMyPageWeightOnlyDoesNotSaveGoalHistory() {
        SignupForm signupForm = new SignupForm();
        signupForm.setUsername("weightonly01");
        signupForm.setPassword("Password123!");
        signupForm.setNickname("체중만변경");
        signupForm.setEmail("weightonly@example.com");
        signupForm.setWeightKg(new BigDecimal("72.0"));

        Member savedMember = memberService.register(signupForm);
        savedMember.setGoalWeight(new BigDecimal("68.0"));
        savedMember.setGoalEatKcal(2200);
        savedMember.setGoalBurnedKcal(500);

        MyPageForm myPageForm = new MyPageForm();
        myPageForm.setWeightKg(new BigDecimal("71.0"));
        myPageForm.setGoalWeight(new BigDecimal("68.00"));
        myPageForm.setGoalEatKcal(2200);
        myPageForm.setGoalBurnedKcal(500);

        memberService.updateMyPageSettings(savedMember.getMemberId(), myPageForm);

        org.junit.jupiter.api.Assertions.assertEquals(0, goalHistoryHandler.storage.size());
    }

    @Test
    @DisplayName("이미 연동된 Google 계정은 기존 회원으로 로그인한다")
    void findGoogleMemberForLogin() {
        GoogleUserProfile googleUserProfile = new GoogleUserProfile(
                "google-subject-1",
                "google@example.com",
                "구글사용자",
                "https://example.com/profile.png",
                true
        );

        SignupForm signupForm = new SignupForm();
        signupForm.setNickname("구글사용자");
        signupForm.setGender(Member.Gender.M);
        signupForm.setHeightCm(new BigDecimal("175.0"));
        signupForm.setWeightKg(new BigDecimal("70.0"));

        Member createdMember = memberService.registerGoogleMember(signupForm, googleUserProfile);
        Member loggedInMember = memberService.findGoogleMemberForLogin(googleUserProfile).orElseThrow();

        assertNotNull(createdMember.getMemberId());
        org.junit.jupiter.api.Assertions.assertEquals(Member.LoginType.GOOGLE, createdMember.getLoginType());
        org.junit.jupiter.api.Assertions.assertEquals("GOOGLE", createdMember.getSocialProvider());
        org.junit.jupiter.api.Assertions.assertEquals("google-subject-1", createdMember.getSocialProviderId());
        org.junit.jupiter.api.Assertions.assertEquals(createdMember.getMemberId(), loggedInMember.getMemberId());
        org.junit.jupiter.api.Assertions.assertEquals(1, socialAccountHandler.storage.size());
        SocialAccount socialAccount = socialAccountHandler.storage.values().iterator().next();
        org.junit.jupiter.api.Assertions.assertEquals(SocialAccount.Provider.GOOGLE, socialAccount.getProvider());
        org.junit.jupiter.api.Assertions.assertEquals("google-subject-1", socialAccount.getProviderUserId());
        org.junit.jupiter.api.Assertions.assertEquals(createdMember.getMemberId(), socialAccount.getMember().getMemberId());
    }

    @Test
    @DisplayName("신규 Google 계정은 추가정보 가입 완료 시 회원과 소셜 계정을 함께 생성한다")
    void registerGoogleMember() {
        GoogleUserProfile googleUserProfile = new GoogleUserProfile(
                "google-subject-2",
                "new-google@example.com",
                "새구글사용자",
                "https://example.com/new-profile.png",
                true
        );

        SignupForm signupForm = new SignupForm();
        signupForm.setNickname("헬스초보");
        signupForm.setGender(Member.Gender.F);
        signupForm.setHeightCm(new BigDecimal("162.4"));
        signupForm.setWeightKg(new BigDecimal("54.8"));

        Member createdMember = memberService.registerGoogleMember(signupForm, googleUserProfile);

        org.junit.jupiter.api.Assertions.assertEquals(Member.LoginType.GOOGLE, createdMember.getLoginType());
        org.junit.jupiter.api.Assertions.assertEquals("헬스초보", createdMember.getNickname());
        org.junit.jupiter.api.Assertions.assertEquals(Member.Gender.F, createdMember.getGender());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("162.4"), createdMember.getHeightCm());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("54.8"), createdMember.getWeightKg());
        org.junit.jupiter.api.Assertions.assertEquals(1, socialAccountHandler.storage.size());
    }

    @Test
    @DisplayName("동일 이메일의 기존 로컬 계정은 Google 로그인 시 자동 연결되고 로컬 로그인도 유지된다")
    void linkGoogleToExistingLocalMemberByEmail() {
        SignupForm localSignupForm = new SignupForm();
        localSignupForm.setUsername("localuser01");
        localSignupForm.setPassword("Password123!");
        localSignupForm.setNickname("로컬회원");
        localSignupForm.setEmail("shared@example.com");

        Member localMember = memberService.register(localSignupForm);

        GoogleUserProfile googleUserProfile = new GoogleUserProfile(
                "google-linked-subject",
                "shared@example.com",
                "구글이름",
                "https://example.com/google.png",
                true
        );

        Member linkedMember = memberService.findGoogleMemberForLogin(googleUserProfile).orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(localMember.getMemberId(), linkedMember.getMemberId());
        org.junit.jupiter.api.Assertions.assertEquals(Member.LoginType.LOCAL, linkedMember.getLoginType());
        org.junit.jupiter.api.Assertions.assertEquals("로컬회원", linkedMember.getNickname());
        org.junit.jupiter.api.Assertions.assertTrue(memberService.authenticate("localuser01", "Password123!").isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(1, socialAccountHandler.storage.size());

        SocialAccount socialAccount = socialAccountHandler.storage.values().iterator().next();
        org.junit.jupiter.api.Assertions.assertEquals(localMember.getMemberId(), socialAccount.getMember().getMemberId());
        org.junit.jupiter.api.Assertions.assertEquals(SocialAccount.Provider.GOOGLE, socialAccount.getProvider());
        org.junit.jupiter.api.Assertions.assertEquals("google-linked-subject", socialAccount.getProviderUserId());
    }

    @Test
    @DisplayName("마이페이지에서 현재 로그인한 계정에 Google 계정을 연동할 수 있다")
    void linkGoogleAccountFromMyPage() {
        SignupForm localSignupForm = new SignupForm();
        localSignupForm.setUsername("mypagelink01");
        localSignupForm.setPassword("Password123!");
        localSignupForm.setNickname("연동대상회원");
        localSignupForm.setEmail("local-link@example.com");

        Member member = memberService.register(localSignupForm);

        GoogleUserProfile googleUserProfile = new GoogleUserProfile(
                "google-mypage-link-subject",
                "different-google@example.com",
                "구글연동대상",
                "https://example.com/google-linked.png",
                true
        );

        Member linkedMember = memberService.linkGoogleAccount(member.getMemberId(), googleUserProfile);

        org.junit.jupiter.api.Assertions.assertEquals(member.getMemberId(), linkedMember.getMemberId());
        org.junit.jupiter.api.Assertions.assertEquals(1, socialAccountHandler.storage.size());
        SocialAccount socialAccount = socialAccountHandler.storage.values().iterator().next();
        org.junit.jupiter.api.Assertions.assertEquals("google-mypage-link-subject", socialAccount.getProviderUserId());
        org.junit.jupiter.api.Assertions.assertEquals(member.getMemberId(), socialAccount.getMember().getMemberId());
        org.junit.jupiter.api.Assertions.assertTrue(memberService.findSocialAccount(member.getMemberId(), SocialAccount.Provider.GOOGLE).isPresent());
    }

    @Test
    @DisplayName("이미 다른 회원에게 연결된 Google 계정은 마이페이지에서 다시 연동할 수 없다")
    void linkGoogleAccountFailsWhenLinkedToAnotherMember() {
        SignupForm firstForm = new SignupForm();
        firstForm.setUsername("firstuser01");
        firstForm.setPassword("Password123!");
        firstForm.setNickname("첫회원");
        firstForm.setEmail("first@example.com");

        SignupForm secondForm = new SignupForm();
        secondForm.setUsername("seconduser01");
        secondForm.setPassword("Password123!");
        secondForm.setNickname("둘째회원");
        secondForm.setEmail("second@example.com");

        Member firstMember = memberService.register(firstForm);
        Member secondMember = memberService.register(secondForm);

        GoogleUserProfile googleUserProfile = new GoogleUserProfile(
                "shared-google-subject",
                "shared-social@example.com",
                "공유구글",
                null,
                true
        );

        memberService.linkGoogleAccount(firstMember.getMemberId(), googleUserProfile);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> memberService.linkGoogleAccount(secondMember.getMemberId(), googleUserProfile));
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
                case "findByEmail" -> storage.values().stream().filter(member -> member.getEmail() != null && member.getEmail().equalsIgnoreCase((String) args[0])).findFirst();
                case "findByUsername" -> Optional.ofNullable(storage.get(((String) args[0]).toLowerCase()));
                case "findById" -> storage.values().stream().filter(member -> member.getMemberId().equals(args[0])).findFirst();
                case "findBySocialProviderAndSocialProviderId" -> storage.values().stream()
                        .filter(member -> member.getSocialProvider() != null && member.getSocialProviderId() != null)
                        .filter(member -> member.getSocialProvider().equals(args[0]) && member.getSocialProviderId().equals(args[1]))
                        .findFirst();
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

    private static class InMemorySocialAccountRepositoryHandler implements InvocationHandler {

        private final Map<Long, SocialAccount> storage = new HashMap<>();
        private final AtomicLong sequence = new AtomicLong(1L);

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();

            return switch (methodName) {
                case "findByProviderAndProviderUserId" -> storage.values().stream()
                        .filter(account -> account.getProvider() == args[0] && account.getProviderUserId().equals(args[1]))
                        .findFirst();
                case "findByMemberMemberIdAndProvider" -> storage.values().stream()
                        .filter(account -> account.getMember() != null && account.getMember().getMemberId().equals(args[0]))
                        .filter(account -> account.getProvider() == args[1])
                        .findFirst();
                case "saveAndFlush", "save" -> saveSocialAccount((SocialAccount) args[0]);
                case "toString" -> "InMemorySocialAccountRepository";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("지원하지 않는 메서드: " + methodName);
            };
        }

        private SocialAccount saveSocialAccount(SocialAccount socialAccount) {
            if (socialAccount.getSocialAccountId() == null) {
                socialAccount.setSocialAccountId(sequence.getAndIncrement());
            }
            storage.put(socialAccount.getSocialAccountId(), socialAccount);
            return socialAccount;
        }
    }

    private static class InMemoryMemberGoalHistoryRepositoryHandler implements InvocationHandler {

        private final Map<Long, MemberGoalHistory> storage = new HashMap<>();
        private final AtomicLong sequence = new AtomicLong(1L);

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();

            return switch (methodName) {
                case "saveAndFlush", "save" -> saveMemberGoalHistory((MemberGoalHistory) args[0]);
                case "toString" -> "InMemoryMemberGoalHistoryRepository";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException("지원하지 않는 메서드: " + methodName);
            };
        }

        private MemberGoalHistory saveMemberGoalHistory(MemberGoalHistory memberGoalHistory) {
            if (memberGoalHistory.getGoalHistoryId() == null) {
                memberGoalHistory.setGoalHistoryId(sequence.getAndIncrement());
            }
            storage.put(memberGoalHistory.getGoalHistoryId(), memberGoalHistory);
            return memberGoalHistory;
        }
    }
}

