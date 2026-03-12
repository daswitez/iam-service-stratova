package com.solveria.iamservice.multitenancy.persistence.repository;

import com.solveria.iamservice.multitenancy.persistence.entity.MembershipStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipType;
import com.solveria.iamservice.multitenancy.persistence.entity.UserTenantMembershipJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTenantMembershipJpaRepository
        extends JpaRepository<UserTenantMembershipJpaEntity, UUID> {

    List<UserTenantMembershipJpaEntity> findByUserIdAndStatusOrderByCreatedAtAsc(
            Long userId, MembershipStatus status);

    Optional<UserTenantMembershipJpaEntity>
            findFirstByUserIdAndMembershipTypeAndStatusOrderByCreatedAtAsc(
                    Long userId, MembershipType membershipType, MembershipStatus status);

    boolean existsByUserIdAndTenant_Id(Long userId, UUID tenantId);
}
