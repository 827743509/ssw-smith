package com.ssw.smith.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ssw.smith.model.dto.RunCreateRequest;
import com.ssw.smith.model.dto.RunEndRequest;
import com.ssw.smith.model.entity.RunEntity;

public interface RunService extends IService<RunEntity> {

    RunEntity create(RunCreateRequest request);

    RunEntity start(RunCreateRequest request);

    RunEntity end(RunEndRequest request);
}