package com.solveria.iamservice.multitenancy.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "iam_academic_cycle")
public class AcademicCycleJpaEntity {

    @Id private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_tenant_id", nullable = false)
    private TenantJpaEntity ownerTenant;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AcademicCycleJpaEntity() {}

    public AcademicCycleJpaEntity(
            String code,
            String name,
            TenantJpaEntity ownerTenant,
            LocalDate startDate,
            LocalDate endDate) {
        this.code = code;
        this.name = name;
        this.ownerTenant = ownerTenant;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public TenantJpaEntity getOwnerTenant() {
        return ownerTenant;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
