package com.itcotato.dortfolio.domain.template.repository;

import com.itcotato.dortfolio.domain.template.entity.ActivityTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityTemplateRepository extends JpaRepository<ActivityTemplate, UUID> {

	List<ActivityTemplate> findAllByActivity_IdOrderBySortOrderAsc(UUID activityId);

	@EntityGraph(attributePaths = {"activity", "template"})
	List<ActivityTemplate> findAllByTemplate_BuiltinCodeIn(List<String> builtinCodes);

	boolean existsByActivity_IdAndTemplate_Id(UUID activityId, UUID templateId);
}
