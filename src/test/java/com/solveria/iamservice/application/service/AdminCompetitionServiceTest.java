package com.solveria.iamservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionUpdateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.AcademicCycleJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionRoleAssignmentMethod;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionScope;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TeamCreationMode;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.repository.AcademicCycleJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
    @Mock private AcademicCycleJpaRepository academicCycleJpaRepository;

    @Test
    void createCompetition_NormalizesCodeAndReturnsResponse() {
        AdminCompetitionService service =
                new AdminCompetitionService(
                        competitionJpaRepository, tenantJpaRepository, academicCycleJpaRepository);

        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");
        AcademicCycleJpaEntity cycle =
                academicCycle("2026-S1", hostTenant, "2026-02-01", "2026-06-30");

        when(competitionJpaRepository.existsByCodeIgnoreCase("biz-sim-2026")).thenReturn(false);
        when(tenantJpaRepository.findById(hostTenant.getId())).thenReturn(Optional.of(hostTenant));
        when(academicCycleJpaRepository.findAllByOwnerTenantIdOrderByStartDateAsc(
                        hostTenant.getId()))
                .thenReturn(List.of(cycle));
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
    void createCompetition_AssignsAcademicCycleWhenWindowFitsHostCycle() {
        AdminCompetitionService service =
                new AdminCompetitionService(
                        competitionJpaRepository, tenantJpaRepository, academicCycleJpaRepository);

        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");
        AcademicCycleJpaEntity cycle =
                academicCycle("2026-S1", hostTenant, "2026-02-01", "2026-06-30");
        CompetitionJpaEntity[] savedCompetition = new CompetitionJpaEntity[1];

        when(competitionJpaRepository.existsByCodeIgnoreCase("biz-sim-2026")).thenReturn(false);
        when(tenantJpaRepository.findById(hostTenant.getId())).thenReturn(Optional.of(hostTenant));
        when(academicCycleJpaRepository.findAllByOwnerTenantIdOrderByStartDateAsc(
                        hostTenant.getId()))
                .thenReturn(List.of(cycle));
        when(competitionJpaRepository.save(any(CompetitionJpaEntity.class)))
                .thenAnswer(
                        invocation -> {
                            CompetitionJpaEntity entity = invocation.getArgument(0);
                            savedCompetition[0] = entity;
                            ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
                            ReflectionTestUtils.setField(entity, "status", CompetitionStatus.DRAFT);
                            ReflectionTestUtils.setField(
                                    entity, "teamCreationMode", TeamCreationMode.ADMIN_MANAGED);
                            ReflectionTestUtils.setField(entity, "createdAt", Instant.now());
                            return entity;
                        });

        service.createCompetition(
                new AdminCompetitionCreateRequest(
                        "biz-sim-2026",
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

        assertEquals(cycle, savedCompetition[0].getAcademicCycle());
    }

    @Test
    void listCompetitions_FiltersByStatusHostTenantAndCycle() {
        AdminCompetitionService service =
                new AdminCompetitionService(
                        competitionJpaRepository, tenantJpaRepository, academicCycleJpaRepository);

        TenantJpaEntity hostTenantA =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");
        TenantJpaEntity hostTenantB =
                tenant(
                        "22222222-2222-2222-2222-222222222222",
                        "ucb",
                        "Universidad Catolica Boliviana");

        AcademicCycleJpaEntity cycleA =
                academicCycle("2026-S1", hostTenantA, "2026-02-01", "2026-06-30");
        AcademicCycleJpaEntity cycleB =
                academicCycle("2026-S2", hostTenantB, "2026-07-01", "2026-11-30");

        CompetitionJpaEntity matchingCompetition =
                competition(
                        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        "biz-sim-2026",
                        "Business Simulation 2026",
                        hostTenantA,
                        cycleA,
                        CompetitionStatus.ACTIVE,
                        "2026-03-12T20:00:00Z");
        CompetitionJpaEntity archivedCompetition =
                competition(
                        "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                        "archived-sim",
                        "Archived Competition",
                        hostTenantA,
                        cycleA,
                        CompetitionStatus.ARCHIVED,
                        "2026-03-13T20:00:00Z");
        CompetitionJpaEntity differentCycleCompetition =
                competition(
                        "cccccccc-cccc-cccc-cccc-cccccccccccc",
                        "biz-sim-2026-s2",
                        "Business Simulation 2026 S2",
                        hostTenantB,
                        cycleB,
                        CompetitionStatus.ACTIVE,
                        "2026-03-14T20:00:00Z");

        when(competitionJpaRepository.findAllByOrderByCreatedAtAsc())
                .thenReturn(
                        List.of(
                                matchingCompetition,
                                archivedCompetition,
                                differentCycleCompetition));

        var response =
                service.listCompetitions("ACTIVE", hostTenantA.getId().toString(), "2026-s1");

        assertEquals(1, response.size());
        assertEquals("biz-sim-2026", response.getFirst().code());
    }

    @Test
    void updateCompetition_WithInvalidSchedule_ThrowsConflict() {
        AdminCompetitionService service =
                new AdminCompetitionService(
                        competitionJpaRepository, tenantJpaRepository, academicCycleJpaRepository);

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

    private CompetitionJpaEntity competition(
            String id,
            String code,
            String name,
            TenantJpaEntity hostTenant,
            AcademicCycleJpaEntity cycle,
            CompetitionStatus status,
            String createdAt) {
        CompetitionJpaEntity competition =
                new CompetitionJpaEntity(
                        code,
                        name,
                        null,
                        CompetitionScope.CROSS_TENANT,
                        hostTenant,
                        cycle,
                        "Smart Retail",
                        "retail-tech",
                        "Retail Technology",
                        new BigDecimal("100000.00"),
                        (short) 4,
                        (short) 6,
                        CompetitionRoleAssignmentMethod.ADMIN_ASSIGNMENT,
                        true);
        ReflectionTestUtils.setField(competition, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(competition, "status", status);
        ReflectionTestUtils.setField(competition, "currency", "USD");
        ReflectionTestUtils.setField(
                competition, "teamCreationMode", TeamCreationMode.ADMIN_MANAGED);
        ReflectionTestUtils.setField(
                competition, "startsAt", Instant.parse("2026-04-01T00:00:00Z"));
        ReflectionTestUtils.setField(competition, "endsAt", Instant.parse("2026-06-01T00:00:00Z"));
        ReflectionTestUtils.setField(competition, "createdAt", Instant.parse(createdAt));
        return competition;
    }

    private AcademicCycleJpaEntity academicCycle(
            String code, TenantJpaEntity ownerTenant, String startDate, String endDate) {
        AcademicCycleJpaEntity academicCycle =
                new AcademicCycleJpaEntity(
                        code,
                        "Semestre " + code,
                        ownerTenant,
                        LocalDate.parse(startDate),
                        LocalDate.parse(endDate));
        ReflectionTestUtils.setField(academicCycle, "id", UUID.randomUUID());
        return academicCycle;
    }

    private TenantJpaEntity tenant(String id, String code, String name) {
        TenantJpaEntity tenant = new TenantJpaEntity(code, name, TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(tenant, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(tenant, "status", TenantStatus.ACTIVE);
        return tenant;
    }
}
