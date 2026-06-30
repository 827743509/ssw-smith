package com.ssw.smith.context;

public final class ProjectContext {

    private static final ThreadLocal<Long> PROJECT_ID_HOLDER = new ThreadLocal<>();

    private ProjectContext() {
    }

    public static void setProjectId(Long projectId) {
        PROJECT_ID_HOLDER.set(projectId);
    }

    public static Long getProjectId() {
        return PROJECT_ID_HOLDER.get();
    }

    public static void clear() {
        PROJECT_ID_HOLDER.remove();
    }
}