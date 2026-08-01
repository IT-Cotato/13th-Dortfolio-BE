package com.itcotato.dortfolio.domain.story.controller.docs;

import com.itcotato.dortfolio.domain.story.dto.res.TimelineActivityResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "나의 스토리 (Story)", description = "활동 보관함 타임라인 조회 API입니다.")
public interface StoryControllerDocs {

    @Operation(summary = "타임라인 조회",
            description = "보관된 활동을 활동 시작일 오름차순으로 조회하고, 각 활동에 속한 기록을 작성순(오래된 순)으로 함께 반환합니다. "
                    + "기록이 없는 활동도 빈 배열과 함께 반환됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<TimelineActivityResponse>>> getTimeline(UUID userId);
}
