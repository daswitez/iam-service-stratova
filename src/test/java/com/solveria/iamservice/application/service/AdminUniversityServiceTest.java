package com.solveria.iamservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.AdminUniversityCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminUniversityUpdateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUniversityServiceTest {

    @Mock private TenantJpaRepository tenantJpaRepository;

    @Test
    void createUniversity_NormalizesCodeAndReturnsResponse() {
        AdminUniversityService service = new AdminUniversityService(tenantJpaRepository);

        when(tenantJpaRepository.existsByCodeIgnoreCase("umsa-lpz")).thenReturn(false);
        when(tenantJpaRepository.save(any(TenantJpaEntity.class)))
                .thenAnswer(
                        invocation -> {
                            TenantJpaEntity entity = invocation.getArgument(0);
                            ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
                            ReflectionTestUtils.setField(entity, "status", TenantStatus.ACTIVE);
                            ReflectionTestUtils.setField(entity, "createdAt", Instant.now());
                            return entity;
                        });

        var response =
                service.createUniversity(
                        new AdminUniversityCreateRequest(
                                "UMSA-LPZ", "Universidad Mayor de San Andres"));

        assertEquals("umsa-lpz", response.code());
        assertEquals("UNIVERSITY", response.type());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    void updateUniversity_DuplicateCode_ThrowsConflict() {
        AdminUniversityService service = new AdminUniversityService(tenantJpaRepository);

        UUID id = UUID.randomUUID();
        TenantJpaEntity tenant = new TenantJpaEntity("umsa", "UMSA", TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(tenant, "id", id);
        ReflectionTestUtils.setField(tenant, "status", TenantStatus.ACTIVE);

        when(tenantJpaRepository.findById(id)).thenReturn(Optional.of(tenant));
        when(tenantJpaRepository.existsByCodeIgnoreCaseAndIdNot("umsa-new", id)).thenReturn(true);

        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        service.updateUniversity(
                                id.toString(),
                                new AdminUniversityUpdateRequest(
                                        "umsa-new", "Universidad Mayor de San Andres")));
    }
}
