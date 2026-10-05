package com.itcotato.dortfolio.global.ai.observability.repository;

import com.itcotato.dortfolio.global.ai.observability.entity.AiCallAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiCallAttemptRepository
        extends JpaRepository<AiCallAttempt, UUID> {

    List<AiCallAttempt> findAllByAiRequest_IdOrderByAttemptNumberAsc(
            UUID aiRequestId
    );
}
