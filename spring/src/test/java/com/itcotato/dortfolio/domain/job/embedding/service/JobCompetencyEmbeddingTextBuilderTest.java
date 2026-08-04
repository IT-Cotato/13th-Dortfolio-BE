package com.itcotato.dortfolio.domain.job.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import org.junit.jupiter.api.Test;

class JobCompetencyEmbeddingTextBuilderTest {

    private final JobCompetencyEmbeddingTextBuilder builder =
            new JobCompetencyEmbeddingTextBuilder();

    @Test
    void buildsStableEmbeddingSourceText() {
        JobCompetency competency = JobCompetency.create(
                Job.create("백엔드 개발자", null),
                CompetencyTag.create("문제 해결", "복잡한 문제의 원인을 파악합니다."),
                1
        );

        assertThat(builder.build(competency)).isEqualTo("""
                직무: 백엔드 개발자
                역량: 문제 해결
                설명: 복잡한 문제의 원인을 파악합니다.""");
    }

    @Test
    void replacesNullDescriptionWithEmptyText() {
        JobCompetency competency = JobCompetency.create(
                Job.create("백엔드 개발자", null),
                CompetencyTag.create("협업", null),
                1
        );

        assertThat(builder.build(competency)).isEqualTo("""
                직무: 백엔드 개발자
                역량: 협업
                설명:""");
    }
}
