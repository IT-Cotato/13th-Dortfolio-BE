package com.itcotato.dortfolio.domain.insight.generation.service;

import com.itcotato.dortfolio.domain.insight.dto.res.InsightEligibilityResponse;
import com.itcotato.dortfolio.domain.insight.entity.Insight;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand;
import com.itcotato.dortfolio.domain.insight.generation.model.InsightGenerationCommand.JobCompetencySnapshot;
import com.itcotato.dortfolio.domain.insight.repository.InsightRepository;
import com.itcotato.dortfolio.domain.insight.service.InsightEligibilityChecker;
import com.itcotato.dortfolio.domain.job.entity.JobCompetency;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyEmbeddingRepository;
import com.itcotato.dortfolio.domain.job.repository.JobCompetencyRepository;
import com.itcotato.dortfolio.domain.user.entity.UserJob;
import com.itcotato.dortfolio.domain.user.repository.UserJobRepository;
import com.itcotato.dortfolio.domain.user.repository.UserRepository;
import com.itcotato.dortfolio.global.exception.CustomException;
import com.itcotato.dortfolio.global.exception.types.InsightErrorCode;
import com.itcotato.dortfolio.global.exception.types.JobErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/* RequestWriter 책임 */
@Component
@RequiredArgsConstructor
public class InsightGenerationRequestWriter {

    private static final int REQUIRED_COMPETENCY_COUNT = 5;

    private final InsightEligibilityChecker eligibilityChecker;
    private final UserRepository userRepository;
    private final UserJobRepository userJobRepository;
    private final JobCompetencyRepository jobCompetencyRepository;
    private final JobCompetencyEmbeddingRepository embeddingRepository;
    private final InsightRepository insightRepository;
    private final com.itcotato.dortfolio.domain.insight.config.InsightProperties
            insightProperties;

    @Transactional
    public InsightGenerationCommand createPending(
            UUID userId,
            LocalDateTime snapshotAt
    ) {
        // 락을 획득한 뒤 실제 정책과 DB 상태를 다시 확인
        InsightEligibilityResponse eligibility =
                eligibilityChecker.check(userId);

        if (!eligibility.eligible()) {
            throw new CustomException(
                    InsightErrorCode
                            .INSIGHT_GENERATION_NOT_ALLOWED
            );
        }

        UserJob primaryUserJob = userJobRepository
                .findByUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() -> new CustomException(
                        InsightErrorCode
                                .INSIGHT_GENERATION_NOT_ALLOWED
                ));

        UUID jobId = primaryUserJob.getJob().getId();
        String jobName = primaryUserJob.getJob().getName();

        List<JobCompetency> jobCompetencies =
                jobCompetencyRepository
                        .findAllByJob_IdOrderBySortOrderAsc(
                                jobId
                        );

        if (jobCompetencies.size()
                != REQUIRED_COMPETENCY_COUNT) {
            throw new CustomException(
                    InsightErrorCode
                            .INSIGHT_JOB_COMPETENCIES_NOT_READY
            );
        }

        // 직무 역량 임베딩이 모두 준비된 경우에만 생성 시장
        validateEmbeddings(jobCompetencies);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(
                        InsightErrorCode
                                .INSIGHT_GENERATION_NOT_ALLOWED
                ));

        int baseRecordCount = Math.toIntExact(
                eligibility.analyzedRecordCount()
        );

        // PENDING 저장은 비동기 실행을 요청하기 전에 커밋되어야 함
        Insight insight;
        // DB 유니크 제약 조건 위반 시 예외 처리
        try {
            insight = insightRepository.saveAndFlush(
                    Insight.pending(
                            user,
                            jobId,
                            jobName,
                            snapshotAt,
                            baseRecordCount,
                            snapshotAt
                    )
            );
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(
                    InsightErrorCode.INSIGHT_GENERATION_IN_PROGRESS
            );
        }

        List<JobCompetencySnapshot> competencySnapshots =
                jobCompetencies.stream()
                        .map(this::toSnapshot)
                        .toList();

        return new InsightGenerationCommand(
                insight.getId(),
                userId,
                jobId,
                jobName,
                snapshotAt,
                competencySnapshots
        );
    }

    private void validateEmbeddings(
            List<JobCompetency> jobCompetencies
    ) {
        String embeddingModel =
                insightProperties.embeddingModel();

        boolean missing = jobCompetencies.stream()
                .anyMatch(jobCompetency ->
                        !embeddingRepository
                                .existsByJobCompetency_IdAndEmbeddingModel(
                                        jobCompetency.getId(),
                                        embeddingModel
                                )
                );

        if (missing) {
            throw new CustomException(
                    JobErrorCode
                            .JOB_COMPETENCY_EMBEDDING_NOT_READY
            );
        }
    }

    private JobCompetencySnapshot toSnapshot(
            JobCompetency jobCompetency
    ) {
        var competencyTag =
                jobCompetency.getCompetencyTag();

        return new JobCompetencySnapshot(
                jobCompetency.getId(),
                competencyTag.getName(),
                competencyTag.getDescription()
        );
    }
}