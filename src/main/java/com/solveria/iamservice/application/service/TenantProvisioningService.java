package com.solveria.iamservice.application.service;

import com.solveria.core.shared.exceptions.EntityNotFoundException;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipType;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.entity.UserTenantMembershipJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.UserTenantMembershipJpaRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TenantProvisioningService {

    private final TenantJpaRepository tenantJpaRepository;
    private final UserTenantMembershipJpaRepository userTenantMembershipJpaRepository;

    public TenantProvisioningService(
            TenantJpaRepository tenantJpaRepository,
            UserTenantMembershipJpaRepository userTenantMembershipJpaRepository) {
        this.tenantJpaRepository = tenantJpaRepository;
        this.userTenantMembershipJpaRepository = userTenantMembershipJpaRepository;
    }

    @Transactional
    public TenantJpaEntity resolveOrCreateTenant(
            String tenantReference, String tenantName, String userCategory, String username) {
        if (StringUtils.hasText(tenantReference)) {
            TenantJpaEntity existing = findTenant(tenantReference.trim());
            if (existing != null) {
                return existing;
            }

            String resolvedName =
                    StringUtils.hasText(tenantName) ? tenantName.trim() : tenantReference.trim();
            return createTenant(tenantReference, resolvedName, inferTenantType(userCategory));
        }

        String resolvedName =
                StringUtils.hasText(tenantName)
                        ? tenantName.trim()
                        : username.trim() + " Workspace";
        return createTenant(resolvedName, resolvedName, inferTenantType(userCategory));
    }

    @Transactional
    public void ensurePrimaryMembership(Long userId, TenantJpaEntity tenant) {
        if (userTenantMembershipJpaRepository.existsByUserIdAndTenant_Id(userId, tenant.getId())) {
            return;
        }

        userTenantMembershipJpaRepository.save(
                new UserTenantMembershipJpaEntity(userId, tenant, MembershipType.PRIMARY));
    }

    private TenantJpaEntity findTenant(String tenantReference) {
        try {
            UUID tenantId = UUID.fromString(tenantReference);
            return tenantJpaRepository
                    .findById(tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantReference));
        } catch (IllegalArgumentException ignored) {
            return tenantJpaRepository.findByCode(normalizeCode(tenantReference)).orElse(null);
        }
    }

    private TenantJpaEntity createTenant(
            String codeSeed, String tenantName, TenantType tenantType) {
        String code = nextAvailableCode(codeSeed);
        return tenantJpaRepository.save(new TenantJpaEntity(code, tenantName, tenantType, null));
    }

    private String nextAvailableCode(String seed) {
        String baseCode = normalizeCode(seed);
        if (!StringUtils.hasText(baseCode)) {
            baseCode = "tenant";
        }
        if (tenantJpaRepository.findByCode(baseCode).isEmpty()) {
            return baseCode;
        }
        return baseCode + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String normalizeCode(String seed) {
        return seed.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    private TenantType inferTenantType(String userCategory) {
        return switch (userCategory) {
            case "STUDENT", "PROFESSOR", "ACADEMIC_ADMIN" -> TenantType.UNIVERSITY;
            case "FOUNDER" -> TenantType.STARTUP;
            case "EXECUTIVE" -> TenantType.ENTERPRISE;
            default -> TenantType.UNIVERSITY;
        };
    }
}
