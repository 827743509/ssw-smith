package com.ssw.smith.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreateRequest(
        @NotBlank String name,
        String description,
        String environment
) {
}
