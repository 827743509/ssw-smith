package com.ssw.smith.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssw.smith.common.ApiResponse;
import com.ssw.smith.model.vo.OtlpIngestResult;
import com.ssw.smith.service.OtlpService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/otlp")
public class OtlpController {

    private static final String APPLICATION_X_PROTOBUF = "application/x-protobuf";

    private final OtlpService otlpService;

    public OtlpController(OtlpService otlpService) {
        this.otlpService = otlpService;
    }

    @PostMapping(value = "/v1/traces", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<OtlpIngestResult> ingestJsonTraces(@RequestBody JsonNode body) {
        return ApiResponse.ok(otlpService.ingestTraces(body));
    }

    @PostMapping(value = "/v1/traces", consumes = APPLICATION_X_PROTOBUF)
    public ApiResponse<OtlpIngestResult> ingestProtoTraces(@RequestBody byte[] body) {
        return ApiResponse.ok(otlpService.ingestTraces(body));
    }
}