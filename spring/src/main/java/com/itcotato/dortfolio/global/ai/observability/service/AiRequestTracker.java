package com.itcotato.dortfolio.global.ai.observability.service;

import com.itcotato.dortfolio.domain.user.entity.User;
import com.itcotato.dortfolio.global.ai.observability.entity.AiFeature;
import com.itcotato.dortfolio.global.ai.observability.entity.AiRequest;
import com.itcotato.dortfolio.global.ai.observability.repository.AiRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiRequestTracker {

    private final AiRequestRepository aiRequestRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID start(User user, AiFeature feature) {
        UUID requestId = UUID.randomUUID();

        AiRequest aiRequest = AiRequest.pending(
                requestId,
                user,
                feature
        );

        aiRequestRepository.save(aiRequest);
        return requestId;
    }
}
