package com.solveria.iamservice.application.service;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.core.shared.exceptions.EntityNotFoundException;
import com.solveria.iamservice.api.rest.dto.AdminSubTenantCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminSubTenantResponse;
import com.solveria.iamservice.api.rest.dto.AdminSubTenantUpdateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSubTenantService {

    private final TenantJpaRepository tenantJpaRepository;

    public AdminSubTenantService(TenantJpaRepository tenantJpaRepository) {
        this.tenantJpaRepository = tenantJpaRepository;
    }

    @Transactional
    public AdminSubTenantResponse createSubTenant(
            String parentId, AdminSubTenantCreateRequest request) {
        TenantJpaEntity parent = getTenant(parentId);
        TenantType childType = parseChildType(request.type());
        validateHierarchy(parent.getType(), childType);

        String normalizedCode = normalizeCode(request.code());
        validateUniqueCode(normalizedCode, null);

        TenantJpaEntity subTenant =
                tenantJpaRepository.save(
                        new TenantJpaEntity(
                                normalizedCode, request.name().trim(), childType, parent));
        return toResponse(subTenant);
    }

    @Transactional(readOnly = true)
    public List<AdminSubTenantResponse> listChildren(String parentId, String type, String status) {
        TenantJpaEntity parent = getTenant(parentId);
        TenantType requestedType = parseOptionalChildType(type);
        TenantStatus requestedStatus = parseStatus(status);

        return tenantJpaRepository
                .findAllByParentTenant_IdOrderByCreatedAtAsc(parent.getId())
                .stream()
                .filter(this::isSupportedSubTenant)
                .filter(tenant -> requestedType == null || tenant.getType() == requestedType)
                .filter(tenant -> requestedStatus == null || tenant.getStatus() == requestedStatus)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminSubTenantResponse getSubTenant(String id) {
        return toResponse(getSubTenantEntity(id));
    }

    @Transactional
    public AdminSubTenantResponse updateSubTenant(String id, AdminSubTenantUpdateRequest request) {
        TenantJpaEntity subTenant = getSubTenantEntity(id);
        String normalizedCode = normalizeCode(request.code());
        validateUniqueCode(normalizedCode, subTenant.getId());

        subTenant.setCode(normalizedCode);
        subTenant.setName(request.name().trim());

        return toResponse(tenantJpaRepository.save(subTenant));
    }

    @Transactional
    public void deactivateSubTenant(String id) {
        TenantJpaEntity subTenant = getSubTenantEntity(id);
        if (subTenant.getStatus() == TenantStatus.INACTIVE) {
            return;
        }
        subTenant.setStatus(TenantStatus.INACTIVE);
        tenantJpaRepository.save(subTenant);
    }

    private void validateUniqueCode(String normalizedCode, UUID currentId) {
        boolean exists =
                currentId == null
                        ? tenantJpaRepository.existsByCodeIgnoreCase(normalizedCode)
                        : tenantJpaRepository.existsByCodeIgnoreCaseAndIdNot(
                                normalizedCode, currentId);
        if (exists) {
            throw new BusinessRuleViolationException("tenant.code.already.taken");
        }
    }

    private void validateHierarchy(TenantType parentType, TenantType childType) {
        boolean valid =
                (parentType == TenantType.UNIVERSITY && childType == TenantType.FACULTY)
                        || (parentType == TenantType.FACULTY && childType == TenantType.PROGRAM);
        if (!valid) {
            throw new BusinessRuleViolationException("tenant.hierarchy.invalid");
        }
    }

    private TenantJpaEntity getTenant(String id) {
        UUID tenantId = parseTenantId(id);
        return tenantJpaRepository
                .findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", id));
    }

    private TenantJpaEntity getSubTenantEntity(String id) {
        TenantJpaEntity tenant = getTenant(id);
        if (!isSupportedSubTenant(tenant)) {
            throw new EntityNotFoundException("SubTenant", id);
        }
        return tenant;
    }

    private boolean isSupportedSubTenant(TenantJpaEntity tenant) {
        return tenant.getType() == TenantType.FACULTY || tenant.getType() == TenantType.PROGRAM;
    }

    private UUID parseTenantId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("tenant.reference.invalid");
        }
    }

    private TenantType parseChildType(String type) {
        try {
            TenantType tenantType = TenantType.valueOf(type.trim().toUpperCase(Locale.ROOT));
            if (tenantType != TenantType.FACULTY && tenantType != TenantType.PROGRAM) {
                throw new IllegalArgumentException("Unsupported sub-tenant type");
            }
            return tenantType;
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("tenant.type.invalid");
        }
    }

    private TenantType parseOptionalChildType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return parseChildType(type);
    }

    private TenantStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TenantStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("tenant.status.invalid");
        }
    }

    private String normalizeCode(String code) {
        String normalized =
                code.trim()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9-]+", "-")
                        .replaceAll("^-+", "")
                        .replaceAll("-+$", "");
        if (normalized.isBlank()) {
            throw new BusinessRuleViolationException("tenant.code.invalid");
        }
        return normalized;
    }

    private AdminSubTenantResponse toResponse(TenantJpaEntity subTenant) {
        TenantJpaEntity parentTenant = subTenant.getParentTenant();
        return new AdminSubTenantResponse(
                subTenant.getId().toString(),
                subTenant.getCode(),
                subTenant.getName(),
                subTenant.getType().name(),
                subTenant.getStatus().name(),
                parentTenant != null ? parentTenant.getId().toString() : null,
                parentTenant != null ? parentTenant.getType().name() : null,
                subTenant.getCreatedAt());
    }
}
