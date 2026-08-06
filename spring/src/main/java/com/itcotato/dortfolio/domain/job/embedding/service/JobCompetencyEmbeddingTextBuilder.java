package com.itcotato.dortfolio.domain.job.embedding.service;

import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import org.springframework.stereotype.Component;

@Component
public class JobCompetencyEmbeddingTextBuilder {

    public String build(JobCompetency jobCompetency) {
        String description =
                jobCompetency.getCompetencyTag().getDescription();

        // null 설명은 빈 문자열로 통일해 임베딩 원문을 안정적으로 생성
        String normalizedDescription =
                description == null ? "" : description.trim();

        return """
                직무: %s
                역량: %s
                설명: %s
                """.formatted(
                jobCompetency.getJob().getName().trim(),
                jobCompetency.getCompetencyTag()
                        .getName()
                        .trim(),
                normalizedDescription
        ).strip();
    }
}