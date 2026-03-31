package com.ownlife.dto;

import com.ownlife.entity.Member;
import lombok.Getter;

@Getter
public class SessionMember {

    private final Long memberId;
    private final String username;
    private final String nickname;
    private final Member.Role role;

    public SessionMember(Long memberId, String username, String nickname, Member.Role role) {
        this.memberId = memberId;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
    }
}

