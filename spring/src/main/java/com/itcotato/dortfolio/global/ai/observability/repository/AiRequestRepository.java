package com.itcotato.dortfolio.global.ai.observability.repository;

import com.itcotato.dortfolio.global.ai.observability.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AiRequestRepository
        extends JpaRepository<AiRequest, UUID> {

    Optional<AiRequest> findByRequestId(UUID requestId);
}
