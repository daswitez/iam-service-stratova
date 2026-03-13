package com.solveria.iamservice.api.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminCompetitionResponse(
        String id,
        String code,
        String name,
        String description,
        String scope,
        String status,
        String hostTenantId,
        String hostTenantCode,
        String hostTenantName,
        String productName,
        String industryCode,
        String industryName,
        BigDecimal initialCapitalAmount,
        String currency,
        int minTeamSize,
        int maxTeamSize,
        String teamCreationMode,
        String roleAssignmentMethod,
        boolean allowOptionalCoo,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt) {}
