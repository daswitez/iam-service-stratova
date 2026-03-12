package com.solveria.iamservice.multitenancy.persistence.repository;

import com.solveria.iamservice.multitenancy.persistence.entity.MembershipStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TeamMemberJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberJpaRepository extends JpaRepository<TeamMemberJpaEntity, UUID> {

    @EntityGraph(
            attributePaths = {
                "team",
                "team.originTenant",
                "team.competition",
                "team.competition.academicCycle"
            })
    List<TeamMemberJpaEntity> findByUserIdAndStatus(Long userId, MembershipStatus status);
}
