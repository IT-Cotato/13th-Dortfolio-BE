package com.itcotato.dortfolio.domain.record.analysis.service;

import com.itcotato.dortfolio.domain.record.analysis.exception.RecordAnalysisErrorCode;
import com.itcotato.dortfolio.domain.record.analysis.repository.RecordAnalysisRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(
	prefix = "record-analysis",
	name = "retry-not-ready-batch-enabled",
	havingValue = "true"
)
public class RecordAnalysisNotReadyRetryBatchRunner implements ApplicationRunner {

	private static final String NOT_READY_CODE =
		RecordAnalysisErrorCode.STRENGTH_TAG_EMBEDDING_NOT_READY.getCode();

	private final RecordAnalysisRepository recordAnalysisRepository;
	private final RecordAnalysisService recordAnalysisService;

	@Override
	public void run(ApplicationArguments args) {
		List<UUID> recordIds = findTargets();
		log.info("Record analysis not-ready retry batch started. targetCount={}", recordIds.size());

		recordIds.forEach(recordAnalysisService::analyze);

		List<UUID> remainingIds = findTargets();
		log.info("Record analysis not-ready retry batch completed. remainingCount={}", remainingIds.size());
		if (!remainingIds.isEmpty()) {
			throw new IllegalStateException(
				"강점 임베딩 준비 전 실패 기록의 재분석이 완료되지 않았습니다. remaining="
					+ remainingIds.size()
			);
		}
	}

	private List<UUID> findTargets() {
		return recordAnalysisRepository.findRetryableRecordIdsByFailureCode(NOT_READY_CODE);
	}
}
