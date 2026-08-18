package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.StrengthTagEmbedding;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrengthTagEmbeddingRepository extends JpaRepository<StrengthTagEmbedding, UUID> {

	long countByEmbeddingModel(String embeddingModel);

	boolean existsByStrengthTag_IdAndEmbeddingModel(
		UUID strengthTagId,
		String embeddingModel
	);
}
