package com.ssw.smith.model.vo;

import java.math.BigDecimal;

public record DashboardStats(
        long traceCount,
        long successCount,
        long errorCount,
        long inputTokens,
        long totalTokens,
        long outputTokens,
        long cacheRead,
        BigDecimal avgLatencyMs,
        BigDecimal avgFeedbackScore
) {
}
