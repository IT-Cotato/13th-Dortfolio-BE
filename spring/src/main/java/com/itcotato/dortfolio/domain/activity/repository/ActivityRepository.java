package com.itcotato.dortfolio.domain.activity.repository;

import com.itcotato.dortfolio.domain.activity.entity.Activity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findAllByUser_IdAndDeletedAtIsNull(UUID userId);

    Optional<Activity> findByIdAndUser_Id(UUID id, UUID userId);
}
