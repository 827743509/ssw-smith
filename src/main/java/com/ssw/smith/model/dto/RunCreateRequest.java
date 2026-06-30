package com.ssw.smith.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RunCreateRequest(
        @NotNull Long traceId,
        Long parentRunId,
        String runKey,
        @NotBlank String name,
        String runType,
        String status,
        String modelName,
        Long latencyMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        JsonNode input,
        JsonNode output,
        JsonNode metadata,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
