package com.solveria.iamservice.config.security;

import static org.mockito.Mockito.*;

import com.solveria.core.iam.infrastructure.persistence.entity.RoleJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.entity.UserJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.repository.RoleJpaRepository;
import com.solveria.core.iam.infrastructure.persistence.repository.UserJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PlatformAdminBootstrapRunnerTest {

    @Mock private UserJpaRepository userJpaRepository;
    @Mock private RoleJpaRepository roleJpaRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private PlatformAdminBootstrapRunner runner;

    @Test
    void run_CreatesRoleAndPlatformAdminWhenMissing() throws Exception {
        PlatformAdminBootstrapProperties properties =
                new PlatformAdminBootstrapProperties(
                        true, "platform.admin", "admin@solveria.local", "Admin12345!");
        runner =
                new PlatformAdminBootstrapRunner(
                        properties, userJpaRepository, roleJpaRepository, passwordEncoder);

        when(roleJpaRepository.findByNameAndTenantId(
                        SecurityConstants.PLATFORM_ADMIN_ROLE, SecurityConstants.SYSTEM_TENANT_ID))
                .thenReturn(Optional.empty());
        when(userJpaRepository.findByEmail("admin@solveria.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin12345!")).thenReturn("encodedPassword");

        RoleJpaEntity savedRole =
                new RoleJpaEntity(SecurityConstants.PLATFORM_ADMIN_ROLE, "Platform admin");
        savedRole.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);
        when(roleJpaRepository.save(any(RoleJpaEntity.class))).thenReturn(savedRole);
        when(userJpaRepository.save(any(UserJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(roleJpaRepository).save(any(RoleJpaEntity.class));
        verify(userJpaRepository).save(any(UserJpaEntity.class));
        verify(passwordEncoder).encode("Admin12345!");
    }

    @Test
    void run_AssignsRoleToExistingUserWhenMissing() throws Exception {
        PlatformAdminBootstrapProperties properties =
                new PlatformAdminBootstrapProperties(
                        true, "platform.admin", "admin@solveria.local", "Admin12345!");
        runner =
                new PlatformAdminBootstrapRunner(
                        properties, userJpaRepository, roleJpaRepository, passwordEncoder);

        RoleJpaEntity existingRole =
                new RoleJpaEntity(SecurityConstants.PLATFORM_ADMIN_ROLE, "Platform admin");
        existingRole.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);

        UserJpaEntity existingUser =
                new UserJpaEntity(
                        "platform.admin",
                        "admin@solveria.local",
                        "encodedPassword",
                        SecurityConstants.PLATFORM_ADMIN_USER_CATEGORY,
                        true);
        existingUser.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);

        when(roleJpaRepository.findByNameAndTenantId(
                        SecurityConstants.PLATFORM_ADMIN_ROLE, SecurityConstants.SYSTEM_TENANT_ID))
                .thenReturn(Optional.of(existingRole));
        when(userJpaRepository.findByEmail("admin@solveria.local"))
                .thenReturn(Optional.of(existingUser));
        when(userJpaRepository.save(existingUser)).thenReturn(existingUser);

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(userJpaRepository).save(existingUser);
    }
}
