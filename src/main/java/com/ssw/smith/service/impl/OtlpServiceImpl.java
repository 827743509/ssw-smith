package com.ssw.smith.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ssw.smith.context.ProjectContext;
import com.ssw.smith.model.dto.RunCreateRequest;
import com.ssw.smith.model.dto.TraceCreateRequest;
import com.ssw.smith.model.entity.RunEntity;
import com.ssw.smith.model.entity.TraceEntity;
import com.ssw.smith.model.vo.OtlpIngestResult;
import com.ssw.smith.service.OtlpService;
import com.ssw.smith.service.RunService;
import com.ssw.smith.service.TraceService;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OtlpServiceImpl implements OtlpService {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private final TraceService traceService;
    private final RunService runService;
    private final ObjectMapper objectMapper;

    public OtlpServiceImpl(TraceService traceService, RunService runService, ObjectMapper objectMapper) {
        this.traceService = traceService;
        this.runService = runService;
        this.objectMapper = objectMapper;
    }

    @Override
    public OtlpIngestResult ingestTraces(JsonNode body) {
        return ingestSpans(readSpans(body));
    }

    @Override
    public OtlpIngestResult ingestTraces(byte[] body) {
        if (body == null || body.length == 0) {
            throw new IllegalArgumentException("OTLP protobuf 请求体不能为空");
        }
        try {
            ExportTraceServiceRequest request = ExportTraceServiceRequest.parseFrom(body);
            return ingestSpans(readSpans(request));
        } catch (InvalidProtocolBufferException ex) {
            throw new IllegalArgumentException("OTLP protobuf 请求解析失败", ex);
        }
    }

    private OtlpIngestResult ingestSpans(List<OtlpSpan> spans) {
        Long projectId = ProjectContext.getProjectId();
        if (projectId == null) {
            throw new IllegalArgumentException("projectId is required");
        }

        Map<String, List<OtlpSpan>> spansByTrace = groupSpansByTrace(spans);

        int traceCount = 0;
        int runCount = 0;
        int acceptedSpanCount = spansByTrace.values().stream().mapToInt(List::size).sum();
        int rejectedSpanCount = spans.size() - acceptedSpanCount;

        for (Map.Entry<String, List<OtlpSpan>> entry : spansByTrace.entrySet()) {
            List<OtlpSpan> traceSpans = sortTraceSpans(entry.getValue());
            OtlpSpan rootSpan = findRootSpan(traceSpans);

            TraceEntity trace = traceService.create(toTraceRequest(projectId, entry.getKey(), rootSpan, traceSpans));
            traceCount++;
            runCount += createRuns(trace.getId(), traceSpans);
        }

        return new OtlpIngestResult(traceCount, runCount, rejectedSpanCount);
    }

    private Map<String, List<OtlpSpan>> groupSpansByTrace(List<OtlpSpan> spans) {
        Map<String, List<OtlpSpan>> spansByTrace = new HashMap<>();
        for (OtlpSpan span : spans) {
            if (StringUtils.hasText(span.traceId())) {
                spansByTrace.computeIfAbsent(span.traceId(), key -> new ArrayList<>()).add(span);
            }
        }
        return spansByTrace;
    }

    private List<OtlpSpan> sortTraceSpans(List<OtlpSpan> spans) {
        return spans.stream()
                .sorted(Comparator.comparing(OtlpSpan::startTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private OtlpSpan findRootSpan(List<OtlpSpan> spans) {
        return spans.stream()
                .filter(span -> !StringUtils.hasText(span.parentSpanId()))
                .findFirst()
                .orElse(spans.get(0));
    }

    private int createRuns(Long traceId, List<OtlpSpan> spans) {
        int runCount = 0;
        Map<String, Long> runIdsBySpanId = new HashMap<>();
        for (OtlpSpan span : spans) {
            Long parentRunId = StringUtils.hasText(span.parentSpanId()) ? runIdsBySpanId.get(span.parentSpanId()) : null;
            RunEntity run = runService.create(toRunRequest(traceId, parentRunId, span));
            runIdsBySpanId.put(span.spanId(), run.getId());
            runCount++;
        }
        return runCount;
    }

    private TraceCreateRequest toTraceRequest(Long projectId, String traceId, OtlpSpan rootSpan, List<OtlpSpan> spans) {
        return new TraceCreateRequest(
                projectId,
                traceId,
                rootSpan.name(),
                status(rootSpan.statusCode()),
                durationMs(rootSpan.startTime(), rootSpan.endTime()),
                sumInt(spans, "gen_ai.usage.total_tokens", "llm.usage.total_tokens"),
                sumInt(spans, "gen_ai.usage.input_tokens", "llm.usage.prompt_tokens"),
                sumInt(spans, "gen_ai.usage.output_tokens", "llm.usage.completion_tokens"),
                firstJson(spans, "input.value", "gen_ai.prompt", "llm.prompts"),
                firstJson(spans, "output.value", "gen_ai.completion", "llm.completions"),
                rootSpan.metadata(),
                rootSpan.errorMessage(),
                rootSpan.startTime(),
                rootSpan.endTime()
        );
    }

    private RunCreateRequest toRunRequest(Long traceId, Long parentRunId, OtlpSpan span) {
        return new RunCreateRequest(
                traceId,
                parentRunId,
                span.spanId(),
                span.name(),
                runType(span),
                status(span.statusCode()),
                modelName(span),
                durationMs(span.startTime(), span.endTime()),
                intAttribute(span, "gen_ai.usage.input_tokens", "llm.usage.prompt_tokens"),
                intAttribute(span, "gen_ai.usage.output_tokens", "llm.usage.completion_tokens"),
                intAttribute(span, "gen_ai.usage.total_tokens", "llm.usage.total_tokens"),
                jsonAttribute(span, "input.value", "gen_ai.prompt", "llm.prompts"),
                jsonAttribute(span, "output.value", "gen_ai.completion", "llm.completions"),
                span.metadata(),
                span.errorMessage(),
                span.startTime(),
                span.endTime()
        );
    }

    private List<OtlpSpan> readSpans(JsonNode body) {
        List<OtlpSpan> spans = new ArrayList<>();
        for (JsonNode resourceSpan : body.path("resourceSpans")) {
            ObjectNode resourceAttributes = attributesToObject(resourceSpan.path("resource").path("attributes"));
            ArrayNode scopeGroups = resourceSpan.has("scopeSpans")
                    ? (ArrayNode) resourceSpan.path("scopeSpans")
                    : objectMapper.createArrayNode();
            if (!resourceSpan.has("scopeSpans") && resourceSpan.has("instrumentationLibrarySpans")) {
                scopeGroups = (ArrayNode) resourceSpan.path("instrumentationLibrarySpans");
            }
            for (JsonNode scopeSpan : scopeGroups) {
                readScopeSpans(spans, resourceAttributes, scopeSpan);
            }
        }
        return spans;
    }

    private void readScopeSpans(List<OtlpSpan> spans, ObjectNode resourceAttributes, JsonNode scopeSpan) {
        ObjectNode scopeAttributes = objectMapper.createObjectNode();
        JsonNode scope = scopeSpan.has("scope") ? scopeSpan.path("scope") : scopeSpan.path("instrumentationLibrary");
        if (scope.hasNonNull("name")) {
            scopeAttributes.put("name", scope.path("name").asText());
        }
        if (scope.hasNonNull("version")) {
            scopeAttributes.put("version", scope.path("version").asText());
        }
        for (JsonNode span : scopeSpan.path("spans")) {
            ObjectNode spanAttributes = attributesToObject(span.path("attributes"));
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.set("resource", resourceAttributes.deepCopy());
            metadata.set("scope", scopeAttributes.deepCopy());
            metadata.set("attributes", spanAttributes.deepCopy());
            if (span.has("events")) {
                metadata.set("events", span.path("events"));
            }

            spans.add(new OtlpSpan(
                    span.path("traceId").asText(null),
                    span.path("spanId").asText(null),
                    span.path("parentSpanId").asText(null),
                    span.path("name").asText("otel-span"),
                    span.path("kind").asText(null),
                    span.path("status").path("code").asText(null),
                    span.path("status").path("message").asText(null),
                    parseUnixNano(span.path("startTimeUnixNano")),
                    parseUnixNano(span.path("endTimeUnixNano")),
                    spanAttributes,
                    metadata
            ));
        }
    }

    private List<OtlpSpan> readSpans(ExportTraceServiceRequest request) {
        List<OtlpSpan> spans = new ArrayList<>();
        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            ObjectNode resourceAttributes = attributesToObject(resourceSpans.getResource().getAttributesList());
            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                readScopeSpans(spans, resourceAttributes, scopeSpans);
            }
        }
        return spans;
    }

    private void readScopeSpans(List<OtlpSpan> spans, ObjectNode resourceAttributes, ScopeSpans scopeSpans) {
        ObjectNode scopeAttributes = objectMapper.createObjectNode();
        if (scopeSpans.hasScope()) {
            if (StringUtils.hasText(scopeSpans.getScope().getName())) {
                scopeAttributes.put("name", scopeSpans.getScope().getName());
            }
            if (StringUtils.hasText(scopeSpans.getScope().getVersion())) {
                scopeAttributes.put("version", scopeSpans.getScope().getVersion());
            }
            ObjectNode attributes = attributesToObject(scopeSpans.getScope().getAttributesList());
            if (!attributes.isEmpty()) {
                scopeAttributes.set("attributes", attributes);
            }
        }

        for (Span span : scopeSpans.getSpansList()) {
            ObjectNode spanAttributes = attributesToObject(span.getAttributesList());
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.set("resource", resourceAttributes.deepCopy());
            metadata.set("scope", scopeAttributes.deepCopy());
            metadata.set("attributes", spanAttributes.deepCopy());
            if (span.getEventsCount() > 0) {
                metadata.set("events", eventsToArray(span.getEventsList()));
            }
            if (span.getLinksCount() > 0) {
                metadata.set("links", linksToArray(span.getLinksList()));
            }

            spans.add(new OtlpSpan(
                    hex(span.getTraceId()),
                    hex(span.getSpanId()),
                    hex(span.getParentSpanId()),
                    StringUtils.hasText(span.getName()) ? span.getName() : "otel-span",
                    span.getKind().name(),
                    span.hasStatus() ? span.getStatus().getCode().name() : null,
                    span.hasStatus() ? span.getStatus().getMessage() : null,
                    parseUnixNano(span.getStartTimeUnixNano()),
                    parseUnixNano(span.getEndTimeUnixNano()),
                    spanAttributes,
                    metadata
            ));
        }
    }

    private ArrayNode eventsToArray(List<Span.Event> events) {
        ArrayNode array = objectMapper.createArrayNode();
        for (Span.Event event : events) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", event.getName());
            node.put("timeUnixNano", Long.toUnsignedString(event.getTimeUnixNano()));
            node.set("attributes", attributesToObject(event.getAttributesList()));
            array.add(node);
        }
        return array;
    }

    private ArrayNode linksToArray(List<Span.Link> links) {
        ArrayNode array = objectMapper.createArrayNode();
        for (Span.Link link : links) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("traceId", hex(link.getTraceId()));
            node.put("spanId", hex(link.getSpanId()));
            node.set("attributes", attributesToObject(link.getAttributesList()));
            array.add(node);
        }
        return array;
    }

    private ObjectNode attributesToObject(JsonNode attributes) {
        ObjectNode object = objectMapper.createObjectNode();
        for (JsonNode attribute : attributes) {
            String key = attribute.path("key").asText(null);
            if (StringUtils.hasText(key)) {
                object.set(key, attributeValue(attribute.path("value")));
            }
        }
        return object;
    }

    private ObjectNode attributesToObject(List<KeyValue> attributes) {
        ObjectNode object = objectMapper.createObjectNode();
        for (KeyValue attribute : attributes) {
            if (StringUtils.hasText(attribute.getKey())) {
                object.set(attribute.getKey(), attributeValue(attribute.getValue()));
            }
        }
        return object;
    }

    private JsonNode attributeValue(JsonNode value) {
        if (value.has("stringValue")) return value.path("stringValue");
        if (value.has("intValue")) return value.path("intValue");
        if (value.has("doubleValue")) return value.path("doubleValue");
        if (value.has("boolValue")) return value.path("boolValue");
        if (value.has("arrayValue")) return value.path("arrayValue").path("values");
        if (value.has("kvlistValue")) return attributesToObject(value.path("kvlistValue").path("values"));
        return value;
    }

    private JsonNode attributeValue(AnyValue value) {
        return switch (value.getValueCase()) {
            case STRING_VALUE -> objectMapper.getNodeFactory().textNode(value.getStringValue());
            case BOOL_VALUE -> objectMapper.getNodeFactory().booleanNode(value.getBoolValue());
            case INT_VALUE -> objectMapper.getNodeFactory().numberNode(value.getIntValue());
            case DOUBLE_VALUE -> objectMapper.getNodeFactory().numberNode(value.getDoubleValue());
            case ARRAY_VALUE -> arrayValue(value.getArrayValue().getValuesList());
            case KVLIST_VALUE -> attributesToObject(value.getKvlistValue().getValuesList());
            case BYTES_VALUE -> objectMapper.getNodeFactory().textNode(Base64.getEncoder().encodeToString(value.getBytesValue().toByteArray()));
            case VALUE_NOT_SET -> objectMapper.nullNode();
            default -> objectMapper.nullNode();
        };
    }

    private ArrayNode arrayValue(List<AnyValue> values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (AnyValue value : values) {
            array.add(attributeValue(value));
        }
        return array;
    }

    private LocalDateTime parseUnixNano(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        long nanos = Long.parseLong(text);
        return parseUnixNano(nanos);
    }

    private LocalDateTime parseUnixNano(long nanos) {
        if (nanos <= 0) {
            return null;
        }
        Instant instant = Instant.ofEpochSecond(nanos / 1_000_000_000L, nanos % 1_000_000_000L);
        return LocalDateTime.ofInstant(instant, SYSTEM_ZONE);
    }

    private String hex(ByteString bytes) {
        if (bytes == null || bytes.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder(bytes.size() * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }

    private Long durationMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end).toMillis();
    }

    private String status(String statusCode) {
        return "STATUS_CODE_ERROR".equals(statusCode) ? "ERROR" : "SUCCESS";
    }

    private String runType(OtlpSpan span) {
        String system = textAttribute(span, "gen_ai.system", "llm.system");
        if (StringUtils.hasText(system) || StringUtils.hasText(modelName(span))) {
            return "LLM";
        }
        return "SPAN";
    }

    private String modelName(OtlpSpan span) {
        return textAttribute(span, "model_name", "gen_ai.request.model", "gen_ai.response.model", "llm.model_name", "model");
    }

    private String textAttribute(OtlpSpan span, String... keys) {
        for (String key : keys) {
            JsonNode value = span.attributes().get(key);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private Integer intAttribute(OtlpSpan span, String... keys) {
        for (String key : keys) {
            JsonNode value = span.attributes().get(key);
            if (value != null && value.canConvertToInt()) {
                return value.asInt();
            }
        }
        return 0;
    }

    private Integer sumInt(List<OtlpSpan> spans, String... keys) {
        return spans.stream().mapToInt(span -> intAttribute(span, keys)).sum();
    }

    private JsonNode jsonAttribute(OtlpSpan span, String... keys) {
        for (String key : keys) {
            JsonNode value = span.attributes().get(key);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private JsonNode firstJson(List<OtlpSpan> spans, String... keys) {
        return spans.stream()
                .map(span -> jsonAttribute(span, keys))
                .filter(value -> value != null && !value.isNull())
                .findFirst()
                .orElse(null);
    }

    private record OtlpSpan(
            String traceId,
            String spanId,
            String parentSpanId,
            String name,
            String kind,
            String statusCode,
            String errorMessage,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ObjectNode attributes,
            ObjectNode metadata
    ) {
    }
}