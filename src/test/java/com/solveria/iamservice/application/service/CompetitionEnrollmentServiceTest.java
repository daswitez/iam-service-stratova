package com.solveria.iamservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solveria.core.iam.infrastructure.persistence.entity.UserJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.repository.UserJpaRepository;
import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.CompetitionEnrollmentStatusUpdateRequest;
import com.solveria.iamservice.api.rest.dto.CompetitionStaffEnrollmentCreateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionEnrollmentJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionEnrollmentStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionParticipantType;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionRoleAssignmentMethod;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionScope;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipType;
import com.solveria.iamservice.multitenancy.persistence.entity.TeamCreationMode;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.entity.UserTenantMembershipJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionEnrollmentJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionTenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.UserTenantMembershipJpaRepository;
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
class CompetitionEnrollmentServiceTest {

    @Mock private CompetitionEnrollmentJpaRepository competitionEnrollmentJpaRepository;
    @Mock private CompetitionJpaRepository competitionJpaRepository;
    @Mock private CompetitionTenantJpaRepository competitionTenantJpaRepository;
    @Mock private TenantJpaRepository tenantJpaRepository;
    @Mock private UserJpaRepository userJpaRepository;
    @Mock private UserTenantMembershipJpaRepository userTenantMembershipJpaRepository;

    @Test
    void createStaffEnrollment_WithValidHostMembership_ReturnsResponse() {
        CompetitionEnrollmentService service = service();
        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");
        CompetitionJpaEntity competition =
                competition("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "biz-sim-2026", hostTenant);
        UserJpaEntity user = user(10L, "mentor@solveria.local", "PROFESSOR");
        UserTenantMembershipJpaEntity membership = activeMembership(user.getId(), hostTenant);

        when(competitionJpaRepository.findById(competition.getId()))
                .thenReturn(Optional.of(competition));
        when(userJpaRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tenantJpaRepository.findById(hostTenant.getId())).thenReturn(Optional.of(hostTenant));
        when(userTenantMembershipJpaRepository.findByUserIdAndTenant_Id(
                        user.getId(), hostTenant.getId()))
                .thenReturn(Optional.of(membership));
        when(competitionEnrollmentJpaRepository.existsByCompetition_IdAndUserId(
                        competition.getId(), user.getId()))
                .thenReturn(false);
        when(competitionEnrollmentJpaRepository.save(any(CompetitionEnrollmentJpaEntity.class)))
                .thenAnswer(
                        invocation -> {
                            CompetitionEnrollmentJpaEntity entity = invocation.getArgument(0);
                            ReflectionTestUtils.setField(
                                    entity,
                                    "id",
                                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
                            ReflectionTestUtils.setField(
                                    entity, "status", CompetitionEnrollmentStatus.INVITED);
                            ReflectionTestUtils.setField(
                                    entity, "createdAt", Instant.parse("2026-03-13T23:00:00Z"));
                            return entity;
                        });

        var response =
                service.createStaffEnrollment(
                        competition.getId().toString(),
                        new CompetitionStaffEnrollmentCreateRequest(
                                user.getId(), hostTenant.getId().toString(), "JUDGE"));

        assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", response.id());
        assertEquals("JUDGE", response.participantType());
        assertEquals("INVITED", response.status());
        assertEquals(hostTenant.getId().toString(), response.originTenantId());
    }

    @Test
    void createStaffEnrollment_WithoutActiveMembership_ThrowsConflict() {
        CompetitionEnrollmentService service = service();
        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");
        TenantJpaEntity originTenant =
                tenant(
                        "22222222-2222-2222-2222-222222222222",
                        "ucb",
                        "Universidad Catolica Boliviana");
        CompetitionJpaEntity competition =
                competition("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "biz-sim-2026", hostTenant);
        UserJpaEntity user = user(10L, "mentor@solveria.local", "PROFESSOR");
        UserTenantMembershipJpaEntity membership = activeMembership(user.getId(), originTenant);
        membership.setStatus(MembershipStatus.LEFT);

        when(competitionJpaRepository.findById(competition.getId()))
                .thenReturn(Optional.of(competition));
        when(userJpaRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tenantJpaRepository.findById(originTenant.getId()))
                .thenReturn(Optional.of(originTenant));
        when(userTenantMembershipJpaRepository.findByUserIdAndTenant_Id(
                        user.getId(), originTenant.getId()))
                .thenReturn(Optional.of(membership));

        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        service.createStaffEnrollment(
                                competition.getId().toString(),
                                new CompetitionStaffEnrollmentCreateRequest(
                                        user.getId(), originTenant.getId().toString(), "MENTOR")));
    }

    @Test
    void createStaffEnrollment_WithTenantNotEnabledForCompetition_ThrowsConflict() {
        CompetitionEnrollmentService service = service();
        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");
        TenantJpaEntity originTenant =
                tenant(
                        "22222222-2222-2222-2222-222222222222",
                        "ucb",
                        "Universidad Catolica Boliviana");
        CompetitionJpaEntity competition =
                competition("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "biz-sim-2026", hostTenant);
        UserJpaEntity user = user(10L, "mentor@solveria.local", "PROFESSOR");
        UserTenantMembershipJpaEntity membership = activeMembership(user.getId(), originTenant);

        when(competitionJpaRepository.findById(competition.getId()))
                .thenReturn(Optional.of(competition));
        when(userJpaRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tenantJpaRepository.findById(originTenant.getId()))
                .thenReturn(Optional.of(originTenant));
        when(userTenantMembershipJpaRepository.findByUserIdAndTenant_Id(
                        user.getId(), originTenant.getId()))
                .thenReturn(Optional.of(membership));
        when(competitionTenantJpaRepository.findByCompetition_IdAndTenant_Id(
                        competition.getId(), originTenant.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        service.createStaffEnrollment(
                                competition.getId().toString(),
                                new CompetitionStaffEnrollmentCreateRequest(
                                        user.getId(), originTenant.getId().toString(), "MENTOR")));
    }

    @Test
    void updateEnrollmentStatus_ToApproved_SetsApprovedAt() {
        CompetitionEnrollmentService service = service();
        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");
        CompetitionJpaEntity competition =
                competition("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "biz-sim-2026", hostTenant);
        CompetitionEnrollmentJpaEntity enrollment =
                enrollment(
                        "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                        competition,
                        10L,
                        hostTenant,
                        CompetitionParticipantType.JUDGE,
                        CompetitionEnrollmentStatus.INVITED);

        when(competitionEnrollmentJpaRepository.findByCompetition_IdAndId(
                        competition.getId(), enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(competitionEnrollmentJpaRepository.save(enrollment)).thenReturn(enrollment);

        var response =
                service.updateEnrollmentStatus(
                        competition.getId().toString(),
                        enrollment.getId().toString(),
                        new CompetitionEnrollmentStatusUpdateRequest("APPROVED"));

        assertEquals("APPROVED", response.status());
        assertNotNull(response.approvedAt());
    }

    @Test
    void withdrawEnrollment_SetsWithdrawnStatus() {
        CompetitionEnrollmentService service = service();
        TenantJpaEntity hostTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres");
        CompetitionJpaEntity competition =
                competition("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "biz-sim-2026", hostTenant);
        CompetitionEnrollmentJpaEntity enrollment =
                enrollment(
                        "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                        competition,
                        10L,
                        hostTenant,
                        CompetitionParticipantType.JUDGE,
                        CompetitionEnrollmentStatus.INVITED);

        when(competitionEnrollmentJpaRepository.findByCompetition_IdAndId(
                        competition.getId(), enrollment.getId()))
                .thenReturn(Optional.of(enrollment));

        service.withdrawEnrollment(competition.getId().toString(), enrollment.getId().toString());

        assertEquals(CompetitionEnrollmentStatus.WITHDRAWN, enrollment.getStatus());
        verify(competitionEnrollmentJpaRepository).save(enrollment);
    }

    private CompetitionEnrollmentService service() {
        return new CompetitionEnrollmentService(
                competitionEnrollmentJpaRepository,
                competitionJpaRepository,
                competitionTenantJpaRepository,
                tenantJpaRepository,
                userJpaRepository,
                userTenantMembershipJpaRepository);
    }

    private CompetitionJpaEntity competition(String id, String code, TenantJpaEntity hostTenant) {
        CompetitionJpaEntity competition =
                new CompetitionJpaEntity(
                        code,
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
        ReflectionTestUtils.setField(competition, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(competition, "status", CompetitionStatus.DRAFT);
        ReflectionTestUtils.setField(competition, "currency", "USD");
        ReflectionTestUtils.setField(
                competition, "teamCreationMode", TeamCreationMode.ADMIN_MANAGED);
        return competition;
    }

    private CompetitionEnrollmentJpaEntity enrollment(
            String id,
            CompetitionJpaEntity competition,
            Long userId,
            TenantJpaEntity originTenant,
            CompetitionParticipantType participantType,
            CompetitionEnrollmentStatus status) {
        CompetitionEnrollmentJpaEntity enrollment =
                new CompetitionEnrollmentJpaEntity(
                        competition, userId, originTenant, participantType);
        ReflectionTestUtils.setField(enrollment, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(enrollment, "status", status);
        ReflectionTestUtils.setField(
                enrollment, "createdAt", Instant.parse("2026-03-13T23:00:00Z"));
        return enrollment;
    }

    private TenantJpaEntity tenant(String id, String code, String name) {
        TenantJpaEntity tenant = new TenantJpaEntity(code, name, TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(tenant, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(tenant, "status", TenantStatus.ACTIVE);
        return tenant;
    }

    private UserJpaEntity user(Long id, String email, String userCategory) {
        UserJpaEntity user =
                new UserJpaEntity(
                        email.split("@")[0], email, "encoded-password", userCategory, true);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserTenantMembershipJpaEntity activeMembership(Long userId, TenantJpaEntity tenant) {
        UserTenantMembershipJpaEntity membership =
                new UserTenantMembershipJpaEntity(userId, tenant, MembershipType.SECONDARY);
        ReflectionTestUtils.setField(membership, "status", MembershipStatus.ACTIVE);
        ReflectionTestUtils.setField(membership, "id", UUID.randomUUID());
        return membership;
    }
}
