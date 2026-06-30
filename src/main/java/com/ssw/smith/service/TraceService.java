package com.ssw.smith.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ssw.smith.model.dto.TraceCreateRequest;
import com.ssw.smith.model.entity.TraceEntity;
import com.ssw.smith.model.vo.DashboardStats;
import com.ssw.smith.model.vo.TraceDetail;

public interface TraceService extends IService<TraceEntity> {

    TraceEntity create(TraceCreateRequest request);

    IPage<TraceEntity> pageByProject(Long projectId, String status, int pageNo, int pageSize);

    TraceDetail detail(Long traceId);

    DashboardStats dashboard(Long projectId);
}
