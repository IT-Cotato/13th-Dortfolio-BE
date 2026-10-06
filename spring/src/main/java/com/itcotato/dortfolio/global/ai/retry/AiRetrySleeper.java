package com.itcotato.dortfolio.global.ai.retry;

import java.io.IOException;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
public class AiRetrySleeper {
    public void sleep(Duration duration) {
        try {
            if (!duration.isZero()) {
                Thread.sleep(duration.toMillis());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResourceAccessException("AI retry interrupted", new IOException(exception));
        }
    }
}
