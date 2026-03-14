package com.solveria.iamservice.multitenancy.persistence.repository;

import com.solveria.iamservice.multitenancy.persistence.entity.AcademicCycleJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicCycleJpaRepository extends JpaRepository<AcademicCycleJpaEntity, UUID> {
    List<AcademicCycleJpaEntity> findAllByOwnerTenantIdOrderByStartDateAsc(UUID ownerTenantId);
}
