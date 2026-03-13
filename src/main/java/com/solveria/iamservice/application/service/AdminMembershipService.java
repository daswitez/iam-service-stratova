package com.solveria.iamservice.application.service;

import com.solveria.core.iam.infrastructure.persistence.entity.UserJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.repository.UserJpaRepository;
import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.core.shared.exceptions.EntityNotFoundException;
import com.solveria.iamservice.api.rest.dto.AdminMembershipCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminMembershipResponse;
import com.solveria.iamservice.api.rest.dto.AdminMembershipUpdateRequest;
import com.solveria.iamservice.config.security.SecurityConstants;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipType;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.UserTenantMembershipJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.UserTenantMembershipJpaRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminMembershipService {

    private final UserJpaRepository userJpaRepository;
    private final TenantJpaRepository tenantJpaRepository;
    private final UserTenantMembershipJpaRepository userTenantMembershipJpaRepository;

    public AdminMembershipService(
            UserJpaRepository userJpaRepository,
            TenantJpaRepository tenantJpaRepository,
            UserTenantMembershipJpaRepository userTenantMembershipJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.tenantJpaRepository = tenantJpaRepository;
        this.userTenantMembershipJpaRepository = userTenantMembershipJpaRepository;
    }

    @Transactional
    public AdminMembershipResponse createMembership(AdminMembershipCreateRequest request) {
        UserJpaEntity user = getUser(request.userId());
        TenantJpaEntity tenant = getTenant(request.tenantId());
        if (userTenantMembershipJpaRepository.existsByUserIdAndTenant_Id(
                user.getId(), tenant.getId())) {
            throw new BusinessRuleViolationException("membership.already.exists");
        }

        MembershipStatus status = parseStatusOrDefault(request.status(), MembershipStatus.ACTIVE);
        UserTenantMembershipJpaEntity membership =
                new UserTenantMembershipJpaEntity(
                        user.getId(),
                        tenant,
                        request.isPrimary() ? MembershipType.PRIMARY : MembershipType.SECONDARY);
        membership.setStatus(status);

        UserTenantMembershipJpaEntity savedMembership =
                userTenantMembershipJpaRepository.save(membership);

        rebalanceActivePrimaryMemberships(
                user.getId(),
                request.isPrimary() && status == MembershipStatus.ACTIVE
                        ? savedMembership.getId()
                        : null);
        syncLegacyTenantId(user);

        return toResponse(getMembershipEntity(savedMembership.getId().toString()));
    }

    @Transactional(readOnly = true)
    public AdminMembershipResponse getMembership(String id) {
        return toResponse(getMembershipEntity(id));
    }

    @Transactional(readOnly = true)
    public List<AdminMembershipResponse> listMembershipsByUser(Long userId, String status) {
        getUser(userId);
        MembershipStatus requestedStatus = parseOptionalStatus(status);

        return userTenantMembershipJpaRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(
                        membership ->
                                requestedStatus == null
                                        || membership.getStatus() == requestedStatus)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminMembershipResponse> listMembershipsByTenant(String tenantId, String status) {
        TenantJpaEntity tenant = getTenant(tenantId);
        MembershipStatus requestedStatus = parseOptionalStatus(status);

        return userTenantMembershipJpaRepository
                .findAllByTenant_IdOrderByUserIdAscCreatedAtAsc(tenant.getId())
                .stream()
                .filter(
                        membership ->
                                requestedStatus == null
                                        || membership.getStatus() == requestedStatus)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminMembershipResponse updateMembership(
            String id, AdminMembershipUpdateRequest request) {
        UserTenantMembershipJpaEntity membership = getMembershipEntity(id);

        if (StringUtils.hasText(request.status())) {
            membership.setStatus(parseStatusOrDefault(request.status(), membership.getStatus()));
        }

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            membership.setMembershipType(MembershipType.SECONDARY);
        } else if (request.isPrimary() != null) {
            membership.setMembershipType(
                    request.isPrimary() ? MembershipType.PRIMARY : MembershipType.SECONDARY);
        }

        UserTenantMembershipJpaEntity savedMembership =
                userTenantMembershipJpaRepository.save(membership);

        rebalanceActivePrimaryMemberships(
                savedMembership.getUserId(),
                Boolean.TRUE.equals(request.isPrimary())
                                && savedMembership.getStatus() == MembershipStatus.ACTIVE
                        ? savedMembership.getId()
                        : null);
        syncLegacyTenantId(getUser(savedMembership.getUserId()));

        return toResponse(getMembershipEntity(savedMembership.getId().toString()));
    }

    @Transactional
    public void deactivateMembership(String id) {
        UserTenantMembershipJpaEntity membership = getMembershipEntity(id);
        if (membership.getStatus() == MembershipStatus.LEFT) {
            syncLegacyTenantId(getUser(membership.getUserId()));
            return;
        }

        membership.setStatus(MembershipStatus.LEFT);
        membership.setMembershipType(MembershipType.SECONDARY);
        userTenantMembershipJpaRepository.save(membership);

        rebalanceActivePrimaryMemberships(membership.getUserId(), null);
        syncLegacyTenantId(getUser(membership.getUserId()));
    }

    private void rebalanceActivePrimaryMemberships(Long userId, UUID preferredPrimaryId) {
        List<UserTenantMembershipJpaEntity> activeMemberships =
                userTenantMembershipJpaRepository.findByUserIdAndStatusOrderByCreatedAtAsc(
                        userId, MembershipStatus.ACTIVE);
        if (activeMemberships.isEmpty()) {
            return;
        }

        UserTenantMembershipJpaEntity selectedPrimary =
                preferredPrimaryId != null
                        ? activeMemberships.stream()
                                .filter(membership -> membership.getId().equals(preferredPrimaryId))
                                .findFirst()
                                .orElse(null)
                        : null;

        if (selectedPrimary == null) {
            selectedPrimary =
                    activeMemberships.stream()
                            .filter(
                                    membership ->
                                            membership.getMembershipType()
                                                    == MembershipType.PRIMARY)
                            .findFirst()
                            .orElse(activeMemberships.getFirst());
        }

        UUID selectedPrimaryId = selectedPrimary.getId();
        activeMemberships.forEach(
                membership -> {
                    MembershipType expectedType =
                            membership.getId().equals(selectedPrimaryId)
                                    ? MembershipType.PRIMARY
                                    : MembershipType.SECONDARY;
                    if (membership.getMembershipType() != expectedType) {
                        membership.setMembershipType(expectedType);
                        userTenantMembershipJpaRepository.save(membership);
                    }
                });
    }

    private void syncLegacyTenantId(UserJpaEntity user) {
        String resolvedTenantId =
                userTenantMembershipJpaRepository
                        .findFirstByUserIdAndMembershipTypeAndStatusOrderByCreatedAtAsc(
                                user.getId(), MembershipType.PRIMARY, MembershipStatus.ACTIVE)
                        .map(membership -> membership.getTenant().getId().toString())
                        .orElse(SecurityConstants.SYSTEM_TENANT_ID);

        if (!resolvedTenantId.equals(user.getTenantId())) {
            user.setTenantId(resolvedTenantId);
            userJpaRepository.save(user);
        }
    }

    private UserJpaEntity getUser(Long userId) {
        return userJpaRepository
                .findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(userId)));
    }

    private TenantJpaEntity getTenant(String tenantId) {
        UUID parsedTenantId = parseUuid(tenantId, "tenant.reference.invalid");
        return tenantJpaRepository
                .findById(parsedTenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));
    }

    private UserTenantMembershipJpaEntity getMembershipEntity(String id) {
        UUID membershipId = parseUuid(id, "membership.reference.invalid");
        return userTenantMembershipJpaRepository
                .findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Membership", id));
    }

    private UUID parseUuid(String value, String errorCode) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(errorCode);
        }
    }

    private MembershipStatus parseOptionalStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parseStatusOrDefault(status, null);
    }

    private MembershipStatus parseStatusOrDefault(String status, MembershipStatus defaultValue) {
        if (!StringUtils.hasText(status)) {
            return defaultValue;
        }
        try {
            return MembershipStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("membership.status.invalid");
        }
    }

    private AdminMembershipResponse toResponse(UserTenantMembershipJpaEntity membership) {
        TenantJpaEntity tenant = membership.getTenant();
        return new AdminMembershipResponse(
                membership.getId().toString(),
                membership.getUserId(),
                tenant.getId().toString(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getType().name(),
                membership.getMembershipType().name(),
                membership.getMembershipType() == MembershipType.PRIMARY,
                membership.getStatus().name(),
                membership.getCreatedAt());
    }
}
