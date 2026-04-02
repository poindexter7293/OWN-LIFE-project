package com.ownlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class PendingKakaoSignup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String email;
    private String nickname;
    private String profileImageUrl;

    public PendingKakaoSignup(String id, String email, String nickname, String profileImageUrl) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public static PendingKakaoSignup from(KakaoUserProfile kakaoUserProfile) {
        return new PendingKakaoSignup(
                kakaoUserProfile.getId(),
                kakaoUserProfile.getEmail(),
                kakaoUserProfile.getNickname(),
                kakaoUserProfile.getProfileImageUrl()
        );
    }

    public KakaoUserProfile toKakaoUserProfile() {
        return new KakaoUserProfile(id, email, nickname, profileImageUrl, true);
    }
}

