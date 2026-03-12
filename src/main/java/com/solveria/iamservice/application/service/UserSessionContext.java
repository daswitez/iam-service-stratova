package com.solveria.iamservice.application.service;

import com.solveria.iamservice.api.rest.dto.AuthResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record UserSessionContext(
        String primaryTenantId,
        String activeTenantId,
        Set<String> roles,
        List<AuthResponse.TenantMembershipDto> memberships,
        List<AuthResponse.TeamCompetitionDto> teamCompetitions) {

    public Map<String, Object> toClaims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        if (primaryTenantId != null) {
            claims.put("primaryTenantId", primaryTenantId);
        }
        if (activeTenantId != null) {
            claims.put("activeTenantId", activeTenantId);
        }
        claims.put(
                "tenantIds",
                memberships.stream().map(AuthResponse.TenantMembershipDto::tenantId).toList());
        claims.put(
                "competitionIds",
                teamCompetitions.stream()
                        .map(AuthResponse.TeamCompetitionDto::competitionId)
                        .distinct()
                        .toList());
        claims.put(
                "teamIds",
                teamCompetitions.stream().map(AuthResponse.TeamCompetitionDto::teamId).toList());
        claims.put("roles", roles);
        return claims;
    }
}
