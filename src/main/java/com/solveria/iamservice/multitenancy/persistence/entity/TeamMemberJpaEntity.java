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
        name = "iam_team_member",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_iam_team_member",
                        columnNames = {"team_id", "user_id"}))
public class TeamMemberJpaEntity {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamJpaEntity team;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 32)
    private TeamMemberRole memberRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TeamMemberJpaEntity() {}

    public TeamMemberJpaEntity(TeamJpaEntity team, Long userId, TeamMemberRole memberRole) {
        this.team = team;
        this.userId = userId;
        this.memberRole = memberRole;
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

    public TeamJpaEntity getTeam() {
        return team;
    }

    public Long getUserId() {
        return userId;
    }

    public TeamMemberRole getMemberRole() {
        return memberRole;
    }
}
