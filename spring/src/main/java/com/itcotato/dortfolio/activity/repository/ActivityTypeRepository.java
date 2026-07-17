package com.itcotato.dortfolio.activity.repository;

import com.itcotato.dortfolio.activity.entity.ActivityType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityTypeRepository extends JpaRepository<ActivityType, UUID> {

    List<ActivityType> findAllByUserId(UUID userId);

    Optional<ActivityType> findByIdAndUserId(UUID id, UUID userId);
}
