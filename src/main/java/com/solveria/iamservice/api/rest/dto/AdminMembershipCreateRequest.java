package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminMembershipCreateRequest(
        @NotNull(message = "User id is required") Long userId,
        @NotBlank(message = "Tenant id is required") String tenantId,
        @Pattern(
                        regexp = "^(ACTIVE|INVITED|SUSPENDED|LEFT)$",
                        message = "Status must be ACTIVE, INVITED, SUSPENDED, or LEFT")
                String status,
        boolean isPrimary) {}
