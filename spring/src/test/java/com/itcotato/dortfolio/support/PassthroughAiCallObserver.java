package com.itcotato.dortfolio.support;

import com.itcotato.dortfolio.global.ai.observability.dto.AiUsageResponse;
import com.itcotato.dortfolio.global.ai.observability.service.AiCallObserver;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Keeps existing domain-only tests independent from observability persistence. */
public final class PassthroughAiCallObserver extends AiCallObserver {

    public PassthroughAiCallObserver() {
        super(null, null);
    }

    @Override
    public <T> T attempt(UUID requestId, int attemptNumber, Supplier<T> call,
            Function<T, AiUsageResponse> usageExtractor, Consumer<T> validator) {
        T response = call.get();
        validator.accept(response);
        return response;
    }
}
