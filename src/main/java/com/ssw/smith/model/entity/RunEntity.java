package com.ssw.smith.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("obs_run")
public class RunEntity {

    private Long id;
    private Long traceId;
    private Long parentRunId;
    private String runKey;
    private String name;
    private String runType;
    private String status;
    private String modelName;
    private Long latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
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
