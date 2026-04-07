package com.ownlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class PendingNaverSignup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String email;
    private String name;
    private String nickname;
    private String profileImageUrl;

    public PendingNaverSignup(String id, String email, String name, String nickname, String profileImageUrl) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public static PendingNaverSignup from(NaverUserProfile naverUserProfile) {
        return new PendingNaverSignup(
                naverUserProfile.getId(),
                naverUserProfile.getEmail(),
                naverUserProfile.getName(),
                naverUserProfile.getNickname(),
                naverUserProfile.getProfileImageUrl()
        );
    }

    public NaverUserProfile toNaverUserProfile() {
        return new NaverUserProfile(id, email, name, nickname, profileImageUrl, StringUtils.hasText(email));
    }
}

