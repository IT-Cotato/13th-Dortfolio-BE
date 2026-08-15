package com.itcotato.dortfolio.domain.job.dto;

import com.itcotato.dortfolio.domain.job.entity.Job;

import java.util.UUID;

public record JobSummaryResponse(
        UUID id,
        String code,
        String name
) {
    public static JobSummaryResponse from(Job job) {
        return new JobSummaryResponse(
                job.getId(),
                job.getCode(),
                job.getName()
        );
    }
}
