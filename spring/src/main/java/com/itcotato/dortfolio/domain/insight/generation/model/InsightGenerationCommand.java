package com.itcotato.dortfolio.domain.insight.generation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/* 생성 명령 스냅샷 */
public record InsightGenerationCommand(
        UUID insightId,
        UUID userId,
        UUID jobId,
        String jobName,
        LocalDateTime snapshotAt,
        List<JobCompetencySnapshot> jobCompetencies
) {

    public InsightGenerationCommand {
        // 호출 측에서 원본 리스트를 수정해도 생성 기준이 바뀌지 않게 방어 복사
        jobCompetencies = List.copyOf(jobCompetencies);
    }

    public record JobCompetencySnapshot(
            UUID jobCompetencyId,
            String competencyName,
            String competencyDescription
    ) {
    }
}