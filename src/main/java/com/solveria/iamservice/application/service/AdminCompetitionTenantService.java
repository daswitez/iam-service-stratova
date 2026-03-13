package com.solveria.iamservice.application.service;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.core.shared.exceptions.EntityNotFoundException;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionTenantCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionTenantResponse;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionTenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionTenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCompetitionTenantService {

    private final CompetitionJpaRepository competitionJpaRepository;
    private final CompetitionTenantJpaRepository competitionTenantJpaRepository;
    private final TenantJpaRepository tenantJpaRepository;

    public AdminCompetitionTenantService(
            CompetitionJpaRepository competitionJpaRepository,
            CompetitionTenantJpaRepository competitionTenantJpaRepository,
            TenantJpaRepository tenantJpaRepository) {
        this.competitionJpaRepository = competitionJpaRepository;
        this.competitionTenantJpaRepository = competitionTenantJpaRepository;
        this.tenantJpaRepository = tenantJpaRepository;
    }

    @Transactional
    public AdminCompetitionTenantResponse addParticipantTenant(
            String competitionId, AdminCompetitionTenantCreateRequest request) {
        CompetitionJpaEntity competition = getCompetition(competitionId);
        TenantJpaEntity tenant = getActiveTenant(request.tenantId());

        if (competitionTenantJpaRepository.existsByCompetition_IdAndTenant_Id(
                competition.getId(), tenant.getId())) {
            throw new BusinessRuleViolationException("competition.tenant.already.exists");
        }

        CompetitionTenantJpaEntity competitionTenant =
                competitionTenantJpaRepository.save(
                        new CompetitionTenantJpaEntity(competition, tenant));

        return toResponse(competitionTenant);
    }

    @Transactional(readOnly = true)
    public List<AdminCompetitionTenantResponse> listParticipantTenants(String competitionId) {
        CompetitionJpaEntity competition = getCompetition(competitionId);
        return competitionTenantJpaRepository
                .findAllByCompetition_IdOrderByCreatedAtAsc(competition.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void removeParticipantTenant(String competitionId, String tenantId) {
        UUID parsedCompetitionId = parseUuid(competitionId, "competition.reference.invalid");
        UUID parsedTenantId = parseUuid(tenantId, "tenant.reference.invalid");

        CompetitionTenantJpaEntity competitionTenant =
                competitionTenantJpaRepository
                        .findByCompetition_IdAndTenant_Id(parsedCompetitionId, parsedTenantId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "CompetitionTenant",
                                                competitionId + ":" + tenantId));

        competitionTenantJpaRepository.delete(competitionTenant);
    }

    private CompetitionJpaEntity getCompetition(String competitionId) {
        UUID parsedCompetitionId = parseUuid(competitionId, "competition.reference.invalid");
        return competitionJpaRepository
                .findById(parsedCompetitionId)
                .orElseThrow(() -> new EntityNotFoundException("Competition", competitionId));
    }

    private TenantJpaEntity getActiveTenant(String tenantId) {
        UUID parsedTenantId = parseUuid(tenantId, "tenant.reference.invalid");
        TenantJpaEntity tenant =
                tenantJpaRepository
                        .findById(parsedTenantId)
                        .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new BusinessRuleViolationException("competition.tenant.inactive");
        }
        return tenant;
    }

    private UUID parseUuid(String value, String errorCode) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(errorCode);
        }
    }

    private AdminCompetitionTenantResponse toResponse(
            CompetitionTenantJpaEntity competitionTenant) {
        CompetitionJpaEntity competition = competitionTenant.getCompetition();
        TenantJpaEntity tenant = competitionTenant.getTenant();
        return new AdminCompetitionTenantResponse(
                competitionTenant.getId().toString(),
                competition.getId().toString(),
                competition.getCode(),
                tenant.getId().toString(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getType().name(),
                tenant.getStatus().name(),
                competitionTenant.getCreatedAt());
    }
}
