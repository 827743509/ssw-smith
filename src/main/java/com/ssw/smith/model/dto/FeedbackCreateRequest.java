package com.ssw.smith.model.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FeedbackCreateRequest(
        @NotNull Long traceId,
        Long runId,
        String feedbackKey,
        @NotNull BigDecimal score,
        String comment,
        String source
) {
}
