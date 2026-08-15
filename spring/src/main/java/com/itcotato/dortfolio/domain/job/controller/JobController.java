package com.itcotato.dortfolio.domain.job.controller;

import com.itcotato.dortfolio.domain.job.controller.docs.JobControllerDocs;
import com.itcotato.dortfolio.domain.job.dto.JobListResponse;
import com.itcotato.dortfolio.domain.job.service.JobQueryService;
import com.itcotato.dortfolio.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController implements JobControllerDocs {

    private final JobQueryService jobQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<JobListResponse>> getJobs() {
        JobListResponse response = jobQueryService.getJobs();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "직무 목록 조회에 성공하였습니다.",
                        response
                )
        );
    }
}
