package com.ownlife.dto;

import lombok.Getter;

@Getter
public class KakaoUserProfile {

    private final String id;
    private final String email;
    private final String nickname;
    private final String profileImageUrl;
    private final boolean emailVerified;

    public KakaoUserProfile(String id,
                            String email,
                            String nickname,
                            String profileImageUrl,
                            boolean emailVerified) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.emailVerified = emailVerified;
    }
}

