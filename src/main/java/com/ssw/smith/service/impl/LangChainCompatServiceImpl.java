package com.ssw.smith.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssw.smith.context.ProjectContext;
import com.ssw.smith.model.dto.ProjectCreateRequest;
import com.ssw.smith.model.dto.RunCreateRequest;
import com.ssw.smith.model.dto.RunEndRequest;
import com.ssw.smith.model.dto.TraceCreateRequest;
import com.ssw.smith.model.entity.ProjectEntity;
import com.ssw.smith.model.entity.RunEntity;
import com.ssw.smith.model.entity.TraceEntity;
import com.ssw.smith.service.LangChainCompatService;
import com.ssw.smith.service.ProjectService;
import com.ssw.smith.service.RunService;
import com.ssw.smith.service.TraceService;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class LangChainCompatServiceImpl implements LangChainCompatService {

    private static final String DEFAULT_SESSION_NAME = "default";

    private final ProjectService projectService;
    private final TraceService traceService;
    private final RunService runService;
    private final ObjectMapper objectMapper;

    public LangChainCompatServiceImpl(
            ProjectService projectService,
            TraceService traceService,
            RunService runService,
            ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.traceService = traceService;
        this.runService = runService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> submitMetadata(JsonNode body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("metadata", body == null ? objectMapper.createObjectNode() : body);
        return result;
    }

    @Override
    public RunEntity createRun(JsonNode body) {
        log.info("收到 LangChain Run 上报内容：{}", body);
        String runKey = firstText(body, "id", "run_id", "runKey");
        if (!StringUtils.hasText(runKey)) {
            runKey = UUID.randomUUID().toString();
        }
        String traceKey = firstText(body, "trace_id", "traceId");
        if (!StringUtils.hasText(traceKey)) {
            traceKey = runKey;
        }

        ProjectEntity project = resolveProject(body);
        TraceEntity trace = ensureTrace(project.getId(), traceKey, body);
        Long parentRunId = resolveParentRunId(body);

        RunEntity existing = findRun(runKey);
        if (existing != null) {
            updateRunModelName(existing, body);
            return existing;
        }

        return runService.create(new RunCreateRequest(
                trace.getId(),
                parentRunId,
                runKey,
                textOrDefault(firstText(body, "name"), "langchain-run"),
                textOrDefault(firstText(body, "run_type", "runType"), "CHAIN"),
                normalizeStatus(firstText(body, "status"), firstText(body, "error"), boolValue(body, "dotted_order")),
                modelName(body),
                durationMs(parseTime(firstText(body, "start_time", "startedAt")), parseTime(firstText(body, "end_time", "endedAt"))),
                intValue(body, "prompt_tokens", "promptTokens", "extra.usage_metadata.input_tokens"),
                intValue(body, "completion_tokens", "completionTokens", "extra.usage_metadata.output_tokens"),
                intValue(body, "total_tokens", "totalTokens", "extra.usage_metadata.total_tokens"),
                firstNode(body, "inputs", "input"),
                firstNode(body, "outputs", "output"),
                metadata(body),
                firstText(body, "error", "errorMessage"),
                parseTime(firstText(body, "start_time", "startedAt")),
                parseTime(firstText(body, "end_time", "endedAt"))
        ));
    }

    @Override
    public RunEntity patchRun(String runId, JsonNode body) {
        log.info("收到 LangChain Run 上报内容：{}", body);
        RunEntity run = runService.end(new RunEndRequest(
                null,
                runId,
                normalizeStatus(firstText(body, "status"), firstText(body, "error"), null),
                durationMs(parseTime(firstText(body, "start_time", "startedAt")), parseTime(firstText(body, "end_time", "endedAt"))),
                intValue(body, "prompt_tokens", "promptTokens", "extra.usage_metadata.input_tokens"),
                intValue(body, "completion_tokens", "completionTokens", "extra.usage_metadata.output_tokens"),
                intValue(body, "total_tokens", "totalTokens", "extra.usage_metadata.total_tokens"),
                firstNode(body, "outputs", "output"),
                metadata(body),
                firstText(body, "error", "errorMessage"),
                parseTime(firstText(body, "end_time", "endedAt"))
        ));
        updateRunModelName(run, body);
        syncTraceFromRootRun(run, body);
        return run;
    }
    @Override
    public void ingestRunsMultipart(Collection<Part> parts) {
        Map<String, ObjectNode> groupedRuns = new LinkedHashMap<>();
        Map<String, String> groupedOperations = new LinkedHashMap<>();
        List<RunEntity> created = new ArrayList<>();
        List<RunEntity> updated = new ArrayList<>();
        List<String> rejected = new ArrayList<>();

        for (Part part : parts) {
            String name = part.getName();
            try {
                JsonNode body = readPartAsJson(part);
                if (name.startsWith("post.") && !name.endsWith(".inputs")) {
                    mergeLangSmithRunPart("post", name, body, groupedRuns, groupedOperations);
                } else if (name.startsWith("patch.") && !name.endsWith(".outputs")) {
                    mergeLangSmithRunPart("patch", name, body, groupedRuns, groupedOperations);
                } else if (name.startsWith("post.") && name.endsWith(".inputs")) {
                    mergeLangSmithRunPart("post", name, body, groupedRuns, groupedOperations);
                } else if (name.startsWith("patch.") && name.endsWith(".outputs")) {
                    mergeLangSmithRunPart("patch", name, body, groupedRuns, groupedOperations);
                } else if (name.startsWith("feedback.")) {
                    log.debug("忽略 LangSmith feedback multipart 分片：{}", name);
                } else if (name.startsWith("attachment.")) {
                    log.debug("忽略 LangSmith attachment multipart 分片：{}", name);
                } else if ("operations".equalsIgnoreCase(name)) {
                    processOperationsNode(body, created, updated, rejected);
                } else {
                    log.debug("忽略暂不支持的 LangSmith multipart 分片：{}", name);
                }
            } catch (Exception ex) {
                rejected.add(name + ": " + ex.getMessage());
                log.warn("处理 LangSmith multipart 分片失败，分片名={}，原因={}", name, ex.getMessage());
            }
        }

        flushGroupedRuns(groupedRuns, groupedOperations, created, updated, rejected);
        if (!rejected.isEmpty()) {
            log.warn("LangSmith multipart 上报处理完成，其中 {} 个分片被拒绝：{}", rejected.size(), rejected);
        }
    }

    private JsonNode readPartAsJson(Part part) throws IOException {
        String content = new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!StringUtils.hasText(content)) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }

    private void mergeLangSmithRunPart(
            String operation,
            String partName,
            JsonNode node,
            Map<String, ObjectNode> groupedRuns,
            Map<String, String> groupedOperations) {
        String[] parts = partName.split("\\.", 3);
        if (parts.length < 2) {
            return;
        }

        String runId = parts[1];
        String normalizedOperation = normalizeOperation(operation);
        String key = normalizedOperation + ":" + runId;
        ObjectNode runNode = groupedRuns.computeIfAbsent(key, ignored -> objectMapper.createObjectNode());
        groupedOperations.putIfAbsent(key, normalizedOperation);
        if (!runNode.has("id")) {
            runNode.put("id", runId);
        }
        if (!runNode.has("run_id")) {
            runNode.put("run_id", runId);
        }

        if (parts.length == 2) {
            if (node != null && node.isObject()) {
                node.fields().forEachRemaining(entry -> runNode.set(entry.getKey(), entry.getValue()));
            }
            return;
        }

        String fieldName = parts[2];
        if (StringUtils.hasText(fieldName)) {
            runNode.set(fieldName, node);
        }
    }

    private void flushGroupedRuns(
            Map<String, ObjectNode> groupedRuns,
            Map<String, String> groupedOperations,
            List<RunEntity> created,
            List<RunEntity> updated,
            List<String> rejected) {
        groupedRuns.forEach((key, node) -> {
            String operation = groupedOperations.getOrDefault(key, "post");
            if ("patch".equals(operation)) {
                processMultipartNode("patch", node, created, updated, rejected);
            } else {
                processMultipartNode("post", node, created, updated, rejected);
            }
        });
    }

    private void processOperationsNode(
            JsonNode node,
            List<RunEntity> created,
            List<RunEntity> updated,
            List<String> rejected) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> processOperationsNode(item, created, updated, rejected));
            return;
        }
        if (!node.isObject()) {
            return;
        }

        boolean handled = false;
        if (node.has("operations")) {
            handled = true;
            processOperationsNode(node.path("operations"), created, updated, rejected);
        }
        if (node.has("post")) {
            handled = true;
            processMultipartNode("post", node.path("post"), created, updated, rejected);
        }
        if (node.has("runs")) {
            handled = true;
            processMultipartNode("runs", node.path("runs"), created, updated, rejected);
        }
        if (node.has("patch")) {
            handled = true;
            processMultipartNode("patch", node.path("patch"), created, updated, rejected);
        }
        if (node.has("create")) {
            handled = true;
            processMultipartNode("create", node.path("create"), created, updated, rejected);
        }
        if (node.has("update")) {
            handled = true;
            processMultipartNode("patch", node.path("update"), created, updated, rejected);
        }
        if (node.has("method") || node.has("op")) {
            handled = true;
            processOperationItem(node, created, updated, rejected);
        }
        if (!handled && isRunLikeNode(node)) {
            processMultipartNode("runs", node, created, updated, rejected);
        }
    }

    private void processOperationItem(
            JsonNode node,
            List<RunEntity> created,
            List<RunEntity> updated,
            List<String> rejected) {
        String method = textOrDefault(firstText(node, "method", "op", "operation"), "");
        JsonNode payload = firstNode(node, "body", "payload", "run", "data");
        if (payload == null) {
            payload = node;
        }
        if (method.equalsIgnoreCase("patch")
                || method.equalsIgnoreCase("update")
                || method.equalsIgnoreCase("end")) {
            processMultipartNode("patch", payload, created, updated, rejected);
            return;
        }
        if (method.equalsIgnoreCase("post")
                || method.equalsIgnoreCase("create")
                || method.equalsIgnoreCase("insert")) {
            processMultipartNode("post", payload, created, updated, rejected);
        }
    }

    private void processMultipartNode(
            String partName,
            JsonNode node,
            List<RunEntity> created,
            List<RunEntity> updated,
            List<String> rejected) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> processMultipartNode(partName, item, created, updated, rejected));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        if (node.has("post") || node.has("runs") || node.has("patch")) {
            if (node.has("post")) {
                processMultipartNode("post", node.path("post"), created, updated, rejected);
            }
            if (node.has("runs")) {
                processMultipartNode("runs", node.path("runs"), created, updated, rejected);
            }
            if (node.has("patch")) {
                processMultipartNode("patch", node.path("patch"), created, updated, rejected);
            }
            return;
        }
        if (isControlNode(node)) {
            return;
        }

        if (isPatchPart(partName)) {
            String runId = firstText(node, "id", "run_id", "runKey");
            if (!StringUtils.hasText(runId)) {
                rejected.add(partName + ": run id is required for patch");
                return;
            }
            try {
                updated.add(patchRun(runId, node));
            } catch (Exception ex) {
                rejected.add(partName + "/" + runId + ": " + ex.getMessage());
            }
            return;
        }

        if (!isRunLikeNode(node)) {
            return;
        }
        try {
            created.add(createRun(node));
        } catch (Exception ex) {
            String runId = textOrDefault(firstText(node, "id", "run_id", "runKey"), "unknown");
            rejected.add(partName + "/" + runId + ": " + ex.getMessage());
        }
    }

    private String normalizeOperation(String operation) {
        return "patch".equalsIgnoreCase(operation) || "update".equalsIgnoreCase(operation) ? "patch" : "post";
    }

    private boolean isPatchPart(String partName) {
        return StringUtils.hasText(partName) && (partName.toLowerCase().contains("patch") || partName.toLowerCase().contains("update"));
    }

    private boolean isControlNode(JsonNode node) {
        return node.has("query")
                || node.has("variables")
                || node.has("map")
                || node.has("attachments");
    }

    private boolean isRunLikeNode(JsonNode node) {
        return node != null
                && node.isObject()
                && (node.has("id")
                || node.has("run_id")
                || node.has("runKey")
                || node.has("trace_id")
                || node.has("traceId")
                || node.has("name")
                || node.has("inputs")
                || node.has("outputs")
                || node.has("input")
                || node.has("output"));
    }
    private ProjectEntity resolveProject(JsonNode body) {
        Long contextProjectId = ProjectContext.getProjectId();
        if (contextProjectId != null) {
            ProjectEntity project = projectService.getById(contextProjectId);
            if (project != null) {
                return project;
            }
        }

        Long projectId = longValue(body, "projectId", "project_id", "session_id");
        if (projectId != null) {
            ProjectEntity project = projectService.getById(projectId);
            if (project != null) {
                return project;
            }
        }
        String projectName = textOrDefault(firstText(body, "session_name", "project_name", "projectName"), DEFAULT_SESSION_NAME);
        ProjectEntity project = findProjectByName(projectName);
        if (project != null) {
            return project;
        }
        return projectService.create(new ProjectCreateRequest(projectName, "LangChain compatible session", "dev"));
    }

    private ProjectEntity findProjectByName(String name) {
        return projectService.getOne(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getName, name)
                .last("limit 1"), false);
    }

    private TraceEntity ensureTrace(Long projectId, String traceKey, JsonNode body) {
        TraceEntity existing = findTrace(traceKey);
        if (existing != null) {
            return existing;
        }
        return traceService.create(new TraceCreateRequest(
                projectId,
                traceKey,
                textOrDefault(firstText(body, "name"), "langchain-trace"),
                normalizeStatus(firstText(body, "status"), firstText(body, "error"), null),
                durationMs(parseTime(firstText(body, "start_time", "startedAt")), parseTime(firstText(body, "end_time", "endedAt"))),
                intValue(body, "total_tokens", "totalTokens", "extra.usage_metadata.total_tokens"),
                intValue(body, "prompt_tokens", "promptTokens", "extra.usage_metadata.input_tokens"),
                intValue(body, "completion_tokens", "completionTokens", "extra.usage_metadata.output_tokens"),
                firstNode(body, "inputs", "input"),
                firstNode(body, "outputs", "output"),
                metadata(body),
                firstText(body, "error", "errorMessage"),
                parseTime(firstText(body, "start_time", "startedAt")),
                parseTime(firstText(body, "end_time", "endedAt"))
        ));
    }

    private TraceEntity findTrace(String traceKey) {
        return traceService.getOne(new LambdaQueryWrapper<TraceEntity>()
                .eq(TraceEntity::getTraceKey, traceKey)
                .last("limit 1"), false);
    }

    private RunEntity findRun(String runKey) {
        return runService.getOne(new LambdaQueryWrapper<RunEntity>()
                .eq(RunEntity::getRunKey, runKey)
                .last("limit 1"), false);
    }

    private Long resolveParentRunId(JsonNode body) {
        String parentRunKey = firstText(body, "parent_run_id", "parentRunId");
        if (!StringUtils.hasText(parentRunKey)) {
            return null;
        }
        RunEntity parent = findRun(parentRunKey);
        return parent == null ? null : parent.getId();
    }

    private void syncTraceFromRootRun(RunEntity run, JsonNode body) {
        TraceEntity trace = traceService.getById(run.getTraceId());
        if (trace == null || !run.getRunKey().equals(trace.getTraceKey())) {
            return;
        }
        trace.setStatus(run.getStatus());
        trace.setLatencyMs(run.getLatencyMs());
        trace.setPromptTokens(run.getPromptTokens());
        trace.setCompletionTokens(run.getCompletionTokens());
        trace.setTotalTokens(run.getTotalTokens());
        trace.setOutput(run.getOutput());
        trace.setErrorMessage(run.getErrorMessage());
        trace.setEndedAt(parseTime(firstText(body, "end_time", "endedAt")));
        traceService.updateById(trace);
    }

    private JsonNode metadata(JsonNode body) {
        JsonNode extra = firstNode(body, "extra", "metadata");
        return extra == null ? objectMapper.createObjectNode() : extra;
    }
    private void updateRunModelName(RunEntity run, JsonNode body) {
        String model = modelName(body);
        if (!StringUtils.hasText(model)) {
            return;
        }
        if (model.equals(run.getModelName())) {
            return;
        }
        run.setModelName(model);
        runService.updateById(run);
    }
    private String modelName(JsonNode body) {
        String model = firstText(
                body,
                "ls_model_name",
                "model_name",
                "model",
                "modelName",
                "metadata.ls_model_name",
                "metadata.model_name",
                "metadata.model",
                "extra.metadata.ls_model_name",
                "extra.metadata.model_name",
                "extra.metadata.model",
                "invocation_params.model_name",
                "invocation_params.model",
                "extra.invocation_params.model_name",
                "extra.invocation_params.model",
                "kwargs.model_name",
                "kwargs.model",
                "serialized.kwargs.model_name",
                "serialized.kwargs.model"
        );
        return model;
    }

    private String firstText(JsonNode node, String... paths) {
        JsonNode value = firstNode(node, paths);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return StringUtils.hasText(text) ? text : null;
    }

    private JsonNode firstNode(JsonNode node, String... paths) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String path : paths) {
            JsonNode current = node;
            for (String part : path.split("\\.")) {
                current = current.path(part);
            }
            if (!current.isMissingNode() && !current.isNull()) {
                return current;
            }
        }
        return null;
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private Integer intValue(JsonNode node, String... paths) {
        JsonNode value = firstNode(node, paths);
        return value == null || !value.canConvertToInt() ? 0 : value.asInt();
    }

    private Long longValue(JsonNode node, String... paths) {
        JsonNode value = firstNode(node, paths);
        return value == null || !value.canConvertToLong() ? null : value.asLong();
    }

    private Boolean boolValue(JsonNode node, String... paths) {
        JsonNode value = firstNode(node, paths);
        return value == null || !value.isBoolean() ? null : value.asBoolean();
    }

    private String normalizeStatus(String status, String error, Boolean runningSignal) {
        if (StringUtils.hasText(error)) {
            return "ERROR";
        }
        if (StringUtils.hasText(status)) {
            if ("error".equalsIgnoreCase(status)) return "ERROR";
            if ("running".equalsIgnoreCase(status)) return "RUNNING";
            return status.toUpperCase();
        }
        return Boolean.TRUE.equals(runningSignal) ? "RUNNING" : "SUCCESS";
    }

    private LocalDateTime parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ignored) {
            return LocalDateTime.parse(value);
        }
    }

    private Long durationMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toMillis();
    }
}