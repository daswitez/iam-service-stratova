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
        name = "iam_team",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_iam_team_competition_code",
                        columnNames = {"competition_id", "code"}))
public class TeamJpaEntity {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private CompetitionJpaEntity competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_tenant_id", nullable = false)
    private TenantJpaEntity originTenant;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TeamStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TeamJpaEntity() {}

    public TeamJpaEntity(
            CompetitionJpaEntity competition,
            TenantJpaEntity originTenant,
            String code,
            String name) {
        this.competition = competition;
        this.originTenant = originTenant;
        this.code = code;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = TeamStatus.FORMING;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public CompetitionJpaEntity getCompetition() {
        return competition;
    }

    public TenantJpaEntity getOriginTenant() {
        return originTenant;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
