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
        name = "iam_competition_enrollment",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_iam_competition_enrollment",
                        columnNames = {"competition_id", "user_id"}))
public class CompetitionEnrollmentJpaEntity {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    private CompetitionJpaEntity competition;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_tenant_id", nullable = false)
    private TenantJpaEntity originTenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 32)
    private CompetitionParticipantType participantType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CompetitionEnrollmentStatus status;

    @Column(name = "invited_by_user_id")
    private Long invitedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CompetitionEnrollmentJpaEntity() {}

    public CompetitionEnrollmentJpaEntity(
            CompetitionJpaEntity competition,
            Long userId,
            TenantJpaEntity originTenant,
            CompetitionParticipantType participantType) {
        this.competition = competition;
        this.userId = userId;
        this.originTenant = originTenant;
        this.participantType = participantType;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = CompetitionEnrollmentStatus.INVITED;
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

    public Long getUserId() {
        return userId;
    }

    public TenantJpaEntity getOriginTenant() {
        return originTenant;
    }

    public CompetitionParticipantType getParticipantType() {
        return participantType;
    }

    public void setParticipantType(CompetitionParticipantType participantType) {
        this.participantType = participantType;
    }

    public CompetitionEnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(CompetitionEnrollmentStatus status) {
        this.status = status;
    }

    public Long getInvitedByUserId() {
        return invitedByUserId;
    }

    public void setInvitedByUserId(Long invitedByUserId) {
        this.invitedByUserId = invitedByUserId;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
