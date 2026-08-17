package com.itcotato.dortfolio.domain.record.analysis.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
	prefix = "strength-tag-embedding.batch",
	name = "enabled",
	havingValue = "true"
)
public class StrengthTagEmbeddingBatchRunner implements ApplicationRunner {

	private final StrengthTagEmbeddingService embeddingService;
	private final StrengthTagEmbeddingBatchProperties properties;

	@Override
	public void run(ApplicationArguments args) {
		if (properties.command() == StrengthTagEmbeddingBatchCommand.VERIFY) {
			verifyComplete();
			return;
		}

		StrengthTagEmbeddingBatchResult result = embeddingService.generateAllMissing();
		StrengthTagEmbeddingCoverage coverage = embeddingService.inspectCoverage();
		log.info(
			"Strength tag embedding command completed. total={}, generated={}, skipped={}, failed={}, embedded={}, missing={}",
			result.totalCount(),
			result.generatedCount(),
			result.skippedCount(),
			result.failedCount(),
			coverage.embeddedCount(),
			coverage.missingCount()
		);

		if (result.hasFailures() || !coverage.isComplete()) {
			throw new IllegalStateException(
				"강점 태그 임베딩 생성이 완료되지 않았습니다. failed="
					+ result.failedCount()
					+ ", missing="
					+ coverage.missingCount()
			);
		}
	}

	private void verifyComplete() {
		StrengthTagEmbeddingCoverage coverage = embeddingService.inspectCoverage();
		log.info(
			"Strength tag embedding coverage. model={}, total={}, embedded={}, missing={}, missingIds={}",
			coverage.embeddingModel(),
			coverage.totalCount(),
			coverage.embeddedCount(),
			coverage.missingCount(),
			coverage.missingStrengthTagIds()
		);
		if (!coverage.isComplete()) {
			throw new IllegalStateException("누락된 강점 태그 임베딩이 있습니다. missing=" + coverage.missingCount());
		}
	}
}
