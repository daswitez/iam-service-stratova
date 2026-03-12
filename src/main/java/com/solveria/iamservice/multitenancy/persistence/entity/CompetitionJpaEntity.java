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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iam_competition")
public class CompetitionJpaEntity {

    @Id private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CompetitionScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CompetitionStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_tenant_id", nullable = false)
    private TenantJpaEntity ownerTenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_cycle_id")
    private AcademicCycleJpaEntity academicCycle;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CompetitionJpaEntity() {}

    public CompetitionJpaEntity(
            String code,
            String name,
            String description,
            CompetitionScope scope,
            TenantJpaEntity ownerTenant,
            AcademicCycleJpaEntity academicCycle) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.scope = scope;
        this.ownerTenant = ownerTenant;
        this.academicCycle = academicCycle;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = CompetitionStatus.DRAFT;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public CompetitionScope getScope() {
        return scope;
    }

    public CompetitionStatus getStatus() {
        return status;
    }

    public AcademicCycleJpaEntity getAcademicCycle() {
        return academicCycle;
    }
}
