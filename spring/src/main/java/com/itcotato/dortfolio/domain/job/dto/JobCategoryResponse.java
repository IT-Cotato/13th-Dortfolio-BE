package com.itcotato.dortfolio.domain.job.dto;

import java.util.List;

public record JobCategoryResponse(
        String code,
        String name,
        List<JobSummaryResponse> jobs
) {
}
