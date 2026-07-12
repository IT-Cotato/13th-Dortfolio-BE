package com.itcotato.dortfolio.domain.template.repository;

import com.itcotato.dortfolio.domain.template.entity.ActivityTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityTemplateRepository extends JpaRepository<ActivityTemplate, UUID> {

	List<ActivityTemplate> findAllByActivityIdOrderBySortOrderAsc(UUID activityId);

	void deleteAllByActivityId(UUID activityId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from ActivityTemplate at where at.activityId = :activityId")
	void deleteAllByActivityIdInBulk(@Param("activityId") UUID activityId);
}
