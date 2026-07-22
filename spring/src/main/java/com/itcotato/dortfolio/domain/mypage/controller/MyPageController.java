package com.itcotato.dortfolio.domain.mypage.controller;

import com.itcotato.dortfolio.domain.mypage.controller.docs.MyPageControllerDocs;
import com.itcotato.dortfolio.domain.mypage.dto.MyPageResponse;
import com.itcotato.dortfolio.domain.mypage.dto.UpdateDesiredJobRequest;
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
}