package com.solveria.iamservice.config.security;

import com.solveria.core.iam.infrastructure.persistence.entity.RoleJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.entity.UserJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.repository.RoleJpaRepository;
import com.solveria.core.iam.infrastructure.persistence.repository.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class PlatformAdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrapRunner.class);

    private final PlatformAdminBootstrapProperties properties;
    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformAdminBootstrapRunner(
            PlatformAdminBootstrapProperties properties,
            UserJpaRepository userJpaRepository,
            RoleJpaRepository roleJpaRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userJpaRepository = userJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("event=PLATFORM_ADMIN_BOOTSTRAP_SKIPPED reason=disabled");
            return;
        }

        if (!StringUtils.hasText(properties.username())
                || !StringUtils.hasText(properties.email())
                || !StringUtils.hasText(properties.password())) {
            log.warn("event=PLATFORM_ADMIN_BOOTSTRAP_SKIPPED reason=missing_configuration");
            return;
        }

        RoleJpaEntity platformAdminRole = ensurePlatformAdminRole();
        ensurePlatformAdminUser(platformAdminRole);
    }

    private RoleJpaEntity ensurePlatformAdminRole() {
        return roleJpaRepository
                .findByNameAndTenantId(
                        SecurityConstants.PLATFORM_ADMIN_ROLE, SecurityConstants.SYSTEM_TENANT_ID)
                .orElseGet(
                        () -> {
                            RoleJpaEntity role =
                                    new RoleJpaEntity(
                                            SecurityConstants.PLATFORM_ADMIN_ROLE,
                                            "Platform-wide root administrator");
                            role.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);
                            RoleJpaEntity saved = roleJpaRepository.save(role);
                            log.info(
                                    "event=PLATFORM_ADMIN_ROLE_CREATED role={} tenantId={}",
                                    saved.getName(),
                                    saved.getTenantId());
                            return saved;
                        });
    }

    private void ensurePlatformAdminUser(RoleJpaEntity platformAdminRole) {
        UserJpaEntity user =
                userJpaRepository
                        .findByEmail(properties.email())
                        .orElseGet(() -> createPlatformAdmin(platformAdminRole));

        boolean changed = false;

        if (!user.isActive()) {
            user.setActive(true);
            changed = true;
        }

        if (!SecurityConstants.SYSTEM_TENANT_ID.equals(user.getTenantId())) {
            user.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);
            changed = true;
        }

        boolean hasRole =
                user.getRoles().stream()
                        .anyMatch(
                                role ->
                                        SecurityConstants.PLATFORM_ADMIN_ROLE.equals(
                                                role.getName()));
        if (!hasRole) {
            user.getRoles().add(platformAdminRole);
            changed = true;
        }

        if (changed) {
            userJpaRepository.save(user);
            log.info(
                    "event=PLATFORM_ADMIN_USER_UPDATED email={} username={}",
                    user.getEmail(),
                    user.getUsername());
        } else {
            log.info(
                    "event=PLATFORM_ADMIN_USER_PRESENT email={} username={}",
                    user.getEmail(),
                    user.getUsername());
        }
    }

    private UserJpaEntity createPlatformAdmin(RoleJpaEntity platformAdminRole) {
        UserJpaEntity user =
                new UserJpaEntity(
                        properties.username(),
                        properties.email(),
                        passwordEncoder.encode(properties.password()),
                        SecurityConstants.PLATFORM_ADMIN_USER_CATEGORY,
                        true);
        user.setTenantId(SecurityConstants.SYSTEM_TENANT_ID);
        user.getRoles().add(platformAdminRole);
        UserJpaEntity saved = userJpaRepository.save(user);
        log.info(
                "event=PLATFORM_ADMIN_USER_CREATED email={} username={}",
                saved.getEmail(),
                saved.getUsername());
        return saved;
    }
}
