package com.solveria.iamservice.api.rest.dto;

import java.util.List;
import java.util.Set;

public record AuthResponse(String token, UserDto user, ContextDto context) {
    public AuthResponse(String token, UserDto user) {
        this(token, user, null);
    }

    public record UserDto(
            Long id,
            String username,
            String email,
            String primaryTenantId,
            String userCategory,
            Set<String> roles) {}

    public record ContextDto(
            String activeTenantId,
            List<TenantMembershipDto> memberships,
            List<TeamCompetitionDto> teamCompetitions) {}

    public record TenantMembershipDto(
            String tenantId,
            String tenantCode,
            String tenantName,
            String tenantType,
            String membershipType) {}

    public record TeamCompetitionDto(
            String teamId,
            String teamCode,
            String teamName,
            String memberRole,
            String competitionId,
            String competitionCode,
            String competitionName,
            String competitionScope,
            String academicCycleCode,
            String originTenantId) {}
}
