package com.itcotato.dortfolio.domain.mypage.controller;

import com.itcotato.dortfolio.domain.mypage.controller.docs.MyPageControllerDocs;
import com.itcotato.dortfolio.domain.mypage.dto.*;
import com.itcotato.dortfolio.domain.mypage.service.MyPageService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MyPageController implements MyPageControllerDocs {

    private final MyPageService myPageService;

    /* 마이페이지 기본 정보 조회 API */
    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPageInfo(
            @AuthenticationPrincipal UUID userId
    ) {
        MyPageResponse response = myPageService.getMyPageInfo(userId);
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회에 성공하였습니다.", response));
    }

    /* 희망 직무 변경 API */
    @Override
    @PatchMapping("/job")
    public ResponseEntity<ApiResponse<Void>> updateDesiredJob(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateDesiredJobRequest request
    ) {
        myPageService.updateDesiredJob(userId, request);
        return ResponseEntity.ok(ApiResponse.success("희망 직무가 성공적으로 변경되었습니다."));
    }

    /* 프로필 이미지 업로드 Presigned URL 발급 API */
    @Override
    @PostMapping("/profile-image/presigned-url")
    public ResponseEntity<ApiResponse<ProfileImagePresignedUrlResponse>> getProfileImagePresignedUrl(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ProfileImagePresignedUrlRequest request
    ) {
        ProfileImagePresignedUrlResponse response = myPageService.createProfileImagePresignedUrl(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Presigned URL 발급에 성공하였습니다.", response));
    }

    /* 회원 프로필 정보(이름, 프로필 사진) 수정 API */
    @Override
    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        myPageService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("회원정보가 수정되었습니다."));
    }
}
