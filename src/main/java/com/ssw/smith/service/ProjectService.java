package com.ssw.smith.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ssw.smith.model.dto.ProjectCreateRequest;
import com.ssw.smith.model.entity.ProjectEntity;

public interface ProjectService extends IService<ProjectEntity> {

    ProjectEntity create(ProjectCreateRequest request);
}
