package com.itcotato.dortfolio.domain.insight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class InsightTimeConfig {

    @Bean
    public Clock insightClock() {
        return Clock.systemDefaultZone();
    }
}
