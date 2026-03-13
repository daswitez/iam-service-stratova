package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCompetitionTenantCreateRequest(
        @NotBlank(message = "Tenant id is required") String tenantId) {}
