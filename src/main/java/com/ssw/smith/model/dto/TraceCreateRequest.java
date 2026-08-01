package com.ssw.smith.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TraceCreateRequest(
        @NotNull Long projectId,
        String traceKey,
        @NotBlank String name,
        String status,
        Long latencyMs,
        Integer inputTokens,
        Integer totalTokens,
        Integer outputTokens,
        Integer cacheRead,
        JsonNode input,
        JsonNode output,
        JsonNode metadata,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
