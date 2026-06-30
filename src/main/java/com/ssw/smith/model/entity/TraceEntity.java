package com.ssw.smith.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("obs_trace")
public class TraceEntity {

    private Long id;
    private Long projectId;
    private String traceKey;
    private String name;
    private String status;
    private Long latencyMs;
    private Integer totalTokens;
    private Integer promptTokens;
    private Integer completionTokens;
    private String input;
    private String output;
    private String metadata;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
