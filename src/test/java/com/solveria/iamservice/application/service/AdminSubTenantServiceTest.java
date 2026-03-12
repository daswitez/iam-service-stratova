package com.solveria.iamservice.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.AdminSubTenantCreateRequest;
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
class AdminSubTenantServiceTest {

    @Mock private TenantJpaRepository tenantJpaRepository;

    @Test
    void createSubTenant_FacultyUnderUniversity_ReturnsResponse() {
        AdminSubTenantService service = new AdminSubTenantService(tenantJpaRepository);

        UUID parentId = UUID.randomUUID();
        TenantJpaEntity university =
                new TenantJpaEntity(
                        "umsa", "Universidad Mayor de San Andres", TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(university, "id", parentId);
        ReflectionTestUtils.setField(university, "status", TenantStatus.ACTIVE);

        when(tenantJpaRepository.findById(parentId)).thenReturn(Optional.of(university));
        when(tenantJpaRepository.existsByCodeIgnoreCase("facultad-ingenieria")).thenReturn(false);
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
                service.createSubTenant(
                        parentId.toString(),
                        new AdminSubTenantCreateRequest(
                                "facultad-ingenieria", "Facultad de Ingenieria", "FACULTY"));

        assertEquals("FACULTY", response.type());
        assertEquals(parentId.toString(), response.parentTenantId());
        assertEquals("UNIVERSITY", response.parentTenantType());
    }

    @Test
    void createSubTenant_ProgramUnderUniversity_ThrowsConflict() {
        AdminSubTenantService service = new AdminSubTenantService(tenantJpaRepository);

        UUID parentId = UUID.randomUUID();
        TenantJpaEntity university =
                new TenantJpaEntity(
                        "umsa", "Universidad Mayor de San Andres", TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(university, "id", parentId);
        ReflectionTestUtils.setField(university, "status", TenantStatus.ACTIVE);

        when(tenantJpaRepository.findById(parentId)).thenReturn(Optional.of(university));

        assertThrows(
                BusinessRuleViolationException.class,
                () ->
                        service.createSubTenant(
                                parentId.toString(),
                                new AdminSubTenantCreateRequest(
                                        "adm-sis-umsa",
                                        "Administracion de Sistemas UMSA",
                                        "PROGRAM")));
    }
}
