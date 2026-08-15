package com.itcotato.dortfolio.domain.job.controller.docs;

import com.itcotato.dortfolio.domain.job.dto.JobListResponse;
import com.itcotato.dortfolio.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Job", description = "직무 API")
public interface JobControllerDocs {

    @Operation(
            summary = "직무 목록 조회",
            description = "희망 직무 선택에 사용하는 대분류별 상세 직무 목록을 조회합니다."
    )
    ResponseEntity<ApiResponse<JobListResponse>> getJobs();
}
