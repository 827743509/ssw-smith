package com.ssw.smith.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ssw.smith.mapper.ProjectMapper;
import com.ssw.smith.model.dto.ProjectCreateRequest;
import com.ssw.smith.model.entity.ProjectEntity;
import com.ssw.smith.service.ProjectService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, ProjectEntity> implements ProjectService {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public ProjectEntity create(ProjectCreateRequest request) {
        ProjectEntity project = new ProjectEntity();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setEnvironment(request.environment() == null ? "dev" : request.environment());
        project.setApiKey(generateApiKey());
        save(project);
        return project;
    }

    private String generateApiKey() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return "ssw_" + HexFormat.of().formatHex(bytes);
    }
}
