package com.solveria.iamservice.application.service;

import com.solveria.core.iam.domain.model.User;
import com.solveria.iamservice.api.rest.dto.AuthResponse;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipType;
import com.solveria.iamservice.multitenancy.persistence.entity.TeamMemberJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.UserTenantMembershipJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.repository.TeamMemberJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.UserTenantMembershipJpaRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserContextService {

    private final UserTenantMembershipJpaRepository userTenantMembershipJpaRepository;
    private final TeamMemberJpaRepository teamMemberJpaRepository;

    public UserContextService(
            UserTenantMembershipJpaRepository userTenantMembershipJpaRepository,
            TeamMemberJpaRepository teamMemberJpaRepository) {
        this.userTenantMembershipJpaRepository = userTenantMembershipJpaRepository;
        this.teamMemberJpaRepository = teamMemberJpaRepository;
    }

    @Transactional(readOnly = true)
    public UserSessionContext buildContext(User user) {
        List<UserTenantMembershipJpaEntity> memberships =
                userTenantMembershipJpaRepository.findByUserIdAndStatusOrderByCreatedAtAsc(
                        user.getId(), MembershipStatus.ACTIVE);

        List<AuthResponse.TenantMembershipDto> membershipDtos;
        String primaryTenantId;
        String activeTenantId;

        if (memberships.isEmpty()) {
            String legacyTenantId = user.getTenantId();
            membershipDtos =
                    StringUtils.hasText(legacyTenantId)
                            ? List.of(
                                    new AuthResponse.TenantMembershipDto(
                                            legacyTenantId,
                                            legacyTenantId,
                                            legacyTenantId,
                                            "LEGACY",
                                            MembershipType.PRIMARY.name()))
                            : List.of();
            primaryTenantId = legacyTenantId;
            activeTenantId = legacyTenantId;
        } else {
            membershipDtos =
                    memberships.stream()
                            .map(
                                    membership ->
                                            new AuthResponse.TenantMembershipDto(
                                                    membership.getTenant().getId().toString(),
                                                    membership.getTenant().getCode(),
                                                    membership.getTenant().getName(),
                                                    membership.getTenant().getType().name(),
                                                    membership.getMembershipType().name()))
                            .toList();
            primaryTenantId =
                    memberships.stream()
                            .filter(
                                    membership ->
                                            membership.getMembershipType()
                                                    == MembershipType.PRIMARY)
                            .findFirst()
                            .orElse(memberships.getFirst())
                            .getTenant()
                            .getId()
                            .toString();
            activeTenantId = primaryTenantId;
        }

        List<AuthResponse.TeamCompetitionDto> teamCompetitionDtos =
                teamMemberJpaRepository
                        .findByUserIdAndStatus(user.getId(), MembershipStatus.ACTIVE)
                        .stream()
                        .map(this::toTeamCompetition)
                        .toList();

        Set<String> roles = new LinkedHashSet<>();
        roles.add(user.getUserCategory());

        return new UserSessionContext(
                primaryTenantId, activeTenantId, roles, membershipDtos, teamCompetitionDtos);
    }

    private AuthResponse.TeamCompetitionDto toTeamCompetition(TeamMemberJpaEntity teamMember) {
        String cycleCode =
                teamMember.getTeam().getCompetition().getAcademicCycle() != null
                        ? teamMember.getTeam().getCompetition().getAcademicCycle().getCode()
                        : null;

        return new AuthResponse.TeamCompetitionDto(
                teamMember.getTeam().getId().toString(),
                teamMember.getTeam().getCode(),
                teamMember.getTeam().getName(),
                teamMember.getMemberRole().name(),
                teamMember.getTeam().getCompetition().getId().toString(),
                teamMember.getTeam().getCompetition().getCode(),
                teamMember.getTeam().getCompetition().getName(),
                teamMember.getTeam().getCompetition().getScope().name(),
                cycleCode,
                teamMember.getTeam().getOriginTenant().getId().toString());
    }
}
