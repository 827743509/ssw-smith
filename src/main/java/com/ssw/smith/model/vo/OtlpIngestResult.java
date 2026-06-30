package com.ssw.smith.model.vo;

public record OtlpIngestResult(
        int traceCount,
        int runCount,
        int rejectedSpanCount
) {
}