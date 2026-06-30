package com.ssw.smith.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssw.smith.model.vo.OtlpIngestResult;

public interface OtlpService {

    OtlpIngestResult ingestTraces(JsonNode body);

    OtlpIngestResult ingestTraces(byte[] body);
}