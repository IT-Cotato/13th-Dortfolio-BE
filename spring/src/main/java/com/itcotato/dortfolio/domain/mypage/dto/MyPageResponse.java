package com.itcotato.dortfolio.domain.mypage.dto;

import com.itcotato.dortfolio.domain.user.entity.User;
import java.util.UUID;

public record MyPageResponse(
        String name,
        String email,
        String profileImageUrl,
        String profileImageKey,
        UUID desiredJobId,
        String desiredJob
) {
    public static MyPageResponse of(
            User user,
            String profileImageUrl,
            UUID desiredJobId,
            String desiredJob
    ) {
        return new MyPageResponse(
                user.getNickname(),
                user.getEmail(),
                profileImageUrl,
                user.getProfileImageUrl(),
                desiredJobId,
                desiredJob
        );
    }
}
