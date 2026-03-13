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
import java.math.BigDecimal;
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

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "industry_code", nullable = false, length = 80)
    private String industryCode;

    @Column(name = "industry_name", nullable = false, length = 255)
    private String industryName;

    @Column(name = "initial_capital_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialCapitalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "min_team_size", nullable = false)
    private Short minTeamSize;

    @Column(name = "max_team_size", nullable = false)
    private Short maxTeamSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_creation_mode", nullable = false, length = 32)
    private TeamCreationMode teamCreationMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_assignment_method", nullable = false, length = 32)
    private CompetitionRoleAssignmentMethod roleAssignmentMethod;

    @Column(name = "allow_optional_coo", nullable = false)
    private Boolean allowOptionalCoo;

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
            AcademicCycleJpaEntity academicCycle,
            String productName,
            String industryCode,
            String industryName,
            BigDecimal initialCapitalAmount,
            Short minTeamSize,
            Short maxTeamSize,
            CompetitionRoleAssignmentMethod roleAssignmentMethod,
            Boolean allowOptionalCoo) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.scope = scope;
        this.ownerTenant = ownerTenant;
        this.academicCycle = academicCycle;
        this.productName = productName;
        this.industryCode = industryCode;
        this.industryName = industryName;
        this.initialCapitalAmount = initialCapitalAmount;
        this.minTeamSize = minTeamSize;
        this.maxTeamSize = maxTeamSize;
        this.roleAssignmentMethod = roleAssignmentMethod;
        this.allowOptionalCoo = allowOptionalCoo;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = CompetitionStatus.DRAFT;
        }
        if (currency == null) {
            currency = "USD";
        }
        if (minTeamSize == null) {
            minTeamSize = 4;
        }
        if (maxTeamSize == null) {
            maxTeamSize = 6;
        }
        if (teamCreationMode == null) {
            teamCreationMode = TeamCreationMode.ADMIN_MANAGED;
        }
        if (roleAssignmentMethod == null) {
            roleAssignmentMethod = CompetitionRoleAssignmentMethod.ADMIN_ASSIGNMENT;
        }
        if (allowOptionalCoo == null) {
            allowOptionalCoo = true;
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

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CompetitionScope getScope() {
        return scope;
    }

    public void setScope(CompetitionScope scope) {
        this.scope = scope;
    }

    public CompetitionStatus getStatus() {
        return status;
    }

    public void setStatus(CompetitionStatus status) {
        this.status = status;
    }

    public TenantJpaEntity getOwnerTenant() {
        return ownerTenant;
    }

    public void setOwnerTenant(TenantJpaEntity ownerTenant) {
        this.ownerTenant = ownerTenant;
    }

    public AcademicCycleJpaEntity getAcademicCycle() {
        return academicCycle;
    }

    public void setAcademicCycle(AcademicCycleJpaEntity academicCycle) {
        this.academicCycle = academicCycle;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public String getIndustryName() {
        return industryName;
    }

    public void setIndustryName(String industryName) {
        this.industryName = industryName;
    }

    public BigDecimal getInitialCapitalAmount() {
        return initialCapitalAmount;
    }

    public void setInitialCapitalAmount(BigDecimal initialCapitalAmount) {
        this.initialCapitalAmount = initialCapitalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Short getMinTeamSize() {
        return minTeamSize;
    }

    public void setMinTeamSize(Short minTeamSize) {
        this.minTeamSize = minTeamSize;
    }

    public Short getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(Short maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }

    public TeamCreationMode getTeamCreationMode() {
        return teamCreationMode;
    }

    public void setTeamCreationMode(TeamCreationMode teamCreationMode) {
        this.teamCreationMode = teamCreationMode;
    }

    public CompetitionRoleAssignmentMethod getRoleAssignmentMethod() {
        return roleAssignmentMethod;
    }

    public void setRoleAssignmentMethod(CompetitionRoleAssignmentMethod roleAssignmentMethod) {
        this.roleAssignmentMethod = roleAssignmentMethod;
    }

    public Boolean getAllowOptionalCoo() {
        return allowOptionalCoo;
    }

    public void setAllowOptionalCoo(Boolean allowOptionalCoo) {
        this.allowOptionalCoo = allowOptionalCoo;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(Instant startsAt) {
        this.startsAt = startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(Instant endsAt) {
        this.endsAt = endsAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
