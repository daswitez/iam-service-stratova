package com.solveria.iamservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solveria.core.iam.infrastructure.persistence.entity.UserJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.repository.UserJpaRepository;
import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.AdminMembershipCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminMembershipUpdateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipType;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.entity.UserTenantMembershipJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.UserTenantMembershipJpaRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminMembershipServiceTest {

    @Mock private UserJpaRepository userJpaRepository;
    @Mock private TenantJpaRepository tenantJpaRepository;
    @Mock private UserTenantMembershipJpaRepository userTenantMembershipJpaRepository;

    @Test
    void createMembership_PrimaryRequest_DemotesPreviousPrimary() {
        AdminMembershipService service =
                new AdminMembershipService(
                        userJpaRepository, tenantJpaRepository, userTenantMembershipJpaRepository);

        Long userId = 10L;
        UserJpaEntity user = mock(UserJpaEntity.class);
        when(user.getId()).thenReturn(userId);
        when(user.getTenantId()).thenReturn("11111111-1111-1111-1111-111111111111");
        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(user));

        TenantJpaEntity currentTenant =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres",
                        TenantType.UNIVERSITY);
        TenantJpaEntity newTenant =
                tenant(
                        "22222222-2222-2222-2222-222222222222",
                        "facultad-ingenieria",
                        "Facultad de Ingenieria",
                        TenantType.FACULTY);
        when(tenantJpaRepository.findById(newTenant.getId())).thenReturn(Optional.of(newTenant));

        UserTenantMembershipJpaEntity currentPrimary =
                membership(
                        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        userId,
                        currentTenant,
                        MembershipType.PRIMARY,
                        MembershipStatus.ACTIVE,
                        Instant.parse("2026-01-01T00:00:00Z"));

        Map<UUID, UserTenantMembershipJpaEntity> memberships = new LinkedHashMap<>();
        memberships.put(currentPrimary.getId(), currentPrimary);

        when(userTenantMembershipJpaRepository.existsByUserIdAndTenant_Id(
                        userId, newTenant.getId()))
                .thenReturn(false);
        when(userTenantMembershipJpaRepository.save(
                        org.mockito.ArgumentMatchers.any(UserTenantMembershipJpaEntity.class)))
                .thenAnswer(
                        invocation -> {
                            UserTenantMembershipJpaEntity entity = invocation.getArgument(0);
                            if (entity.getId() == null) {
                                ReflectionTestUtils.setField(
                                        entity,
                                        "id",
                                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
                            }
                            if (entity.getCreatedAt() == null) {
                                ReflectionTestUtils.setField(
                                        entity, "createdAt", Instant.parse("2026-02-01T00:00:00Z"));
                            }
                            memberships.put(entity.getId(), entity);
                            return entity;
                        });
        when(userTenantMembershipJpaRepository.findById(
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")))
                .thenAnswer(
                        invocation ->
                                Optional.ofNullable(
                                        memberships.get(invocation.getArgument(0, UUID.class))));
        when(userTenantMembershipJpaRepository.findByUserIdAndStatusOrderByCreatedAtAsc(
                        userId, MembershipStatus.ACTIVE))
                .thenAnswer(
                        invocation ->
                                memberships.values().stream()
                                        .filter(m -> m.getUserId().equals(userId))
                                        .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                                        .sorted(
                                                Comparator.comparing(
                                                        UserTenantMembershipJpaEntity
                                                                ::getCreatedAt))
                                        .toList());
        when(userTenantMembershipJpaRepository
                        .findFirstByUserIdAndMembershipTypeAndStatusOrderByCreatedAtAsc(
                                userId, MembershipType.PRIMARY, MembershipStatus.ACTIVE))
                .thenAnswer(
                        invocation ->
                                memberships.values().stream()
                                        .filter(m -> m.getUserId().equals(userId))
                                        .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                                        .filter(
                                                m ->
                                                        m.getMembershipType()
                                                                == MembershipType.PRIMARY)
                                        .sorted(
                                                Comparator.comparing(
                                                        UserTenantMembershipJpaEntity
                                                                ::getCreatedAt))
                                        .findFirst());

        var response =
                service.createMembership(
                        new AdminMembershipCreateRequest(
                                userId, newTenant.getId().toString(), "ACTIVE", true));

        assertEquals("PRIMARY", response.membershipType());
        assertEquals(newTenant.getId().toString(), response.tenantId());
        assertEquals(MembershipType.SECONDARY, currentPrimary.getMembershipType());
        verify(user).setTenantId(newTenant.getId().toString());
    }

    @Test
    void updateMembership_PrimaryToLeft_PromotesOldestActiveSecondary() {
        AdminMembershipService service =
                new AdminMembershipService(
                        userJpaRepository, tenantJpaRepository, userTenantMembershipJpaRepository);

        Long userId = 15L;
        UserJpaEntity user = mock(UserJpaEntity.class);
        when(user.getId()).thenReturn(userId);
        when(user.getTenantId()).thenReturn("11111111-1111-1111-1111-111111111111");
        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(user));

        TenantJpaEntity tenantA =
                tenant(
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres",
                        TenantType.UNIVERSITY);
        TenantJpaEntity tenantB =
                tenant(
                        "33333333-3333-3333-3333-333333333333",
                        "adm-sis",
                        "Administracion de Sistemas",
                        TenantType.PROGRAM);

        UserTenantMembershipJpaEntity primary =
                membership(
                        "cccccccc-cccc-cccc-cccc-cccccccccccc",
                        userId,
                        tenantA,
                        MembershipType.PRIMARY,
                        MembershipStatus.ACTIVE,
                        Instant.parse("2026-01-01T00:00:00Z"));
        UserTenantMembershipJpaEntity secondary =
                membership(
                        "dddddddd-dddd-dddd-dddd-dddddddddddd",
                        userId,
                        tenantB,
                        MembershipType.SECONDARY,
                        MembershipStatus.ACTIVE,
                        Instant.parse("2026-01-02T00:00:00Z"));

        Map<UUID, UserTenantMembershipJpaEntity> memberships = new LinkedHashMap<>();
        memberships.put(primary.getId(), primary);
        memberships.put(secondary.getId(), secondary);

        when(userTenantMembershipJpaRepository.findById(primary.getId()))
                .thenReturn(Optional.of(primary));
        when(userTenantMembershipJpaRepository.save(
                        org.mockito.ArgumentMatchers.any(UserTenantMembershipJpaEntity.class)))
                .thenAnswer(
                        invocation -> {
                            UserTenantMembershipJpaEntity entity = invocation.getArgument(0);
                            memberships.put(entity.getId(), entity);
                            return entity;
                        });
        when(userTenantMembershipJpaRepository.findByUserIdAndStatusOrderByCreatedAtAsc(
                        userId, MembershipStatus.ACTIVE))
                .thenAnswer(
                        invocation ->
                                memberships.values().stream()
                                        .filter(m -> m.getUserId().equals(userId))
                                        .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                                        .sorted(
                                                Comparator.comparing(
                                                        UserTenantMembershipJpaEntity
                                                                ::getCreatedAt))
                                        .toList());
        when(userTenantMembershipJpaRepository
                        .findFirstByUserIdAndMembershipTypeAndStatusOrderByCreatedAtAsc(
                                userId, MembershipType.PRIMARY, MembershipStatus.ACTIVE))
                .thenAnswer(
                        invocation ->
                                memberships.values().stream()
                                        .filter(m -> m.getUserId().equals(userId))
                                        .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                                        .filter(
                                                m ->
                                                        m.getMembershipType()
                                                                == MembershipType.PRIMARY)
                                        .sorted(
                                                Comparator.comparing(
                                                        UserTenantMembershipJpaEntity
                                                                ::getCreatedAt))
                                        .findFirst());

        var response =
                service.updateMembership(
                        primary.getId().toString(), new AdminMembershipUpdateRequest("LEFT", null));

        assertEquals("LEFT", response.status());
        assertEquals("SECONDARY", response.membershipType());
        assertEquals(MembershipType.SECONDARY, primary.getMembershipType());
        assertEquals(MembershipType.PRIMARY, secondary.getMembershipType());
        verify(user).setTenantId(tenantB.getId().toString());
    }

    @Test
    void createMembership_DuplicateUserAndTenant_ThrowsConflict() {
        AdminMembershipService service =
                new AdminMembershipService(
                        userJpaRepository, tenantJpaRepository, userTenantMembershipJpaRepository);

        Long userId = 20L;
        String tenantId = "44444444-4444-4444-4444-444444444444";
        UserJpaEntity user = mock(UserJpaEntity.class);
        when(user.getId()).thenReturn(userId);
        when(userJpaRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tenantJpaRepository.findById(UUID.fromString(tenantId)))
                .thenReturn(
                        Optional.of(
                                tenant(
                                        tenantId,
                                        "tenant-dup",
                                        "Tenant Duplicado",
                                        TenantType.UNIVERSITY)));
        when(userTenantMembershipJpaRepository.existsByUserIdAndTenant_Id(
                        userId, UUID.fromString(tenantId)))
                .thenReturn(true);

        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        service.createMembership(
                                new AdminMembershipCreateRequest(
                                        userId, tenantId, "ACTIVE", false)));
    }

    private TenantJpaEntity tenant(String id, String code, String name, TenantType type) {
        TenantJpaEntity tenant = new TenantJpaEntity(code, name, type, null);
        ReflectionTestUtils.setField(tenant, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(tenant, "status", TenantStatus.ACTIVE);
        ReflectionTestUtils.setField(tenant, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        return tenant;
    }

    private UserTenantMembershipJpaEntity membership(
            String id,
            Long userId,
            TenantJpaEntity tenant,
            MembershipType membershipType,
            MembershipStatus status,
            Instant createdAt) {
        UserTenantMembershipJpaEntity membership =
                new UserTenantMembershipJpaEntity(userId, tenant, membershipType);
        ReflectionTestUtils.setField(membership, "id", UUID.fromString(id));
        ReflectionTestUtils.setField(membership, "status", status);
        ReflectionTestUtils.setField(membership, "createdAt", createdAt);
        return membership;
    }
}
