package com.itcotato.dortfolio.domain.record.repository;

import com.itcotato.dortfolio.domain.record.entity.StrengthTag;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StrengthTagRepository extends JpaRepository<StrengthTag, UUID> {
}
