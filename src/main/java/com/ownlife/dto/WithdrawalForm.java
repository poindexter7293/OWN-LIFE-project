package com.ownlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WithdrawalForm {

    private String currentPassword;
    private String confirmationText;
}

