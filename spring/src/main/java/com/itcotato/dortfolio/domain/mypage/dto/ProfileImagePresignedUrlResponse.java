package com.itcotato.dortfolio.domain.mypage.dto;

public record ProfileImagePresignedUrlResponse(
        String presignedUrl,
        String s3Key
) {}