package com.ssw.smith.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssw.smith.mapper.FeedbackMapper;
import com.ssw.smith.model.dto.FeedbackCreateRequest;
import com.ssw.smith.model.entity.FeedbackEntity;
import com.ssw.smith.service.FeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, FeedbackEntity> implements FeedbackService {

    @Override
    public FeedbackEntity create(FeedbackCreateRequest request) {
        FeedbackEntity feedback = new FeedbackEntity();
        feedback.setTraceId(request.traceId());
        feedback.setRunId(request.runId());
        feedback.setFeedbackKey(StringUtils.hasText(request.feedbackKey()) ? request.feedbackKey() : "human_score");
        feedback.setScore(request.score());
        feedback.setComment(request.comment());
        feedback.setSource(StringUtils.hasText(request.source()) ? request.source() : "HUMAN");
        save(feedback);
        return feedback;
    }
}
