package com.solveria.iamservice.application.service;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.core.shared.exceptions.EntityNotFoundException;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionResponse;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionUpdateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.AcademicCycleJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionRoleAssignmentMethod;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionScope;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TeamCreationMode;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.repository.AcademicCycleJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminCompetitionService {

    private static final short MVP_MIN_TEAM_SIZE = 4;
    private static final short MVP_MAX_TEAM_SIZE = 6;

    private final CompetitionJpaRepository competitionJpaRepository;
    private final TenantJpaRepository tenantJpaRepository;
    private final AcademicCycleJpaRepository academicCycleJpaRepository;

    public AdminCompetitionService(
            CompetitionJpaRepository competitionJpaRepository,
            TenantJpaRepository tenantJpaRepository,
            AcademicCycleJpaRepository academicCycleJpaRepository) {
        this.competitionJpaRepository = competitionJpaRepository;
        this.tenantJpaRepository = tenantJpaRepository;
        this.academicCycleJpaRepository = academicCycleJpaRepository;
    }

    @Transactional
    public AdminCompetitionResponse createCompetition(AdminCompetitionCreateRequest request) {
        String normalizedCode = normalizeCode(request.code());
        validateUniqueCode(normalizedCode, null);

        TenantJpaEntity hostTenant = getActiveTenant(request.hostTenantId());
        validateCompetitionWindow(request.startsAt(), request.endsAt());
        validateMvpTeamSize(request.minTeamSize(), request.maxTeamSize());
        AcademicCycleJpaEntity academicCycle =
                resolveAcademicCycle(hostTenant, request.startsAt(), request.endsAt());

        CompetitionJpaEntity competition =
                new CompetitionJpaEntity(
                        normalizedCode,
                        request.name().trim(),
                        normalizeDescription(request.description()),
                        parseScope(request.scope()),
                        hostTenant,
                        academicCycle,
                        request.productName().trim(),
                        normalizeCode(request.industryCode()),
                        request.industryName().trim(),
                        request.initialCapitalAmount(),
                        MVP_MIN_TEAM_SIZE,
                        MVP_MAX_TEAM_SIZE,
                        parseRoleAssignmentMethod(request.roleAssignmentMethod()),
                        request.allowOptionalCoo());
        competition.setCurrency("USD");
        competition.setTeamCreationMode(TeamCreationMode.ADMIN_MANAGED);
        competition.setStartsAt(request.startsAt());
        competition.setEndsAt(request.endsAt());

        return toResponse(competitionJpaRepository.save(competition));
    }

    @Transactional(readOnly = true)
    public List<AdminCompetitionResponse> listCompetitions(
            String status, String hostTenantId, String cycle) {
        CompetitionStatus requestedStatus = parseOptionalStatus(status);
        UUID requestedHostTenantId = parseOptionalUuid(hostTenantId, "tenant.reference.invalid");
        String requestedCycle = normalizeOptionalCycle(cycle);
        return competitionJpaRepository.findAllByOrderByCreatedAtAsc().stream()
                .filter(
                        competition ->
                                requestedStatus == null
                                        || competition.getStatus() == requestedStatus)
                .filter(
                        competition ->
                                requestedHostTenantId == null
                                        || competition
                                                .getOwnerTenant()
                                                .getId()
                                                .equals(requestedHostTenantId))
                .filter(competition -> matchesCycle(competition, requestedCycle))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminCompetitionResponse getCompetition(String id) {
        return toResponse(getCompetitionEntity(id));
    }

    @Transactional
    public AdminCompetitionResponse updateCompetition(
            String id, AdminCompetitionUpdateRequest request) {
        CompetitionJpaEntity competition = getCompetitionEntity(id);
        String normalizedCode = normalizeCode(request.code());
        validateUniqueCode(normalizedCode, competition.getId());

        TenantJpaEntity hostTenant = getActiveTenant(request.hostTenantId());
        validateCompetitionWindow(request.startsAt(), request.endsAt());
        validateMvpTeamSize(request.minTeamSize(), request.maxTeamSize());
        AcademicCycleJpaEntity academicCycle =
                resolveAcademicCycle(hostTenant, request.startsAt(), request.endsAt());

        competition.setCode(normalizedCode);
        competition.setName(request.name().trim());
        competition.setDescription(normalizeDescription(request.description()));
        competition.setScope(parseScope(request.scope()));
        competition.setOwnerTenant(hostTenant);
        competition.setAcademicCycle(academicCycle);
        competition.setProductName(request.productName().trim());
        competition.setIndustryCode(normalizeCode(request.industryCode()));
        competition.setIndustryName(request.industryName().trim());
        competition.setInitialCapitalAmount(request.initialCapitalAmount());
        competition.setMinTeamSize(MVP_MIN_TEAM_SIZE);
        competition.setMaxTeamSize(MVP_MAX_TEAM_SIZE);
        competition.setTeamCreationMode(TeamCreationMode.ADMIN_MANAGED);
        competition.setRoleAssignmentMethod(
                parseRoleAssignmentMethod(request.roleAssignmentMethod()));
        competition.setAllowOptionalCoo(request.allowOptionalCoo());
        competition.setStartsAt(request.startsAt());
        competition.setEndsAt(request.endsAt());

        if (StringUtils.hasText(request.status())) {
            competition.setStatus(parseStatus(request.status()));
        }

        return toResponse(competitionJpaRepository.save(competition));
    }

    @Transactional
    public void archiveCompetition(String id) {
        CompetitionJpaEntity competition = getCompetitionEntity(id);
        if (competition.getStatus() == CompetitionStatus.ARCHIVED) {
            return;
        }
        competition.setStatus(CompetitionStatus.ARCHIVED);
        competitionJpaRepository.save(competition);
    }

    private CompetitionJpaEntity getCompetitionEntity(String id) {
        UUID competitionId = parseUuid(id, "competition.reference.invalid");
        return competitionJpaRepository
                .findById(competitionId)
                .orElseThrow(() -> new EntityNotFoundException("Competition", id));
    }

    private TenantJpaEntity getActiveTenant(String tenantId) {
        UUID parsedTenantId = parseUuid(tenantId, "tenant.reference.invalid");
        TenantJpaEntity tenant =
                tenantJpaRepository
                        .findById(parsedTenantId)
                        .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new BusinessRuleViolationException("competition.host.tenant.inactive");
        }
        return tenant;
    }

    private void validateUniqueCode(String normalizedCode, UUID currentId) {
        boolean exists =
                currentId == null
                        ? competitionJpaRepository.existsByCodeIgnoreCase(normalizedCode)
                        : competitionJpaRepository.existsByCodeIgnoreCaseAndIdNot(
                                normalizedCode, currentId);
        if (exists) {
            throw new BusinessRuleViolationException("competition.code.already.taken");
        }
    }

    private void validateCompetitionWindow(java.time.Instant startsAt, java.time.Instant endsAt) {
        if (startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw new BusinessRuleViolationException("competition.schedule.invalid");
        }
    }

    private void validateMvpTeamSize(Integer minTeamSize, Integer maxTeamSize) {
        if (minTeamSize == null
                || maxTeamSize == null
                || minTeamSize != MVP_MIN_TEAM_SIZE
                || maxTeamSize != MVP_MAX_TEAM_SIZE) {
            throw new BusinessRuleViolationException("competition.team.size.fixed.for.mvp");
        }
    }

    private CompetitionScope parseScope(String scope) {
        try {
            return CompetitionScope.valueOf(scope.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("competition.scope.invalid");
        }
    }

    private CompetitionRoleAssignmentMethod parseRoleAssignmentMethod(String method) {
        try {
            return CompetitionRoleAssignmentMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("competition.role.assignment.method.invalid");
        }
    }

    private CompetitionStatus parseOptionalStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parseStatus(status);
    }

    private UUID parseOptionalUuid(String value, String errorCode) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseUuid(value, errorCode);
    }

    private CompetitionStatus parseStatus(String status) {
        try {
            return CompetitionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("competition.status.invalid");
        }
    }

    private String normalizeOptionalCycle(String cycle) {
        if (!StringUtils.hasText(cycle)) {
            return null;
        }
        return cycle.trim().toUpperCase(Locale.ROOT);
    }

    private UUID parseUuid(String value, String errorCode) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(errorCode);
        }
    }

    private String normalizeCode(String code) {
        String normalized =
                code.trim()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9-]+", "-")
                        .replaceAll("^-+", "")
                        .replaceAll("-+$", "");
        if (normalized.isBlank()) {
            throw new BusinessRuleViolationException("competition.code.invalid");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }

    private AcademicCycleJpaEntity resolveAcademicCycle(
            TenantJpaEntity hostTenant, java.time.Instant startsAt, java.time.Instant endsAt) {
        if (startsAt == null || endsAt == null) {
            return null;
        }

        LocalDate startsOn = startsAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endsOn = endsAt.atZone(ZoneOffset.UTC).toLocalDate();

        return academicCycleJpaRepository
                .findAllByOwnerTenantIdOrderByStartDateAsc(hostTenant.getId())
                .stream()
                .filter(cycle -> !cycle.getStartDate().isAfter(startsOn))
                .filter(cycle -> !cycle.getEndDate().isBefore(endsOn))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesCycle(CompetitionJpaEntity competition, String requestedCycle) {
        if (requestedCycle == null) {
            return true;
        }
        AcademicCycleJpaEntity academicCycle = competition.getAcademicCycle();
        return academicCycle != null
                && requestedCycle.equals(academicCycle.getCode().toUpperCase(Locale.ROOT));
    }

    private AdminCompetitionResponse toResponse(CompetitionJpaEntity competition) {
        TenantJpaEntity hostTenant = competition.getOwnerTenant();
        return new AdminCompetitionResponse(
                competition.getId().toString(),
                competition.getCode(),
                competition.getName(),
                competition.getDescription(),
                competition.getScope().name(),
                competition.getStatus().name(),
                hostTenant.getId().toString(),
                hostTenant.getCode(),
                hostTenant.getName(),
                competition.getProductName(),
                competition.getIndustryCode(),
                competition.getIndustryName(),
                competition.getInitialCapitalAmount(),
                competition.getCurrency(),
                competition.getMinTeamSize(),
                competition.getMaxTeamSize(),
                competition.getTeamCreationMode().name(),
                competition.getRoleAssignmentMethod().name(),
                Boolean.TRUE.equals(competition.getAllowOptionalCoo()),
                competition.getStartsAt(),
                competition.getEndsAt(),
                competition.getCreatedAt());
    }
}
