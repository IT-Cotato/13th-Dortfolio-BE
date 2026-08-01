package com.itcotato.dortfolio.domain.story.controller;

import com.itcotato.dortfolio.domain.story.controller.docs.StoryControllerDocs;
import com.itcotato.dortfolio.domain.story.dto.res.TimelineActivityResponse;
import com.itcotato.dortfolio.domain.story.service.StoryService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/story")
@RequiredArgsConstructor
public class StoryController implements StoryControllerDocs {

    private final StoryService storyService;

    /* 타임라인 조회 API (기능명세서 5.1) */
    @Override
    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<TimelineActivityResponse>>> getTimeline(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("타임라인을 조회했습니다.", storyService.getTimeline(userId)));
    }
}
