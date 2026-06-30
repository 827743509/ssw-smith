package com.ssw.smith.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.ssw.smith.mapper.FeedbackMapper;
import com.ssw.smith.mapper.RunMapper;
import com.ssw.smith.mapper.TraceMapper;
import com.ssw.smith.model.dto.TraceCreateRequest;
import com.ssw.smith.model.entity.FeedbackEntity;
import com.ssw.smith.model.entity.RunEntity;
import com.ssw.smith.model.entity.TraceEntity;
import com.ssw.smith.model.vo.DashboardStats;
import com.ssw.smith.model.vo.TraceDetail;
import com.ssw.smith.service.TraceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TraceServiceImpl extends ServiceImpl<TraceMapper, TraceEntity> implements TraceService {

    private final RunMapper runMapper;
    private final FeedbackMapper feedbackMapper;

    public TraceServiceImpl(RunMapper runMapper, FeedbackMapper feedbackMapper) {
        this.runMapper = runMapper;
        this.feedbackMapper = feedbackMapper;
    }

    @Override
    public TraceEntity create(TraceCreateRequest request) {
        TraceEntity trace = new TraceEntity();
        trace.setProjectId(request.projectId());
        trace.setTraceKey(StringUtils.hasText(request.traceKey()) ? request.traceKey() : UUID.randomUUID().toString());
        trace.setName(request.name());
        trace.setStatus(StringUtils.hasText(request.status()) ? request.status() : "SUCCESS");
        trace.setLatencyMs(request.latencyMs());
        trace.setTotalTokens(defaultInt(request.totalTokens()));
        trace.setPromptTokens(defaultInt(request.promptTokens()));
        trace.setCompletionTokens(defaultInt(request.completionTokens()));
        trace.setInput(toJson(request.input()));
        trace.setOutput(toJson(request.output()));
        trace.setMetadata(toJson(request.metadata()));
        trace.setErrorMessage(request.errorMessage());
        trace.setStartedAt(request.startedAt() == null ? LocalDateTime.now() : request.startedAt());
        trace.setEndedAt(request.endedAt());
        save(trace);
        return trace;
    }

    @Override
    public IPage<TraceEntity> pageByProject(Long projectId, String status, int pageNo, int pageSize) {
        LambdaQueryWrapper<TraceEntity> wrapper = new LambdaQueryWrapper<TraceEntity>()
                .eq(projectId != null, TraceEntity::getProjectId, projectId)
                .eq(StringUtils.hasText(status), TraceEntity::getStatus, status)
                .orderByDesc(TraceEntity::getStartedAt)
                .orderByDesc(TraceEntity::getId);
        return page(Page.of(pageNo, pageSize), wrapper);
    }

    @Override
    public TraceDetail detail(Long traceId) {
        TraceEntity trace = getById(traceId);
        if (trace == null) {
            throw new IllegalArgumentException("Trace not found");
        }
        List<RunEntity> runs = runMapper.selectList(new LambdaQueryWrapper<RunEntity>()
                .eq(RunEntity::getTraceId, traceId)
                .orderByAsc(RunEntity::getStartedAt)
                .orderByAsc(RunEntity::getId));
        List<FeedbackEntity> feedbacks = feedbackMapper.selectList(new LambdaQueryWrapper<FeedbackEntity>()
                .eq(FeedbackEntity::getTraceId, traceId)
                .orderByDesc(FeedbackEntity::getCreatedAt));
        return new TraceDetail(trace, runs, feedbacks);
    }

    @Override
    public DashboardStats dashboard(Long projectId) {
        List<TraceEntity> traces = list(new LambdaQueryWrapper<TraceEntity>()
                .eq(projectId != null, TraceEntity::getProjectId, projectId));
        long successCount = traces.stream().filter(trace -> "SUCCESS".equalsIgnoreCase(trace.getStatus())).count();
        long errorCount = traces.stream().filter(trace -> "ERROR".equalsIgnoreCase(trace.getStatus())).count();
        long totalTokens = traces.stream().mapToLong(trace -> trace.getTotalTokens() == null ? 0 : trace.getTotalTokens()).sum();
        BigDecimal avgLatency = traces.stream()
                .filter(trace -> trace.getLatencyMs() != null)
                .map(trace -> BigDecimal.valueOf(trace.getLatencyMs()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long latencyCount = traces.stream().filter(trace -> trace.getLatencyMs() != null).count();
        if (latencyCount > 0) {
            avgLatency = avgLatency.divide(BigDecimal.valueOf(latencyCount), 2, RoundingMode.HALF_UP);
        }

        List<FeedbackEntity> feedbacks = feedbackMapper.selectList(null);
        if (projectId != null) {
            List<Long> traceIds = traces.stream().map(TraceEntity::getId).toList();
            feedbacks = feedbacks.stream().filter(item -> traceIds.contains(item.getTraceId())).toList();
        }
        BigDecimal avgScore = feedbacks.stream()
                .filter(feedback -> feedback.getScore() != null)
                .map(FeedbackEntity::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long scoreCount = feedbacks.stream().filter(feedback -> feedback.getScore() != null).count();
        if (scoreCount > 0) {
            avgScore = avgScore.divide(BigDecimal.valueOf(scoreCount), 2, RoundingMode.HALF_UP);
        }

        return new DashboardStats(traces.size(), successCount, errorCount, totalTokens, avgLatency, avgScore);
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String toJson(JsonNode node) {
        return node == null ? null : node.toString();
    }
}
