package com.itcotato.dortfolio.domain.job.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.job.dto.JobCategoryResponse;
import com.itcotato.dortfolio.domain.job.dto.JobListResponse;
import com.itcotato.dortfolio.domain.job.dto.JobSummaryResponse;
import com.itcotato.dortfolio.domain.job.service.JobQueryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private JobQueryService jobQueryService;

    @InjectMocks
    private JobController jobController;

    @Test
    void returnsGroupedJobCatalog() {
        UUID jobId = UUID.randomUUID();
        JobListResponse expected = new JobListResponse(List.of(
                new JobCategoryResponse(
                        "IT_DEVELOPMENT",
                        "IT/개발",
                        List.of(new JobSummaryResponse(
                                jobId,
                                "JOB_013",
                                "백엔드/서버 개발"
                        ))
                )
        ));
        when(jobQueryService.getJobs()).thenReturn(expected);

        var response = jobController.getJobs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage())
                .isEqualTo("직무 목록 조회에 성공하였습니다.");
        assertThat(response.getBody().getData()).isEqualTo(expected);
        verify(jobQueryService).getJobs();
    }
}
