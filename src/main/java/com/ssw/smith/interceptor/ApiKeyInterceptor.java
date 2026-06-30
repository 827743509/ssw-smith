package com.ssw.smith.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ssw.smith.context.ProjectContext;
import com.ssw.smith.model.entity.ProjectEntity;
import com.ssw.smith.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ProjectService projectService;

    public ApiKeyInterceptor(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ProjectContext.clear();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String apiKey = extractApiKey(request);
        if (!StringUtils.hasText(apiKey)) {
            writeUnauthorized(response, "API key is required");
            return false;
        }

        ProjectEntity project = projectService.getOne(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getApiKey, apiKey)
                .last("limit 1"), false);
        if (project == null) {
            writeUnauthorized(response, "Invalid API key");
            return false;
        }

        ProjectContext.setProjectId(project.getId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ProjectContext.clear();
    }

    private String extractApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("x-api-key");
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getHeader("X-API-Key");
        }
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getHeader("api-key");
        }
        if (!StringUtils.hasText(apiKey)) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authorization) && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                apiKey = authorization.substring(7);
            }
        }
        return StringUtils.hasText(apiKey) ? apiKey.trim() : null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}