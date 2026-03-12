package com.solveria.iamservice.multitenancy.persistence.entity;

import jakarta.persistence.Entity;
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
        name = "iam_competition_tenant",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_iam_competition_tenant",
                        columnNames = {"competition_id", "tenant_id"}))
public class CompetitionTenantJpaEntity {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private CompetitionJpaEntity competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantJpaEntity tenant;

    @jakarta.persistence.Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CompetitionTenantJpaEntity() {}

    public CompetitionTenantJpaEntity(CompetitionJpaEntity competition, TenantJpaEntity tenant) {
        this.competition = competition;
        this.tenant = tenant;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
