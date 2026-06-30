package com.ssw.smith.model.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record RunEndRequest(
        Long id,
        String runKey,
        String status,
        Long latencyMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        JsonNode output,
        JsonNode metadata,
        String errorMessage,
        LocalDateTime endedAt
) {
}