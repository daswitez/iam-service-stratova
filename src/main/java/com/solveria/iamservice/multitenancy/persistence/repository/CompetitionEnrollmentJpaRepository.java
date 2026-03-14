package com.solveria.iamservice.multitenancy.persistence.repository;

import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionEnrollmentJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitionEnrollmentJpaRepository
        extends JpaRepository<CompetitionEnrollmentJpaEntity, UUID> {

    boolean existsByCompetition_IdAndUserId(UUID competitionId, Long userId);

    Optional<CompetitionEnrollmentJpaEntity> findByCompetition_IdAndId(
            UUID competitionId, UUID enrollmentId);
}
