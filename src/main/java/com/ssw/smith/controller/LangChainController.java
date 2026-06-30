package com.ssw.smith.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssw.smith.service.LangChainCompatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("langChain")
@Slf4j
public class LangChainController {

    private final LangChainCompatService langChainCompatService;

    public LangChainController(LangChainCompatService langChainCompatService) {
        this.langChainCompatService = langChainCompatService;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> batchIngestConfig = new java.util.LinkedHashMap<>();
        batchIngestConfig.put("use_multipart_endpoint", true);
        batchIngestConfig.put("size_limit", 100);
        batchIngestConfig.put("scale_up_qsize_trigger", 1000);
        batchIngestConfig.put("scale_up_nthreads_limit", 16);
        batchIngestConfig.put("scale_down_nempty_trigger", 4);

        Map<String, Object> instanceFlags = new java.util.LinkedHashMap<>();
        instanceFlags.put("zstd_compression_enabled", false);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("version", "0.8.9");
        result.put("batch_ingest_config", batchIngestConfig);
        result.put("instance_flags", instanceFlags);
        return result;
    }

    @PostMapping("/v1/metadata/submit")
    public Map<String, Object> submitMetadata(@RequestBody JsonNode body) {
        return langChainCompatService.submitMetadata(body);
    }

    @PostMapping(value = "/runs/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> ingestRunsMultipart(HttpServletRequest request) {
        log.info("收到 LangSmith multipart 上报请求：contentType={}，encoding={}", request.getContentType(), request.getHeader("Content-Encoding"));
        try {
            langChainCompatService.ingestRunsMultipart(request.getParts());
        } catch (Exception ex) {
            log.warn("解析 LangSmith multipart 请求失败：{}", ex.getMessage(), ex);
        }
        return ResponseEntity.accepted().build();
    }
}
