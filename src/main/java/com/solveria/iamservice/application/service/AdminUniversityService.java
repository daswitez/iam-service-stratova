package com.solveria.iamservice.application.service;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.core.shared.exceptions.EntityNotFoundException;
import com.solveria.iamservice.api.rest.dto.AdminUniversityCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminUniversityResponse;
import com.solveria.iamservice.api.rest.dto.AdminUniversityUpdateRequest;
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
public class AdminUniversityService {

    private final TenantJpaRepository tenantJpaRepository;

    public AdminUniversityService(TenantJpaRepository tenantJpaRepository) {
        this.tenantJpaRepository = tenantJpaRepository;
    }

    @Transactional
    public AdminUniversityResponse createUniversity(AdminUniversityCreateRequest request) {
        String normalizedCode = normalizeCode(request.code());
        validateUniqueCode(normalizedCode, null);

        TenantJpaEntity university =
                tenantJpaRepository.save(
                        new TenantJpaEntity(
                                normalizedCode,
                                request.name().trim(),
                                TenantType.UNIVERSITY,
                                null));
        return toResponse(university);
    }

    @Transactional(readOnly = true)
    public List<AdminUniversityResponse> listUniversities(String status) {
        TenantStatus requestedStatus = parseStatus(status);
        return tenantJpaRepository.findAllByTypeOrderByCreatedAtAsc(TenantType.UNIVERSITY).stream()
                .filter(tenant -> requestedStatus == null || tenant.getStatus() == requestedStatus)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUniversityResponse getUniversity(String id) {
        return toResponse(getUniversityEntity(id));
    }

    @Transactional
    public AdminUniversityResponse updateUniversity(
            String id, AdminUniversityUpdateRequest request) {
        TenantJpaEntity university = getUniversityEntity(id);
        String normalizedCode = normalizeCode(request.code());
        validateUniqueCode(normalizedCode, university.getId());

        university.setCode(normalizedCode);
        university.setName(request.name().trim());

        return toResponse(tenantJpaRepository.save(university));
    }

    @Transactional
    public void deactivateUniversity(String id) {
        TenantJpaEntity university = getUniversityEntity(id);
        if (university.getStatus() == TenantStatus.INACTIVE) {
            return;
        }
        university.setStatus(TenantStatus.INACTIVE);
        tenantJpaRepository.save(university);
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

    private TenantJpaEntity getUniversityEntity(String id) {
        UUID tenantId;
        try {
            tenantId = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("tenant.reference.invalid");
        }

        TenantJpaEntity tenant =
                tenantJpaRepository
                        .findById(tenantId)
                        .orElseThrow(() -> new EntityNotFoundException("Tenant", id));
        if (tenant.getType() != TenantType.UNIVERSITY) {
            throw new EntityNotFoundException("University", id);
        }
        return tenant;
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

    private AdminUniversityResponse toResponse(TenantJpaEntity university) {
        return new AdminUniversityResponse(
                university.getId().toString(),
                university.getCode(),
                university.getName(),
                university.getType().name(),
                university.getStatus().name(),
                university.getCreatedAt());
    }
}
