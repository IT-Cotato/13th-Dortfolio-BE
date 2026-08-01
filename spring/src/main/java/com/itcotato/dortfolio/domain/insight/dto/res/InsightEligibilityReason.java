package com.itcotato.dortfolio.domain.insight.dto.res;

public enum InsightEligibilityReason {
    AVAILABLE,
    JOB_NOT_CONFIGURED,
    NOT_ENOUGH_COMPLETED_RECORDS,
    NOT_ENOUGH_ANALYZED_RECORDS,
    ANALYSIS_IN_PROGRESS,
    GENERATION_IN_PROGRESS,
    COOLDOWN,
    NO_CHANGES
}
