package com.ownlife.service;

import com.ownlife.dto.MyPageForm;
import com.ownlife.dto.SignupForm;
import com.ownlife.entity.Member;
import com.ownlife.repository.MemberRepository;
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
import java.util.Base64;
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
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return StringUtils.hasText(username) && memberRepository.existsByUsername(normalizeUsername(username));
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
                .filter(member -> member.getLoginType() == Member.LoginType.LOCAL)
                .filter(member -> member.getStatus() == Member.Status.ACTIVE)
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

        member.setWeightKg(myPageForm.getWeightKg());
        member.setGoalWeight(myPageForm.getGoalWeight());
        member.setGoalEatKcal(myPageForm.getGoalEatKcal());
        member.setGoalBurnedKcal(myPageForm.getGoalBurnedKcal());

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


