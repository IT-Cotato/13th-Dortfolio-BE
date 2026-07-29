package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.CompetencyTag;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetencyTagRepository extends JpaRepository<CompetencyTag, UUID> {
}
