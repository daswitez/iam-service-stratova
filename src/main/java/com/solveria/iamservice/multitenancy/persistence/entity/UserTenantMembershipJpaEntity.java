package com.solveria.iamservice.multitenancy.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "iam_user_tenant_membership",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_iam_user_tenant_membership",
                        columnNames = {"user_id", "tenant_id"}))
public class UserTenantMembershipJpaEntity {

    @Id private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantJpaEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false, length = 32)
    private MembershipType membershipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserTenantMembershipJpaEntity() {}

    public UserTenantMembershipJpaEntity(
            Long userId, TenantJpaEntity tenant, MembershipType membershipType) {
        this.userId = userId;
        this.tenant = tenant;
        this.membershipType = membershipType;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = MembershipStatus.ACTIVE;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public TenantJpaEntity getTenant() {
        return tenant;
    }

    public MembershipType getMembershipType() {
        return membershipType;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
