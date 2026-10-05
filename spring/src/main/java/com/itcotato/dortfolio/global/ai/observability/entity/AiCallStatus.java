package com.itcotato.dortfolio.global.ai.observability.entity;

public enum AiCallStatus {
    SUCCESS,
    TIMEOUT,
    RATE_LIMITED,
    PROVIDER_ERROR,
    INVALID_RESPONSE,
    UNKNOWN_ERROR
}