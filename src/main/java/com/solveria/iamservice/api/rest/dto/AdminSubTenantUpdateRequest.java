package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminSubTenantUpdateRequest(
        @NotBlank(message = "Code is required")
                @Size(max = 80, message = "Code must not exceed 80 characters")
                @Pattern(
                        regexp = "^[A-Za-z0-9-]+$",
                        message = "Code may only contain letters, numbers, and hyphens")
                String code,
        @NotBlank(message = "Name is required")
                @Size(max = 255, message = "Name must not exceed 255 characters")
                String name) {}
