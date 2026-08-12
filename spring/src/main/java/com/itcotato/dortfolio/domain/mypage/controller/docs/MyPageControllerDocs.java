package com.itcotato.dortfolio.domain.mypage.controller.docs;

import com.itcotato.dortfolio.domain.mypage.dto.*;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "MyPage", description = "마이페이지 API")
public interface MyPageControllerDocs {

    @Operation(summary = "마이페이지 기본 정보 조회", description = "로그인된 회원의 이름, 이메일, 프로필 이미지 조회 URL과 S3 Key, 대표 희망 직무 ID와 이름을 조회합니다.")
    ResponseEntity<ApiResponse<MyPageResponse>> getMyPageInfo(
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "희망 직무 변경", description = "회원의 대표 희망 직무를 변경하거나 새로 설정합니다.")
    ResponseEntity<ApiResponse<Void>> updateDesiredJob(
            @Parameter(hidden = true) UUID userId,
            UpdateDesiredJobRequest request
    );

    @Operation(summary = "프로필 이미지 업로드용 Presigned URL 발급", description = "S3에 프로필 이미지를 직접 업로드하기 위한 사용자 전용 Presigned URL과 S3 Key를 발급받습니다. 지원 형식은 jpg, jpeg, png, webp입니다.")
    ResponseEntity<ApiResponse<ProfileImagePresignedUrlResponse>> getProfileImagePresignedUrl(
            @Parameter(hidden = true) UUID userId,
            ProfileImagePresignedUrlRequest request
    );

    @Operation(summary = "회원 프로필 정보(이름, 프로필 사진) 수정", description = "로그인한 회원의 이름(닉네임) 또는 프로필 이미지 S3 Key를 수정합니다. 이미지 삭제 시 빈 문자열을 전달합니다.")
    ResponseEntity<ApiResponse<Void>> updateProfile(
            @Parameter(hidden = true) UUID userId,
            UpdateUserProfileRequest request
    );
}
