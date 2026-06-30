package com.ssw.smith.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.ssw.smith.mapper.RunMapper;
import com.ssw.smith.model.dto.RunCreateRequest;
import com.ssw.smith.model.dto.RunEndRequest;
import com.ssw.smith.model.entity.RunEntity;
import com.ssw.smith.service.RunService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RunServiceImpl extends ServiceImpl<RunMapper, RunEntity> implements RunService {

    @Override
    public RunEntity create(RunCreateRequest request) {
        RunEntity run = buildRun(request, StringUtils.hasText(request.status()) ? request.status() : "SUCCESS");
        save(run);
        return run;
    }

    @Override
    public RunEntity start(RunCreateRequest request) {
        RunEntity run = buildRun(request, "RUNNING");
        save(run);
        return run;
    }

    @Override
    public RunEntity end(RunEndRequest request) {
        RunEntity run = findRun(request);
        LocalDateTime endedAt = request.endedAt() == null ? LocalDateTime.now() : request.endedAt();
        run.setStatus(StringUtils.hasText(request.status()) ? request.status() : "SUCCESS");
        run.setPromptTokens(request.promptTokens() == null ? run.getPromptTokens() : request.promptTokens());
        run.setCompletionTokens(request.completionTokens() == null ? run.getCompletionTokens() : request.completionTokens());
        run.setTotalTokens(request.totalTokens() == null ? run.getTotalTokens() : request.totalTokens());
        run.setOutput(request.output() == null ? run.getOutput() : toJson(request.output()));
        run.setMetadata(request.metadata() == null ? run.getMetadata() : toJson(request.metadata()));
        run.setErrorMessage(request.errorMessage());
        run.setEndedAt(endedAt);
        run.setLatencyMs(request.latencyMs() == null ? calculateLatency(run.getStartedAt(), endedAt) : request.latencyMs());
        updateById(run);
        return run;
    }

    private RunEntity buildRun(RunCreateRequest request, String status) {
        RunEntity run = new RunEntity();
        run.setTraceId(request.traceId());
        run.setParentRunId(request.parentRunId());
        run.setRunKey(StringUtils.hasText(request.runKey()) ? request.runKey() : UUID.randomUUID().toString());
        run.setName(request.name());
        run.setRunType(StringUtils.hasText(request.runType()) ? request.runType() : "LLM");
        run.setStatus(status);
        run.setModelName(request.modelName());
        run.setLatencyMs(request.latencyMs());
        run.setPromptTokens(defaultInt(request.promptTokens()));
        run.setCompletionTokens(defaultInt(request.completionTokens()));
        run.setTotalTokens(defaultInt(request.totalTokens()));
        run.setInput(toJson(request.input()));
        run.setOutput(toJson(request.output()));
        run.setMetadata(toJson(request.metadata()));
        run.setErrorMessage(request.errorMessage());
        run.setStartedAt(request.startedAt() == null ? LocalDateTime.now() : request.startedAt());
        run.setEndedAt(request.endedAt());
        return run;
    }

    private RunEntity findRun(RunEndRequest request) {
        if (request.id() != null) {
            RunEntity run = getById(request.id());
            if (run != null) {
                return run;
            }
        }
        if (StringUtils.hasText(request.runKey())) {
            RunEntity run = getOne(new LambdaQueryWrapper<RunEntity>()
                    .eq(RunEntity::getRunKey, request.runKey())
                    .last("limit 1"));
            if (run != null) {
                return run;
            }
        }
        throw new IllegalArgumentException("Run not found");
    }

    private Long calculateLatency(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            return null;
        }
        return Duration.between(startedAt, endedAt).toMillis();
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String toJson(JsonNode node) {
        return node == null ? null : node.toString();
    }
}