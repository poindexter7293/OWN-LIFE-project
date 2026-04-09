package com.ownlife.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiOneLineCommentDto {

    private String message;
    private String detail;
    private String tone;
    private String badgeLabel;
    private boolean fallback;
}

