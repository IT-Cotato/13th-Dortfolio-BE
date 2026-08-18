package com.itcotato.dortfolio.domain.record.analysis.embedding;

import com.itcotato.dortfolio.domain.record.entity.StrengthTag;
import org.springframework.stereotype.Component;

@Component
public class StrengthTagEmbeddingTextBuilder {

	public String build(StrengthTag strengthTag) {
		return """
			강점: %s
			정의: %s
			판단 기준: %s
			적합 예시: %s
			부적합 예시: %s
			""".formatted(
			strengthTag.getName().trim(),
			strengthTag.getDescription().trim(),
			strengthTag.getEvaluationCriteria().trim(),
			strengthTag.getPositiveExample().trim(),
			strengthTag.getNegativeExample().trim()
		).strip();
	}
}
