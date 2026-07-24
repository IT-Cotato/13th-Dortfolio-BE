package com.itcotato.dortfolio.domain.mypage.dto;

import com.itcotato.dortfolio.domain.user.entity.User;

public record MyPageResponse(
        String name,
        String email,
        String profileImageUrl,
        String desiredJob
) {
    public static MyPageResponse of(User user, String desiredJob) {
        return new MyPageResponse(
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl(),
                desiredJob
        );
    }
}