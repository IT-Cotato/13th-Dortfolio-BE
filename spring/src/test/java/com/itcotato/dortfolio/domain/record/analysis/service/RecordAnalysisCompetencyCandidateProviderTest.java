package com.itcotato.dortfolio.domain.record.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.itcotato.dortfolio.domain.job.entity.Job;
import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyRepository;
import com.itcotato.dortfolio.domain.job.repository.JobRepository;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.record.repository.CompetencyTagRepository;
import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RecordAnalysisCompetencyCandidateProviderTest {

    @Autowired
    private RecordAnalysisCompetencyCandidateProvider candidateProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJobRepository userJobRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobCompetencyRepository jobCompetencyRepository;

    @Autowired
    private CompetencyTagRepository competencyTagRepository;

    @Test
    void returnsOnlyPrimaryJobCompetenciesInSortOrder() {
        User user = createUser();
        Job primaryJob = createJob("PRIMARY", "주 희망 직무");
        Job otherJob = createJob("OTHER", "다른 직무");
        userJobRepository.save(UserJob.create(user, primaryJob, true));
        userJobRepository.save(UserJob.create(user, otherJob, false));

        List<CompetencyTag> primaryTags = createTags("PRIMARY", 5);
        List<CompetencyTag> otherTags = createTags("OTHER", 5);
        saveJobCompetencies(primaryJob, primaryTags, List.of(3, 1, 5, 2, 4));
        saveJobCompetencies(otherJob, otherTags, List.of(1, 2, 3, 4, 5));

        List<CompetencyTag> result = candidateProvider.getRequiredCandidates(user.getId());

        assertThat(result)
            .extracting(CompetencyTag::getId)
            .containsExactly(
                primaryTags.get(1).getId(),
                primaryTags.get(3).getId(),
                primaryTags.get(0).getId(),
                primaryTags.get(4).getId(),
                primaryTags.get(2).getId()
            )
            .doesNotContainAnyElementsOf(otherTags.stream().map(CompetencyTag::getId).toList());
    }

    @Test
    void rejectsUserWithoutPrimaryJob() {
        User user = createUser();

        assertThatThrownBy(() -> candidateProvider.getRequiredCandidates(user.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(RecordAnalysisErrorCode.RECORD_ANALYSIS_PRIMARY_JOB_REQUIRED);
    }

    @Test
    void rejectsJobWithFourCompetencies() {
        assertInvalidCompetencyCount(4);
    }

    @Test
    void rejectsJobWithSixCompetencies() {
        assertInvalidCompetencyCount(6);
    }

    private void assertInvalidCompetencyCount(int count) {
        User user = createUser();
        Job job = createJob("COUNT_" + count, "역량 개수 테스트 직무");
        userJobRepository.save(UserJob.create(user, job, true));
        List<CompetencyTag> tags = createTags("COUNT_" + count, count);
        saveJobCompetencies(job, tags, java.util.stream.IntStream.rangeClosed(1, count).boxed().toList());

        assertThatThrownBy(() -> candidateProvider.getRequiredCandidates(user.getId()))
            .isInstanceOf(CustomException.class)
            .extracting("errorCode")
            .isEqualTo(RecordAnalysisErrorCode.RECORD_ANALYSIS_JOB_COMPETENCIES_INVALID);
    }

    private User createUser() {
        return userRepository.save(User.of(
            UUID.randomUUID() + "@test.com",
            "encoded-password",
            "테스터"
        ));
    }

    private Job createJob(String suffix, String name) {
        return jobRepository.save(Job.create(
            "T_" + UUID.randomUUID().toString().substring(0, 12),
            "IT_DEVELOPMENT",
            name,
            "테스트 직무 설명"
        ));
    }

    private List<CompetencyTag> createTags(String prefix, int count) {
        List<CompetencyTag> tags = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            tags.add(competencyTagRepository.save(CompetencyTag.create(
                "C" + index + "_" + UUID.randomUUID().toString().substring(0, 12),
                prefix + " 역량 " + index,
                prefix + " 역량 설명 " + index
            )));
        }
        return tags;
    }

    private void saveJobCompetencies(Job job, List<CompetencyTag> tags, List<Integer> sortOrders) {
        List<JobCompetency> jobCompetencies = new ArrayList<>();
        for (int index = 0; index < tags.size(); index++) {
            jobCompetencies.add(JobCompetency.create(job, tags.get(index), sortOrders.get(index)));
        }
        jobCompetencyRepository.saveAll(jobCompetencies);
    }
}
