package com.solveria.iamservice.api.rest.dto;

import java.time.Instant;

public record AdminMembershipResponse(
        String id,
        Long userId,
        String tenantId,
        String tenantCode,
        String tenantName,
        String tenantType,
        String membershipType,
        boolean isPrimary,
        String status,
        Instant createdAt) {}
