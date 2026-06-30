package com.ssw.smith.controller;

import com.ssw.smith.common.ApiResponse;
import com.ssw.smith.model.dto.FeedbackCreateRequest;
import com.ssw.smith.model.entity.FeedbackEntity;
import com.ssw.smith.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<FeedbackEntity> create(@Valid @RequestBody FeedbackCreateRequest request) {
        return ApiResponse.ok(feedbackService.create(request));
    }
}
