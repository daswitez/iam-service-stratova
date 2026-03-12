package com.solveria.iamservice.api.rest.dto;

import java.time.Instant;

public record AdminSubTenantResponse(
        String id,
        String code,
        String name,
        String type,
        String status,
        String parentTenantId,
        String parentTenantType,
        Instant createdAt) {}
