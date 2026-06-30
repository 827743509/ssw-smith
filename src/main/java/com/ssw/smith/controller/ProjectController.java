package com.ssw.smith.controller;

import com.ssw.smith.common.ApiResponse;
import com.ssw.smith.model.dto.ProjectCreateRequest;
import com.ssw.smith.model.entity.ProjectEntity;
import com.ssw.smith.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<List<ProjectEntity>> list() {
        return ApiResponse.ok(projectService.list());
    }

    @PostMapping
    public ApiResponse<ProjectEntity> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.ok(projectService.create(request));
    }
}
