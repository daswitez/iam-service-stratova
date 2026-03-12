package com.solveria.iamservice.application.service;

import com.solveria.core.iam.infrastructure.persistence.entity.RoleJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.entity.UserJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.repository.RoleJpaRepository;
import com.solveria.core.iam.infrastructure.persistence.repository.UserJpaRepository;
import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.core.shared.exceptions.EntityNotFoundException;
import com.solveria.iamservice.api.rest.dto.AdminUserCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminUserResponse;
import com.solveria.iamservice.api.rest.dto.AdminUserUpdateRequest;
import com.solveria.iamservice.config.security.SecurityConstants;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipType;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.UserTenantMembershipJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.UserTenantMembershipJpaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserService {

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final TenantJpaRepository tenantJpaRepository;
    private final UserTenantMembershipJpaRepository userTenantMembershipJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            UserJpaRepository userJpaRepository,
            RoleJpaRepository roleJpaRepository,
            TenantJpaRepository tenantJpaRepository,
            UserTenantMembershipJpaRepository userTenantMembershipJpaRepository,
            PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.tenantJpaRepository = tenantJpaRepository;
        this.userTenantMembershipJpaRepository = userTenantMembershipJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        validateCreateRequest(request);

        Set<RoleJpaEntity> roles = resolveRoles(request.roleNames());
        TenantJpaEntity primaryTenant = resolvePrimaryTenant(request.primaryTenantId());

        UserJpaEntity user =
                new UserJpaEntity(
                        request.username().trim(),
                        request.email().trim().toLowerCase(Locale.ROOT),
                        passwordEncoder.encode(request.password()),
                        request.userCategory(),
                        true);
        user.setTenantId(resolveLegacyTenantId(primaryTenant));
        user.assignRoles(roles);

        UserJpaEntity savedUser = userJpaRepository.save(user);
        syncPrimaryTenantMembership(savedUser.getId(), primaryTenant);

        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(Boolean active) {
        return userJpaRepository.findAllByOrderByIdAsc().stream()
                .filter(user -> active == null || user.isActive() == active)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        return toResponse(getUserEntity(id));
    }

    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request) {
        UserJpaEntity user = getUserEntity(id);
        validateUpdateRequest(id, request);

        Set<RoleJpaEntity> newRoles = resolveRoles(request.roleNames());
        boolean newActive = request.active() != null ? request.active() : user.isActive();
        ensurePlatformAdminRetention(user, newRoles, newActive);

        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setUserCategory(request.userCategory());
        user.setActive(newActive);
        if (StringUtils.hasText(request.password())) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.assignRoles(newRoles);

        TenantJpaEntity primaryTenant = resolvePrimaryTenant(request.primaryTenantId());
        if (primaryTenant != null) {
            user.setTenantId(resolveLegacyTenantId(primaryTenant));
        }

        UserJpaEntity savedUser = userJpaRepository.save(user);
        if (primaryTenant != null) {
            syncPrimaryTenantMembership(savedUser.getId(), primaryTenant);
        }

        return toResponse(savedUser);
    }

    @Transactional
    public void deactivateUser(Long id) {
        UserJpaEntity user = getUserEntity(id);
        if (!user.isActive()) {
            return;
        }

        ensurePlatformAdminRetention(user, user.getRoles(), false);
        user.setActive(false);
        userJpaRepository.save(user);
    }

    private void validateCreateRequest(AdminUserCreateRequest request) {
        if (userJpaRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new BusinessRuleViolationException("email.already.taken");
        }
        if (userJpaRepository.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new BusinessRuleViolationException("username.already.taken");
        }
        validatePlatformAdminCategory(request.userCategory(), request.roleNames());
    }

    private void validateUpdateRequest(Long id, AdminUserUpdateRequest request) {
        if (userJpaRepository.existsByEmailIgnoreCaseAndIdNot(request.email().trim(), id)) {
            throw new BusinessRuleViolationException("email.already.taken");
        }
        if (userJpaRepository.existsByUsernameIgnoreCaseAndIdNot(request.username().trim(), id)) {
            throw new BusinessRuleViolationException("username.already.taken");
        }
        validatePlatformAdminCategory(request.userCategory(), request.roleNames());
    }

    private void validatePlatformAdminCategory(String userCategory, Set<String> roleNames) {
        if (roleNames.contains(SecurityConstants.PLATFORM_ADMIN_ROLE)
                && !SecurityConstants.PLATFORM_ADMIN_USER_CATEGORY.equals(userCategory)) {
            throw new BusinessRuleViolationException("platform.admin.requires.academic.admin");
        }
    }

    private Set<RoleJpaEntity> resolveRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(
                        roleName ->
                                roleJpaRepository
                                        .findByNameAndTenantId(
                                                roleName, SecurityConstants.SYSTEM_TENANT_ID)
                                        .orElseThrow(
                                                () ->
                                                        new EntityNotFoundException(
                                                                "Role", roleName)))
                .collect(Collectors.toSet());
    }

    private TenantJpaEntity resolvePrimaryTenant(String primaryTenantId) {
        if (!StringUtils.hasText(primaryTenantId)) {
            return null;
        }

        UUID tenantId;
        try {
            tenantId = UUID.fromString(primaryTenantId.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("tenant.reference.invalid");
        }

        return tenantJpaRepository
                .findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", primaryTenantId));
    }

    private String resolveLegacyTenantId(TenantJpaEntity primaryTenant) {
        return primaryTenant != null
                ? primaryTenant.getId().toString()
                : SecurityConstants.SYSTEM_TENANT_ID;
    }

    private void syncPrimaryTenantMembership(Long userId, TenantJpaEntity primaryTenant) {
        if (primaryTenant == null) {
            return;
        }

        Optional<UserTenantMembershipJpaEntity> currentPrimary =
                userTenantMembershipJpaRepository
                        .findFirstByUserIdAndMembershipTypeAndStatusOrderByCreatedAtAsc(
                                userId, MembershipType.PRIMARY, MembershipStatus.ACTIVE);
        currentPrimary
                .filter(membership -> !membership.getTenant().getId().equals(primaryTenant.getId()))
                .ifPresent(
                        membership -> {
                            membership.setMembershipType(MembershipType.SECONDARY);
                            userTenantMembershipJpaRepository.save(membership);
                        });

        UserTenantMembershipJpaEntity targetMembership =
                userTenantMembershipJpaRepository
                        .findByUserIdAndTenant_Id(userId, primaryTenant.getId())
                        .orElseGet(
                                () ->
                                        new UserTenantMembershipJpaEntity(
                                                userId, primaryTenant, MembershipType.PRIMARY));
        targetMembership.setMembershipType(MembershipType.PRIMARY);
        targetMembership.setStatus(MembershipStatus.ACTIVE);
        userTenantMembershipJpaRepository.save(targetMembership);
    }

    private void ensurePlatformAdminRetention(
            UserJpaEntity user, Set<RoleJpaEntity> newRoles, boolean newActive) {
        boolean currentlyPlatformAdmin = hasPlatformAdminRole(user.getRoles()) && user.isActive();
        boolean remainsPlatformAdmin = hasPlatformAdminRole(newRoles) && newActive;

        if (!currentlyPlatformAdmin || remainsPlatformAdmin) {
            return;
        }

        long activePlatformAdmins =
                userJpaRepository.countDistinctByActiveTrueAndRoles_NameAndRoles_TenantId(
                        SecurityConstants.PLATFORM_ADMIN_ROLE, SecurityConstants.SYSTEM_TENANT_ID);
        if (activePlatformAdmins <= 1) {
            throw new BusinessRuleViolationException("platform.admin.last.active.required");
        }
    }

    private boolean hasPlatformAdminRole(Set<RoleJpaEntity> roles) {
        return roles.stream()
                .anyMatch(
                        role ->
                                SecurityConstants.PLATFORM_ADMIN_ROLE.equals(role.getName())
                                        && SecurityConstants.SYSTEM_TENANT_ID.equals(
                                                role.getTenantId()));
    }

    private UserJpaEntity getUserEntity(Long id) {
        return userJpaRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(id)));
    }

    private AdminUserResponse toResponse(UserJpaEntity user) {
        String primaryTenantId =
                userTenantMembershipJpaRepository
                        .findFirstByUserIdAndMembershipTypeAndStatusOrderByCreatedAtAsc(
                                user.getId(), MembershipType.PRIMARY, MembershipStatus.ACTIVE)
                        .map(membership -> membership.getTenant().getId().toString())
                        .orElseGet(
                                () ->
                                        SecurityConstants.SYSTEM_TENANT_ID.equals(
                                                        user.getTenantId())
                                                ? null
                                                : user.getTenantId());

        Set<String> roleNames =
                user.getRoles().stream()
                        .sorted(Comparator.comparing(RoleJpaEntity::getName))
                        .map(RoleJpaEntity::getName)
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserCategory(),
                user.isActive(),
                primaryTenantId,
                roleNames,
                user.getCreatedAt(),
                user.getLastModifiedAt());
    }
}
