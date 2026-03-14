package com.solveria.iamservice.api.rest.dto;

import java.time.Instant;

public record CompetitionEnrollmentResponse(
        String id,
        String competitionId,
        String competitionCode,
        Long userId,
        String originTenantId,
        String originTenantCode,
        String originTenantName,
        String participantType,
        String status,
        Long invitedByUserId,
        Instant approvedAt,
        Instant createdAt) {}
