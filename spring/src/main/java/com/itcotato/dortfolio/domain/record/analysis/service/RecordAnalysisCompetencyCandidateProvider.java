package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyRepository;
import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecordAnalysisCompetencyCandidateProvider {

    private static final int REQUIRED_COMPETENCY_COUNT = 5;

    private final UserJobRepository userJobRepository;
    private final JobCompetencyRepository jobCompetencyRepository;

    @Transactional(readOnly = true)
    public List<CompetencyTag> getRequiredCandidates(UUID userId) {
        UserJob primaryUserJob = userJobRepository
                .findByUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() -> new CustomException(
                        RecordAnalysisErrorCode.RECORD_ANALYSIS_PRIMARY_JOB_REQUIRED
                ));

        List<JobCompetency> jobCompetencies =
                jobCompetencyRepository.findAllByJob_IdOrderBySortOrderAsc(
                        primaryUserJob.getJob().getId()
                );

        if (jobCompetencies.size() != REQUIRED_COMPETENCY_COUNT) {
            throw new CustomException(
                    RecordAnalysisErrorCode.RECORD_ANALYSIS_JOB_COMPETENCIES_INVALID
            );
        }

        return jobCompetencies.stream()
                .map(JobCompetency::getCompetencyTag)
                .toList();
    }

    public void validateCandidates(UUID userId) {
        getRequiredCandidates(userId);
    }
}
