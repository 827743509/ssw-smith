package com.ssw.smith.model.vo;

import com.ssw.smith.model.entity.FeedbackEntity;
import com.ssw.smith.model.entity.RunEntity;
import com.ssw.smith.model.entity.TraceEntity;

import java.util.List;

public record TraceDetail(
        TraceEntity trace,
        List<RunEntity> runs,
        List<FeedbackEntity> feedbacks
) {
}
