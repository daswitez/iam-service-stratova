package com.solveria.iamservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solveria.core.iam.infrastructure.persistence.entity.RoleJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.entity.UserJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.repository.RoleJpaRepository;
import com.solveria.core.iam.infrastructure.persistence.repository.UserJpaRepository;
import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.AdminUserCreateRequest;
import com.solveria.iamservice.config.security.SecurityConstants;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.UserTenantMembershipJpaRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserJpaRepository userJpaRepository;
    @Mock private RoleJpaRepository roleJpaRepository;
    @Mock private TenantJpaRepository tenantJpaRepository;
    @Mock private UserTenantMembershipJpaRepository userTenantMembershipJpaRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void createUser_WithPrimaryTenantAndPlatformRole_CreatesMembership() {
        AdminUserService service =
                new AdminUserService(
                        userJpaRepository,
                        roleJpaRepository,
                        tenantJpaRepository,
                        userTenantMembershipJpaRepository,
                        passwordEncoder);

        UUID tenantId = UUID.randomUUID();
        TenantJpaEntity tenant = new TenantJpaEntity("umsa", "UMSA", TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(tenant, "id", tenantId);

        RoleJpaEntity platformAdminRole =
                new RoleJpaEntity(SecurityConstants.PLATFORM_ADMIN_ROLE, "Platform admin");
        platformAdminRole.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);

        when(userJpaRepository.existsByEmailIgnoreCase("admin2@solveria.local")).thenReturn(false);
        when(userJpaRepository.existsByUsernameIgnoreCase("platform.admin.2")).thenReturn(false);
        when(roleJpaRepository.findByNameAndTenantId(
                        SecurityConstants.PLATFORM_ADMIN_ROLE, SecurityConstants.SYSTEM_TENANT_ID))
                .thenReturn(Optional.of(platformAdminRole));
        when(tenantJpaRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(passwordEncoder.encode("Admin12345!")).thenReturn("encoded-password");
        when(userJpaRepository.save(any(UserJpaEntity.class)))
                .thenAnswer(
                        invocation -> {
                            UserJpaEntity entity = invocation.getArgument(0);
                            ReflectionTestUtils.setField(entity, "id", 12L);
                            return entity;
                        });
        when(userTenantMembershipJpaRepository
                        .findFirstByUserIdAndMembershipTypeAndStatusOrderByCreatedAtAsc(
                                12L,
                                com.solveria.iamservice.multitenancy.persistence.entity
                                        .MembershipType.PRIMARY,
                                com.solveria.iamservice.multitenancy.persistence.entity
                                        .MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(userTenantMembershipJpaRepository.findByUserIdAndTenant_Id(12L, tenantId))
                .thenReturn(Optional.empty());

        var response =
                service.createUser(
                        new AdminUserCreateRequest(
                                "platform.admin.2",
                                "admin2@solveria.local",
                                "Admin12345!",
                                SecurityConstants.PLATFORM_ADMIN_USER_CATEGORY,
                                tenantId.toString(),
                                Set.of(SecurityConstants.PLATFORM_ADMIN_ROLE)));

        assertEquals(12L, response.id());
        assertEquals(tenantId.toString(), response.primaryTenantId());
        assertEquals(Set.of(SecurityConstants.PLATFORM_ADMIN_ROLE), response.roleNames());
        verify(userTenantMembershipJpaRepository).save(any());
    }

    @Test
    void deactivateUser_LastActivePlatformAdmin_ThrowsConflict() {
        AdminUserService service =
                new AdminUserService(
                        userJpaRepository,
                        roleJpaRepository,
                        tenantJpaRepository,
                        userTenantMembershipJpaRepository,
                        passwordEncoder);

        RoleJpaEntity platformAdminRole =
                new RoleJpaEntity(SecurityConstants.PLATFORM_ADMIN_ROLE, "Platform admin");
        platformAdminRole.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);

        UserJpaEntity existingUser =
                new UserJpaEntity(
                        "platform.admin",
                        "admin@solveria.local",
                        "encoded",
                        SecurityConstants.PLATFORM_ADMIN_USER_CATEGORY,
                        true);
        existingUser.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);
        existingUser.assignRoles(Set.of(platformAdminRole));
        ReflectionTestUtils.setField(existingUser, "id", 1L);

        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userJpaRepository.countDistinctByActiveTrueAndRoles_NameAndRoles_TenantId(
                        SecurityConstants.PLATFORM_ADMIN_ROLE, SecurityConstants.SYSTEM_TENANT_ID))
                .thenReturn(1L);

        assertThrows(BusinessRuleViolationException.class, () -> service.deactivateUser(1L));
    }
}
