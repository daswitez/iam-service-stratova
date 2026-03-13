package com.solveria.iamservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionTenantCreateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionRoleAssignmentMethod;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionScope;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionTenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TeamCreationMode;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionTenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminCompetitionTenantServiceTest {

    @Mock private CompetitionJpaRepository competitionJpaRepository;
    @Mock private CompetitionTenantJpaRepository competitionTenantJpaRepository;
    @Mock private TenantJpaRepository tenantJpaRepository;

    @Test
    void addParticipantTenant_WhenValid_ReturnsResponse() {
        AdminCompetitionTenantService service =
                new AdminCompetitionTenantService(
                        competitionJpaRepository,
                        competitionTenantJpaRepository,
                        tenantJpaRepository);

        CompetitionJpaEntity competition =
                competition(
                        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        "biz-sim-2026",
                        "11111111-1111-1111-1111-111111111111");
        TenantJpaEntity tenant =
                tenant(
                        "22222222-2222-2222-2222-222222222222",
                        "ucb",
                        "Universidad Catolica Boliviana");

        when(competitionJpaRepository.findById(competition.getId()))
                .thenReturn(Optional.of(competition));
        when(tenantJpaRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(competitionTenantJpaRepository.existsByCompetition_IdAndTenant_Id(
                        competition.getId(), tenant.getId()))
                .thenReturn(false);
        when(competitionTenantJpaRepository.save(any(CompetitionTenantJpaEntity.class)))
                .thenAnswer(
                        invocation -> {
                            CompetitionTenantJpaEntity entity = invocation.getArgument(0);
                            ReflectionTestUtils.setField(
                                    entity,
                                    "id",
                                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
                            ReflectionTestUtils.setField(
                                    entity, "createdAt", Instant.parse("2026-03-13T00:00:00Z"));
                            return entity;
                        });

        var response =
                service.addParticipantTenant(
                        competition.getId().toString(),
                        new AdminCompetitionTenantCreateRequest(tenant.getId().toString()));

        assertEquals("biz-sim-2026", response.competitionCode());
        assertEquals("ucb", response.tenantCode());
        assertEquals("ACTIVE", response.tenantStatus());
    }

    @Test
    void addParticipantTenant_WhenDuplicated_ThrowsConflict() {
        AdminCompetitionTenantService service =
                new AdminCompetitionTenantService(
                        competitionJpaRepository,
                        competitionTenantJpaRepository,
                        tenantJpaRepository);

        CompetitionJpaEntity competition =
                competition(
                        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        "biz-sim-2026",
                        "11111111-1111-1111-1111-111111111111");
        TenantJpaEntity tenant =
                tenant(
                        "22222222-2222-2222-2222-222222222222",
                        "ucb",
                        "Universidad Catolica Boliviana");

        when(competitionJpaRepository.findById(competition.getId()))
                .thenReturn(Optional.of(competition));
        when(tenantJpaRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));
        when(competitionTenantJpaRepository.existsByCompetition_IdAndTenant_Id(
                        competition.getId(), tenant.getId()))
                .thenReturn(true);

        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        service.addParticipantTenant(
                                competition.getId().toString(),
                                new AdminCompetitionTenantCreateRequest(
                                        tenant.getId().toString())));
    }

    private CompetitionJpaEntity competition(String id, String code, String hostTenantId) {
        TenantJpaEntity hostTenant =
                tenant(hostTenantId, "umsa", "Universidad Mayor de San Andres");
        CompetitionJpaEntity competition =
                new CompetitionJpaEntity(
                        code,
                        "Business Simulation 2026",
                        "Competencia interuniversitaria",
                        CompetitionScope.CROSS_TENANT,
                        hostTenant,
                        null,
                        "Smart Retail",
                        "retail-tech",
                        "Retail Technology",
                        new BigDecimal("100000.00"),
                        (short) 4,
                        (short) 6,
                        CompetitionRoleAssignmentMethod.ADMIN_ASSIGNMENT,
                        true);
        ReflectionTestUtils.setField(competition, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(competition, "status", CompetitionStatus.DRAFT);
        ReflectionTestUtils.setField(competition, "currency", "USD");
        ReflectionTestUtils.setField(
                competition, "teamCreationMode", TeamCreationMode.ADMIN_MANAGED);
        return competition;
    }

    private TenantJpaEntity tenant(String id, String code, String name) {
        TenantJpaEntity tenant = new TenantJpaEntity(code, name, TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(tenant, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(tenant, "status", TenantStatus.ACTIVE);
        return tenant;
    }
}
