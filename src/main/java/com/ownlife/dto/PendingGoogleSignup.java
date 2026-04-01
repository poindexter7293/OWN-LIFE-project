package com.ownlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class PendingGoogleSignup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String subject;
    private String email;
    private String name;
    private String pictureUrl;

    public PendingGoogleSignup(String subject, String email, String name, String pictureUrl) {
        this.subject = subject;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
    }

    public static PendingGoogleSignup from(GoogleUserProfile googleUserProfile) {
        return new PendingGoogleSignup(
                googleUserProfile.getSubject(),
                googleUserProfile.getEmail(),
                googleUserProfile.getName(),
                googleUserProfile.getPictureUrl()
        );
    }

    public GoogleUserProfile toGoogleUserProfile() {
        return new GoogleUserProfile(subject, email, name, pictureUrl, true);
    }
}

