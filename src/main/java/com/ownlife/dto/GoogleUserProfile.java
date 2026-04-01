package com.ownlife.dto;

import lombok.Getter;

@Getter
public class GoogleUserProfile {

    private final String subject;
    private final String email;
    private final String name;
    private final String pictureUrl;
    private final boolean emailVerified;

    public GoogleUserProfile(String subject,
                             String email,
                             String name,
                             String pictureUrl,
                             boolean emailVerified) {
        this.subject = subject;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.emailVerified = emailVerified;
    }
}

