package com.solveria.iamservice.application.service;

import com.solveria.core.iam.infrastructure.persistence.entity.UserJpaEntity;
import com.solveria.core.iam.infrastructure.persistence.repository.UserJpaRepository;
import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.core.shared.exceptions.EntityNotFoundException;
import com.solveria.iamservice.api.rest.dto.CompetitionEnrollmentResponse;
import com.solveria.iamservice.api.rest.dto.CompetitionEnrollmentStatusUpdateRequest;
import com.solveria.iamservice.api.rest.dto.CompetitionStaffEnrollmentCreateRequest;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionEnrollmentJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionEnrollmentStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionParticipantType;
import com.solveria.iamservice.multitenancy.persistence.entity.CompetitionTenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.MembershipStatus;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionEnrollmentJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.CompetitionTenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.TenantJpaRepository;
import com.solveria.iamservice.multitenancy.persistence.repository.UserTenantMembershipJpaRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionEnrollmentService {

    private final CompetitionEnrollmentJpaRepository competitionEnrollmentJpaRepository;
    private final CompetitionJpaRepository competitionJpaRepository;
    private final CompetitionTenantJpaRepository competitionTenantJpaRepository;
    private final TenantJpaRepository tenantJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final UserTenantMembershipJpaRepository userTenantMembershipJpaRepository;

    public CompetitionEnrollmentService(
            CompetitionEnrollmentJpaRepository competitionEnrollmentJpaRepository,
            CompetitionJpaRepository competitionJpaRepository,
            CompetitionTenantJpaRepository competitionTenantJpaRepository,
            TenantJpaRepository tenantJpaRepository,
            UserJpaRepository userJpaRepository,
            UserTenantMembershipJpaRepository userTenantMembershipJpaRepository) {
        this.competitionEnrollmentJpaRepository = competitionEnrollmentJpaRepository;
        this.competitionJpaRepository = competitionJpaRepository;
        this.competitionTenantJpaRepository = competitionTenantJpaRepository;
        this.tenantJpaRepository = tenantJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.userTenantMembershipJpaRepository = userTenantMembershipJpaRepository;
    }

    @Transactional
    public CompetitionEnrollmentResponse createStaffEnrollment(
            String competitionId, CompetitionStaffEnrollmentCreateRequest request) {
        CompetitionJpaEntity competition = getCompetition(competitionId);
        UserJpaEntity user = getUser(request.userId());
        TenantJpaEntity originTenant = getTenant(request.originTenantId());

        validateActiveMembership(user.getId(), originTenant);
        validateTenantEnabledForCompetition(competition, originTenant);

        if (competitionEnrollmentJpaRepository.existsByCompetition_IdAndUserId(
                competition.getId(), user.getId())) {
            throw new BusinessRuleViolationException("competition.enrollment.already.exists");
        }

        CompetitionEnrollmentJpaEntity enrollment =
                new CompetitionEnrollmentJpaEntity(
                        competition,
                        user.getId(),
                        originTenant,
                        parseStaffParticipantType(request.participantType()));

        CompetitionEnrollmentJpaEntity savedEnrollment =
                competitionEnrollmentJpaRepository.save(enrollment);

        return toResponse(savedEnrollment);
    }

    @Transactional(readOnly = true)
    public CompetitionEnrollmentResponse getEnrollment(String competitionId, String enrollmentId) {
        return toResponse(getEnrollmentEntity(competitionId, enrollmentId));
    }

    @Transactional
    public CompetitionEnrollmentResponse updateEnrollmentStatus(
            String competitionId,
            String enrollmentId,
            CompetitionEnrollmentStatusUpdateRequest request) {
        CompetitionEnrollmentJpaEntity enrollment =
                getEnrollmentEntity(competitionId, enrollmentId);
        CompetitionEnrollmentStatus status = parseEnrollmentStatus(request.status());

        enrollment.setStatus(status);
        if (status == CompetitionEnrollmentStatus.APPROVED && enrollment.getApprovedAt() == null) {
            enrollment.setApprovedAt(Instant.now());
        }

        return toResponse(competitionEnrollmentJpaRepository.save(enrollment));
    }

    @Transactional
    public void withdrawEnrollment(String competitionId, String enrollmentId) {
        CompetitionEnrollmentJpaEntity enrollment =
                getEnrollmentEntity(competitionId, enrollmentId);
        if (enrollment.getStatus() == CompetitionEnrollmentStatus.WITHDRAWN) {
            return;
        }

        enrollment.setStatus(CompetitionEnrollmentStatus.WITHDRAWN);
        competitionEnrollmentJpaRepository.save(enrollment);
    }

    private CompetitionJpaEntity getCompetition(String competitionId) {
        UUID parsedCompetitionId = parseUuid(competitionId, "competition.reference.invalid");
        return competitionJpaRepository
                .findById(parsedCompetitionId)
                .orElseThrow(() -> new EntityNotFoundException("Competition", competitionId));
    }

    private UserJpaEntity getUser(Long userId) {
        return userJpaRepository
                .findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(userId)));
    }

    private TenantJpaEntity getTenant(String tenantId) {
        UUID parsedTenantId = parseUuid(tenantId, "tenant.reference.invalid");
        return tenantJpaRepository
                .findById(parsedTenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));
    }

    private CompetitionEnrollmentJpaEntity getEnrollmentEntity(
            String competitionId, String enrollmentId) {
        UUID parsedCompetitionId = parseUuid(competitionId, "competition.reference.invalid");
        UUID parsedEnrollmentId =
                parseUuid(enrollmentId, "competition.enrollment.reference.invalid");
        return competitionEnrollmentJpaRepository
                .findByCompetition_IdAndId(parsedCompetitionId, parsedEnrollmentId)
                .orElseThrow(
                        () -> new EntityNotFoundException("CompetitionEnrollment", enrollmentId));
    }

    private void validateActiveMembership(Long userId, TenantJpaEntity originTenant) {
        var membership =
                userTenantMembershipJpaRepository.findByUserIdAndTenant_Id(
                        userId, originTenant.getId());
        if (membership.isEmpty() || membership.get().getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "competition.enrollment.origin.tenant.membership.invalid");
        }
    }

    private void validateTenantEnabledForCompetition(
            CompetitionJpaEntity competition, TenantJpaEntity originTenant) {
        if (competition.getOwnerTenant().getId().equals(originTenant.getId())) {
            return;
        }

        CompetitionTenantJpaEntity participantTenant =
                competitionTenantJpaRepository
                        .findByCompetition_IdAndTenant_Id(competition.getId(), originTenant.getId())
                        .orElse(null);
        if (participantTenant == null) {
            throw new BusinessRuleViolationException(
                    "competition.enrollment.origin.tenant.not.enabled");
        }
    }

    private CompetitionParticipantType parseStaffParticipantType(String value) {
        CompetitionParticipantType participantType = parseParticipantType(value);
        if (participantType == CompetitionParticipantType.COMPETITOR) {
            throw new BusinessRuleViolationException(
                    "competition.enrollment.staff.participant.type.invalid");
        }
        return participantType;
    }

    private CompetitionParticipantType parseParticipantType(String value) {
        try {
            return CompetitionParticipantType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(
                    "competition.enrollment.participant.type.invalid");
        }
    }

    private CompetitionEnrollmentStatus parseEnrollmentStatus(String value) {
        try {
            return CompetitionEnrollmentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException("competition.enrollment.status.invalid");
        }
    }

    private UUID parseUuid(String value, String errorCode) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(errorCode);
        }
    }

    private CompetitionEnrollmentResponse toResponse(CompetitionEnrollmentJpaEntity enrollment) {
        return new CompetitionEnrollmentResponse(
                enrollment.getId().toString(),
                enrollment.getCompetition().getId().toString(),
                enrollment.getCompetition().getCode(),
                enrollment.getUserId(),
                enrollment.getOriginTenant().getId().toString(),
                enrollment.getOriginTenant().getCode(),
                enrollment.getOriginTenant().getName(),
                enrollment.getParticipantType().name(),
                enrollment.getStatus().name(),
                enrollment.getInvitedByUserId(),
                enrollment.getApprovedAt(),
                enrollment.getCreatedAt());
    }
}
