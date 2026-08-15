package com.itcotato.dortfolio.domain.job.dto;

import java.util.List;

public record JobListResponse(
        List<JobCategoryResponse> categories
) {
}
