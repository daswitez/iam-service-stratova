package com.solveria.iamservice.api.rest.dto;

import java.time.Instant;

public record AdminCompetitionTenantResponse(
        String id,
        String competitionId,
        String competitionCode,
        String tenantId,
        String tenantCode,
        String tenantName,
        String tenantType,
        String tenantStatus,
        Instant createdAt) {}
