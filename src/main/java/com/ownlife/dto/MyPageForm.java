package com.ownlife.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class MyPageForm {

    private BigDecimal weightKg;
    private BigDecimal goalWeight;
    private Integer goalEatKcal;
    private Integer goalBurnedKcal;
}

