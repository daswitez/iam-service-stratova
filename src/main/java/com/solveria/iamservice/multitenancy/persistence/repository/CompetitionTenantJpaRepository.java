package com.solveria.iamservice.multitenancy.persistence.repository;

import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionTenantJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionTenantJpaRepository
        extends JpaRepository<CompetitionTenantJpaEntity, UUID> {
    boolean existsByCompetition_IdAndTenant_Id(UUID competitionId, UUID tenantId);

    List<CompetitionTenantJpaEntity> findAllByCompetition_IdOrderByCreatedAtAsc(UUID competitionId);

    Optional<CompetitionTenantJpaEntity> findByCompetition_IdAndTenant_Id(
            UUID competitionId, UUID tenantId);
}
