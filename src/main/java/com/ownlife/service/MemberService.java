package com.ownlife.service;

import com.ownlife.dto.MyPageForm;
import com.ownlife.dto.GoogleUserProfile;
import com.ownlife.dto.SignupForm;
import com.ownlife.entity.Member;
import com.ownlife.entity.MemberGoalHistory;
import com.ownlife.entity.SocialAccount;
import com.ownlife.repository.MemberGoalHistoryRepository;
import com.ownlife.repository.MemberRepository;
import com.ownlife.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private final MemberRepository memberRepository;
    private final MemberGoalHistoryRepository memberGoalHistoryRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return StringUtils.hasText(username) && memberRepository.existsByUsername(normalizeUsername(username));
    }

    @Transactional(readOnly = true)
    public boolean existsByNickname(String nickname) {
        return StringUtils.hasText(nickname) && memberRepository.existsByNickname(trimToNull(nickname));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return StringUtils.hasText(email) && memberRepository.existsByEmail(normalizeIdentity(email));
    }

    @Transactional(readOnly = true)
    public Optional<Member> authenticate(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        if (!StringUtils.hasText(normalizedUsername) || !StringUtils.hasText(rawPassword)) {
            return Optional.empty();
        }

        return memberRepository.findByUsername(normalizedUsername)
                .filter(member -> member.getStatus() == Member.Status.ACTIVE)
                .filter(member -> StringUtils.hasText(member.getPasswordHash()))
                .filter(member -> verifyPassword(rawPassword, member.getPasswordHash()));
    }

    @Transactional(readOnly = true)
    public Optional<Member> findById(Long memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        return memberRepository.findById(memberId)
                .filter(member -> member.getStatus() == Member.Status.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<SocialAccount> findSocialAccount(Long memberId, SocialAccount.Provider provider) {
        if (memberId == null || provider == null) {
            return Optional.empty();
        }
        return socialAccountRepository.findByMemberMemberIdAndProvider(memberId, provider);
    }

    public Member register(SignupForm signupForm) {
        Member member = new Member();
        member.setUsername(normalizeUsername(signupForm.getUsername()));
        member.setPasswordHash(hashPassword(signupForm.getPassword()));
        member.setNickname(trimToNull(signupForm.getNickname()));
        member.setEmail(normalizeIdentity(signupForm.getEmail()));
        member.setGender(signupForm.getGender());
        member.setBirthDate(signupForm.getBirthDate());
        member.setHeightCm(signupForm.getHeightCm());
        member.setWeightKg(signupForm.getWeightKg());
        member.setRole(Member.Role.USER);
        member.setStatus(Member.Status.ACTIVE);
        member.setLoginType(Member.LoginType.LOCAL);
        return memberRepository.saveAndFlush(member);
    }

    public Member updateMyPageSettings(Long memberId, MyPageForm myPageForm) {
        Member member = findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        if (hasGoalChanges(member, myPageForm)) {
            savePreviousGoalHistory(member);
        }

        member.setWeightKg(myPageForm.getWeightKg());
        member.setGoalWeight(myPageForm.getGoalWeight());
        member.setGoalEatKcal(myPageForm.getGoalEatKcal());
        member.setGoalBurnedKcal(myPageForm.getGoalBurnedKcal());

        return memberRepository.saveAndFlush(member);
    }

    public Optional<Member> findGoogleMemberForLogin(GoogleUserProfile googleUserProfile) {
        if (googleUserProfile == null) {
            throw new IllegalArgumentException("Google 인증 정보가 올바르지 않습니다.");
        }
        if (!googleUserProfile.isEmailVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료된 Google 계정만 사용할 수 있습니다.");
        }

        Optional<SocialAccount> existingSocialAccount = socialAccountRepository.findByProviderAndProviderUserId(SocialAccount.Provider.GOOGLE, googleUserProfile.getSubject());
        if (existingSocialAccount.isPresent()) {
            Member member = existingSocialAccount.get().getMember();
            validateActiveMember(member);
            refreshGoogleProfile(member, googleUserProfile);
            upsertGoogleSocialAccount(member, googleUserProfile);
            return Optional.of(memberRepository.saveAndFlush(member));
        }

        Optional<Member> existingEmailMember = memberRepository.findByEmail(normalizeIdentity(googleUserProfile.getEmail()));
        if (existingEmailMember.isPresent()) {
            Member member = existingEmailMember.get();
            validateActiveMember(member);
            refreshGoogleProfile(member, googleUserProfile);
            upsertGoogleSocialAccount(member, googleUserProfile);
            return Optional.of(memberRepository.saveAndFlush(member));
        }

        return Optional.empty();
    }

    public Member registerGoogleMember(SignupForm signupForm, GoogleUserProfile googleUserProfile) {
        if (signupForm == null) {
            throw new IllegalArgumentException("회원가입 정보가 올바르지 않습니다.");
        }
        if (googleUserProfile == null) {
            throw new IllegalArgumentException("Google 인증 정보가 올바르지 않습니다.");
        }
        if (!googleUserProfile.isEmailVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료된 Google 계정만 사용할 수 있습니다.");
        }

        Optional<Member> existingEmailMember = memberRepository.findByEmail(normalizeIdentity(googleUserProfile.getEmail()));
        if (existingEmailMember.isPresent()) {
            Member existingMember = existingEmailMember.get();
            validateActiveMember(existingMember);
            refreshGoogleProfile(existingMember, googleUserProfile);
            existingMember.setNickname(trimToNull(signupForm.getNickname()));
            existingMember.setGender(signupForm.getGender());
            existingMember.setBirthDate(signupForm.getBirthDate());
            existingMember.setHeightCm(signupForm.getHeightCm());
            existingMember.setWeightKg(signupForm.getWeightKg());
            upsertGoogleSocialAccount(existingMember, googleUserProfile);
            return memberRepository.saveAndFlush(existingMember);
        }

        Member member = new Member();
        member.setUsername(generateGoogleUsername(googleUserProfile.getSubject()));
        member.setNickname(trimToNull(signupForm.getNickname()));
        member.setEmail(normalizeIdentity(googleUserProfile.getEmail()));
        member.setGender(signupForm.getGender());
        member.setBirthDate(signupForm.getBirthDate());
        member.setHeightCm(signupForm.getHeightCm());
        member.setWeightKg(signupForm.getWeightKg());
        member.setRole(Member.Role.USER);
        member.setStatus(Member.Status.ACTIVE);
        member.setLoginType(Member.LoginType.GOOGLE);
        member.setSocialProvider("GOOGLE");
        member.setSocialProviderId(googleUserProfile.getSubject());
        member.setProfileImageUrl(trimToNull(googleUserProfile.getPictureUrl()));
        Member savedMember = memberRepository.saveAndFlush(member);
        upsertGoogleSocialAccount(savedMember, googleUserProfile);
        return savedMember;
    }

    public Member linkGoogleAccount(Long memberId, GoogleUserProfile googleUserProfile) {
        if (googleUserProfile == null) {
            throw new IllegalArgumentException("Google 인증 정보가 올바르지 않습니다.");
        }
        if (!googleUserProfile.isEmailVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료된 Google 계정만 연동할 수 있습니다.");
        }

        Member member = findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        Optional<SocialAccount> linkedByProviderUserId = socialAccountRepository
                .findByProviderAndProviderUserId(SocialAccount.Provider.GOOGLE, googleUserProfile.getSubject());
        if (linkedByProviderUserId.isPresent()) {
            SocialAccount socialAccount = linkedByProviderUserId.get();
            if (!socialAccount.getMember().getMemberId().equals(member.getMemberId())) {
                throw new IllegalStateException("이미 다른 계정에 연결된 Google 계정입니다.");
            }
            upsertGoogleSocialAccount(member, googleUserProfile);
            return memberRepository.saveAndFlush(member);
        }

        Optional<SocialAccount> existingGoogleAccount = socialAccountRepository
                .findByMemberMemberIdAndProvider(member.getMemberId(), SocialAccount.Provider.GOOGLE);
        if (existingGoogleAccount.isPresent()) {
            throw new IllegalStateException("이미 Google 계정이 연동되어 있습니다.");
        }

        applyGoogleProfileDefaultsForLinkedMember(member, googleUserProfile);
        upsertGoogleSocialAccount(member, googleUserProfile);
        return memberRepository.saveAndFlush(member);
    }

    private String normalizeUsername(String value) {
        return normalizeIdentity(value);
    }

    private String normalizeIdentity(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateActiveMember(Member member) {
        if (member.getStatus() != Member.Status.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정은 로그인할 수 없습니다.");
        }
    }

    private void refreshGoogleProfile(Member member, GoogleUserProfile googleUserProfile) {
        member.setEmail(normalizeIdentity(googleUserProfile.getEmail()));
        if (member.getLoginType() == null) {
            member.setLoginType(Member.LoginType.GOOGLE);
        }
        if (!StringUtils.hasText(member.getSocialProvider())) {
            member.setSocialProvider("GOOGLE");
        }
        if (!StringUtils.hasText(member.getSocialProviderId())) {
            member.setSocialProviderId(googleUserProfile.getSubject());
        }
        if (!StringUtils.hasText(member.getNickname())) {
            member.setNickname(resolveGoogleNickname(googleUserProfile));
        }
        if (!StringUtils.hasText(member.getProfileImageUrl())) {
            member.setProfileImageUrl(trimToNull(googleUserProfile.getPictureUrl()));
        }
        if (!StringUtils.hasText(member.getUsername())) {
            member.setUsername(generateGoogleUsername(googleUserProfile.getSubject()));
        }
    }

    private void upsertGoogleSocialAccount(Member member, GoogleUserProfile googleUserProfile) {
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(SocialAccount.Provider.GOOGLE, googleUserProfile.getSubject())
                .orElseGet(() -> socialAccountRepository
                        .findByMemberMemberIdAndProvider(member.getMemberId(), SocialAccount.Provider.GOOGLE)
                        .orElseGet(SocialAccount::new));

        socialAccount.setMember(member);
        socialAccount.setProvider(SocialAccount.Provider.GOOGLE);
        socialAccount.setProviderUserId(googleUserProfile.getSubject());
        socialAccount.setProviderEmail(normalizeIdentity(googleUserProfile.getEmail()));
        socialAccount.setProviderName(trimToNull(googleUserProfile.getName()));
        socialAccount.setProfileImageUrl(trimToNull(googleUserProfile.getPictureUrl()));
        socialAccount.setLastLoginAt(LocalDateTime.now());
        socialAccountRepository.saveAndFlush(socialAccount);
    }

    private void applyGoogleProfileDefaultsForLinkedMember(Member member, GoogleUserProfile googleUserProfile) {
        if (!StringUtils.hasText(member.getEmail())) {
            member.setEmail(normalizeIdentity(googleUserProfile.getEmail()));
        }
        if (member.getLoginType() == null) {
            member.setLoginType(Member.LoginType.GOOGLE);
        }
        if (!StringUtils.hasText(member.getSocialProvider())) {
            member.setSocialProvider("GOOGLE");
        }
        if (!StringUtils.hasText(member.getSocialProviderId())) {
            member.setSocialProviderId(googleUserProfile.getSubject());
        }
        if (!StringUtils.hasText(member.getProfileImageUrl())) {
            member.setProfileImageUrl(trimToNull(googleUserProfile.getPictureUrl()));
        }
    }

    private boolean hasGoalChanges(Member member, MyPageForm myPageForm) {
        return hasBigDecimalChanged(member.getGoalWeight(), myPageForm.getGoalWeight())
                || !Objects.equals(member.getGoalEatKcal(), myPageForm.getGoalEatKcal())
                || !Objects.equals(member.getGoalBurnedKcal(), myPageForm.getGoalBurnedKcal());
    }

    private boolean hasBigDecimalChanged(java.math.BigDecimal currentValue, java.math.BigDecimal newValue) {
        if (currentValue == null && newValue == null) {
            return false;
        }
        if (currentValue == null || newValue == null) {
            return true;
        }
        return currentValue.compareTo(newValue) != 0;
    }

    private void savePreviousGoalHistory(Member member) {
        MemberGoalHistory memberGoalHistory = new MemberGoalHistory();
        memberGoalHistory.setMember(member);
        memberGoalHistory.setGoalWeight(member.getGoalWeight());
        memberGoalHistory.setGoalEatKcal(member.getGoalEatKcal());
        memberGoalHistory.setGoalBurnedKcal(member.getGoalBurnedKcal());
        memberGoalHistoryRepository.save(memberGoalHistory);
    }

    private String resolveGoogleNickname(GoogleUserProfile googleUserProfile) {
        String name = trimToNull(googleUserProfile.getName());
        if (name != null) {
            return name.length() > 50 ? name.substring(0, 50) : name;
        }

        String email = normalizeIdentity(googleUserProfile.getEmail());
        if (email == null) {
            return "Google 사용자";
        }

        int atIndex = email.indexOf('@');
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;
        return localPart.length() > 50 ? localPart.substring(0, 50) : localPart;
    }

    private String generateGoogleUsername(String subject) {
        String normalizedSubject = trimToNull(subject);
        if (!StringUtils.hasText(normalizedSubject)) {
            normalizedSubject = Long.toString(Math.abs(secureRandom.nextLong()));
        }

        String candidate = "google_" + normalizedSubject.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
        if (candidate.length() > 50) {
            candidate = candidate.substring(0, 50);
        }
        if (!StringUtils.hasText(candidate) || candidate.length() < 4) {
            candidate = "google" + Math.abs(secureRandom.nextInt(100_000));
        }

        String uniqueCandidate = candidate;
        int suffix = 1;
        while (memberRepository.existsByUsername(uniqueCandidate)) {
            String suffixText = Integer.toString(suffix++);
            int maxBaseLength = Math.max(1, 50 - suffixText.length());
            uniqueCandidate = candidate.substring(0, Math.min(candidate.length(), maxBaseLength)) + suffixText;
        }
        return uniqueCandidate;
    }

    private String hashPassword(String rawPassword) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            byte[] hash = secretKeyFactory.generateSecret(spec).getEncoded();
            return "pbkdf2$" + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("비밀번호 해시 생성에 실패했습니다.", exception);
        } finally {
            spec.clearPassword();
        }
    }

    private boolean verifyPassword(String rawPassword, String storedPasswordHash) {
        if (!StringUtils.hasText(storedPasswordHash) || !storedPasswordHash.startsWith("pbkdf2$")) {
            return false;
        }

        String[] parts = storedPasswordHash.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            return false;
        }

        byte[] salt;
        byte[] expectedHash;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expectedHash = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            byte[] actualHash = secretKeyFactory.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("비밀번호 검증에 실패했습니다.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}


