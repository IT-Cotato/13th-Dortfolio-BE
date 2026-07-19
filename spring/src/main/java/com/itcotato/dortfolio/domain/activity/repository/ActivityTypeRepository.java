package com.itcotato.dortfolio.domain.activity.repository;

import com.itcotato.dortfolio.domain.activity.entity.ActivityType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityTypeRepository extends JpaRepository<ActivityType, UUID> {

    List<ActivityType> findAllByUser_Id(UUID userId);

    Optional<ActivityType> findByIdAndUser_Id(UUID id, UUID userId);
}
