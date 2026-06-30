package com.ssw.smith.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ssw.smith.model.dto.FeedbackCreateRequest;
import com.ssw.smith.model.entity.FeedbackEntity;

public interface FeedbackService extends IService<FeedbackEntity> {

    FeedbackEntity create(FeedbackCreateRequest request);
}
