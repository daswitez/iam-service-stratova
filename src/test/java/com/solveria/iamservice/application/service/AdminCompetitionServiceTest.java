package com.solveria.iamservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionUpdateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionRoleAssignmentMethod;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionScope;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TeamCreationMode;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionJpaRepository;
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
class AdminCompetitionServiceTest {

    @Mock private CompetitionJpaRepository competitionJpaRepository;
    @Mock private TenantJpaRepository tenantJpaRepository;

    @Test
    void createCompetition_NormalizesCodeAndReturnsResponse() {
        AdminCompetitionService service =
                new AdminCompetitionService(competitionJpaRepository, tenantJpaRepository);

        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");

        when(competitionJpaRepository.existsByCodeIgnoreCase("biz-sim-2026")).thenReturn(false);
        when(tenantJpaRepository.findById(hostTenant.getId())).thenReturn(Optional.of(hostTenant));
        when(competitionJpaRepository.save(any(CompetitionJpaEntity.class)))
                .thenAnswer(
                        invocation -> {
                            CompetitionJpaEntity entity = invocation.getArgument(0);
                            ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
                            ReflectionTestUtils.setField(entity, "status", CompetitionStatus.DRAFT);
                            ReflectionTestUtils.setField(
                                    entity, "teamCreationMode", TeamCreationMode.ADMIN_MANAGED);
                            ReflectionTestUtils.setField(
                                    entity, "createdAt", Instant.parse("2026-03-12T20:00:00Z"));
                            return entity;
                        });

        var response =
                service.createCompetition(
                        new AdminCompetitionCreateRequest(
                                "BIZ-SIM-2026",
                                "Business Simulation 2026",
                                "Competencia interuniversitaria",
                                "CROSS_TENANT",
                                hostTenant.getId().toString(),
                                "Smart Retail",
                                "Retail-Tech",
                                "Retail Technology",
                                new BigDecimal("100000.00"),
                                4,
                                6,
                                "ADMIN_ASSIGNMENT",
                                true,
                                Instant.parse("2026-04-01T00:00:00Z"),
                                Instant.parse("2026-06-01T00:00:00Z")));

        assertEquals("biz-sim-2026", response.code());
        assertEquals("DRAFT", response.status());
        assertEquals("ADMIN_MANAGED", response.teamCreationMode());
        assertEquals("USD", response.currency());
        assertEquals(4, response.minTeamSize());
        assertEquals(6, response.maxTeamSize());
        assertEquals("Competencia interuniversitaria", response.description());
    }

    @Test
    void updateCompetition_WithInvalidSchedule_ThrowsConflict() {
        AdminCompetitionService service =
                new AdminCompetitionService(competitionJpaRepository, tenantJpaRepository);

        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");

        CompetitionJpaEntity competition =
                new CompetitionJpaEntity(
                        "biz-sim-2026",
                        "Business Simulation 2026",
                        null,
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
        ReflectionTestUtils.setField(
                competition, "id", UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        ReflectionTestUtils.setField(competition, "status", CompetitionStatus.DRAFT);
        ReflectionTestUtils.setField(competition, "currency", "USD");
        ReflectionTestUtils.setField(
                competition, "teamCreationMode", TeamCreationMode.ADMIN_MANAGED);

        when(competitionJpaRepository.findById(competition.getId()))
                .thenReturn(Optional.of(competition));
        when(competitionJpaRepository.existsByCodeIgnoreCaseAndIdNot(
                        "biz-sim-2026", competition.getId()))
                .thenReturn(false);
        when(tenantJpaRepository.findById(hostTenant.getId())).thenReturn(Optional.of(hostTenant));

        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        service.updateCompetition(
                                competition.getId().toString(),
                                new AdminCompetitionUpdateRequest(
                                        "biz-sim-2026",
                                        "Business Simulation 2026",
                                        null,
                                        "CROSS_TENANT",
                                        hostTenant.getId().toString(),
                                        "Smart Retail",
                                        "retail-tech",
                                        "Retail Technology",
                                        new BigDecimal("100000.00"),
                                        4,
                                        6,
                                        "ADMIN_ASSIGNMENT",
                                        true,
                                        "ACTIVE",
                                        Instant.parse("2026-06-01T00:00:00Z"),
                                        Instant.parse("2026-04-01T00:00:00Z"))));
    }

    private TenantJpaEntity tenant(String id, String code, String name) {
        TenantJpaEntity tenant = new TenantJpaEntity(code, name, TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(tenant, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(tenant, "status", TenantStatus.ACTIVE);
        return tenant;
    }
}
