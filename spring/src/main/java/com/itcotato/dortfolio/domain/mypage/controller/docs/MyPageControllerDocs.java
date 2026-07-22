package com.itcotato.dortfolio.domain.mypage.controller.docs;

import com.itcotato.dortfolio.domain.mypage.dto.MyPageResponse;
import com.itcotato.dortfolio.domain.mypage.dto.UpdateDesiredJobRequest;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "MyPage", description = "마이페이지 API")
public interface MyPageControllerDocs {

    @Operation(summary = "마이페이지 기본 정보 조회", description = "로그인된 회원의 이름, 이메일, 프로필 이미지 URL, 대표 희망 직무를 조회합니다.")
    ResponseEntity<ApiResponse<MyPageResponse>> getMyPageInfo(
            @Parameter(hidden = true) UUID userId
    );

    @Operation(summary = "희망 직무 변경", description = "회원의 대표 희망 직무를 변경하거나 새로 설정합니다.")
    ResponseEntity<ApiResponse<Void>> updateDesiredJob(
            @Parameter(hidden = true) UUID userId,
            UpdateDesiredJobRequest request
    );
}