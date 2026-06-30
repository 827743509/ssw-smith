package com.ssw.smith.model.vo;

import java.math.BigDecimal;

public record DashboardStats(
        long traceCount,
        long successCount,
        long errorCount,
        long totalTokens,
        BigDecimal avgLatencyMs,
        BigDecimal avgFeedbackScore
) {
}
