package com.ownlife.dto;

import lombok.Getter;

@Getter
public class NaverUserProfile {

    private final String id;
    private final String email;
    private final String name;
    private final String nickname;
    private final String profileImageUrl;
    private final boolean emailVerified;

    public NaverUserProfile(String id,
                            String email,
                            String name,
                            String nickname,
                            String profileImageUrl,
                            boolean emailVerified) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.emailVerified = emailVerified;
    }
}

