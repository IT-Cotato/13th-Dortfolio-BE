package com.itcotato.dortfolio.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.repository.JobRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobQueryServiceTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobQueryService jobQueryService;

    @Test
    void returnsEveryCategoryInDefinedOrderAndGroupsJobs() {
        when(jobRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(
                Job.create("JOB_001", "PLANNING_MANAGEMENT", "경영·사업기획", null),
                Job.create("JOB_012", "IT_DEVELOPMENT", "프론트엔드 개발", null),
                Job.create("JOB_013", "IT_DEVELOPMENT", "백엔드/서버 개발", null)
        ));

        var response = jobQueryService.getJobs();

        assertThat(response.categories())
                .extracting(category -> category.code())
                .containsExactly(
                        "PLANNING_MANAGEMENT",
                        "MARKETING_ADVERTISING",
                        "IT_DEVELOPMENT",
                        "DESIGN",
                        "SALES_CS",
                        "PRODUCTION_MANUFACTURING",
                        "RESEARCH_RND",
                        "FINANCE",
                        "MEDIA_CONTENT",
                        "LOGISTICS_DISTRIBUTION"
                );
        assertThat(response.categories().get(0).name()).isEqualTo("기획/경영");
        assertThat(response.categories().get(2).jobs())
                .extracting(job -> job.code())
                .containsExactly("JOB_012", "JOB_013");
        assertThat(response.categories().get(1).jobs()).isEmpty();
    }

    @Test
    void ignoresJobsWithUnsupportedCategoryCode() {
        when(jobRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(
                Job.create("JOB_001", "PLANNING_MANAGEMENT", "경영·사업기획", null),
                Job.create("JOB_999", "UNSUPPORTED", "미지원 직무", null)
        ));

        var response = jobQueryService.getJobs();

        assertThat(response.categories())
                .flatExtracting(category -> category.jobs())
                .extracting(job -> job.code())
                .containsExactly("JOB_001");
    }
}
