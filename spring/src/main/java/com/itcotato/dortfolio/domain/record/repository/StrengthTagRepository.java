package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.StrengthTag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StrengthTagRepository extends JpaRepository<StrengthTag, UUID> {

	@Query("""
		select strengthTag.id
		from StrengthTag strengthTag
		where not exists (
			select 1
			from StrengthTagEmbedding embedding
			where embedding.strengthTag = strengthTag
			  and embedding.embeddingModel = :embeddingModel
		)
		order by strengthTag.code asc
		""")
	List<UUID> findMissingEmbeddingIdsByModel(
		@Param("embeddingModel") String embeddingModel
	);
}
