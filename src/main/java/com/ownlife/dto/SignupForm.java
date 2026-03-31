package com.ownlife.dto;

import com.ownlife.entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class SignupForm {

    private String username;
    private String password;
    private String passwordConfirm;
    private String nickname;
    private String email;
    private Member.Gender gender;
    private LocalDate birthDate;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
}

