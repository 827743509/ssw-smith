package com.ssw.smith.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ssw.smith.common.ApiResponse;
import com.ssw.smith.model.dto.TraceCreateRequest;
import com.ssw.smith.model.entity.TraceEntity;
import com.ssw.smith.model.vo.DashboardStats;
import com.ssw.smith.model.vo.TraceDetail;
import com.ssw.smith.service.TraceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/traces")
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping
    public ApiResponse<IPage<TraceEntity>> page(
            @RequestParam(name = "projectId", required = false) Long projectId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(traceService.pageByProject(projectId, status, pageNo, pageSize));
    }

    @GetMapping("/{traceId}")
    public ApiResponse<TraceDetail> detail(@PathVariable("traceId") Long traceId) {
        return ApiResponse.ok(traceService.detail(traceId));
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStats> dashboard(@RequestParam(name = "projectId", required = false) Long projectId) {
        return ApiResponse.ok(traceService.dashboard(projectId));
    }

    @PostMapping
    public ApiResponse<TraceEntity> create(@Valid @RequestBody TraceCreateRequest request) {
        return ApiResponse.ok(traceService.create(request));
    }
}