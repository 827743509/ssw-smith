CREATE DATABASE IF NOT EXISTS ssw_smith DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ssw_smith;

CREATE TABLE IF NOT EXISTS obs_project (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    environment VARCHAR(32) NOT NULL DEFAULT 'dev',
    api_key VARCHAR(128) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_obs_project_api_key (api_key),
    KEY idx_obs_project_env (environment)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS obs_trace (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    trace_key VARCHAR(128) NOT NULL,
    name VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    latency_ms BIGINT NULL,
    total_tokens INT NOT NULL DEFAULT 0,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    input JSON NULL,
    output JSON NULL,
    metadata JSON NULL,
    error_message TEXT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_obs_trace_key (trace_key),
    KEY idx_obs_trace_project_started (project_id, started_at),
    KEY idx_obs_trace_status (status),
    CONSTRAINT fk_obs_trace_project FOREIGN KEY (project_id) REFERENCES obs_project (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS obs_run (
    id BIGINT PRIMARY KEY,
    trace_id BIGINT NOT NULL,
    parent_run_id BIGINT NULL,
    run_key VARCHAR(128) NOT NULL,
    name VARCHAR(256) NOT NULL,
    run_type VARCHAR(64) NOT NULL DEFAULT 'LLM',
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    model_name VARCHAR(128) NULL,
    latency_ms BIGINT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    input JSON NULL,
    output JSON NULL,
    metadata JSON NULL,
    error_message TEXT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_obs_run_key (run_key),
    KEY idx_obs_run_trace_started (trace_id, started_at),
    KEY idx_obs_run_parent (parent_run_id),
    CONSTRAINT fk_obs_run_trace FOREIGN KEY (trace_id) REFERENCES obs_trace (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS obs_feedback (
    id BIGINT PRIMARY KEY,
    trace_id BIGINT NOT NULL,
    run_id BIGINT NULL,
    feedback_key VARCHAR(128) NOT NULL DEFAULT 'human_score',
    score DECIMAL(10, 4) NOT NULL,
    comment VARCHAR(1024) NULL,
    source VARCHAR(64) NOT NULL DEFAULT 'HUMAN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_obs_feedback_trace (trace_id),
    KEY idx_obs_feedback_run (run_id),
    CONSTRAINT fk_obs_feedback_trace FOREIGN KEY (trace_id) REFERENCES obs_trace (id),
    CONSTRAINT fk_obs_feedback_run FOREIGN KEY (run_id) REFERENCES obs_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
