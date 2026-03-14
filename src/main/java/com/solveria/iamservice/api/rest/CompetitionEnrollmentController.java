package com.solveria.iamservice.api.rest;

import com.solveria.iamservice.api.rest.dto.CompetitionEnrollmentResponse;
import com.solveria.iamservice.api.rest.dto.CompetitionEnrollmentStatusUpdateRequest;
import com.solveria.iamservice.api.rest.dto.CompetitionStaffEnrollmentCreateRequest;
import com.solveria.iamservice.application.service.CompetitionEnrollmentService;
import com.solveria.iamservice.config.security.PlatformAdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Competition Enrollments", description = "Competition enrollment management")
@RestController
@RequestMapping("/api/v1/competitions/{competitionId}/enrollments")
@PlatformAdminOnly
public class CompetitionEnrollmentController {

    private static final Logger log =
            LoggerFactory.getLogger(CompetitionEnrollmentController.class);

    private final CompetitionEnrollmentService competitionEnrollmentService;

    public CompetitionEnrollmentController(
            CompetitionEnrollmentService competitionEnrollmentService) {
        this.competitionEnrollmentService = competitionEnrollmentService;
    }

    @Operation(
            summary = "Create staff enrollment",
            description = "Enrolls a judge, mentor, investor, or manager in a competition.")
    @PostMapping("/staff")
    public ResponseEntity<CompetitionEnrollmentResponse> createStaffEnrollment(
            @PathVariable String competitionId,
            @Valid @RequestBody CompetitionStaffEnrollmentCreateRequest request) {
        log.info(
                "event=COMPETITION_STAFF_ENROLLMENT_CREATE_REQUEST competitionId={} userId={} originTenantId={} participantType={}",
                competitionId,
                request.userId(),
                request.originTenantId(),
                request.participantType());
        CompetitionEnrollmentResponse response =
                competitionEnrollmentService.createStaffEnrollment(competitionId, request);
        return ResponseEntity.created(
                        URI.create(
                                "/api/v1/competitions/"
                                        + competitionId
                                        + "/enrollments/"
                                        + response.id()))
                .body(response);
    }

    @Operation(summary = "Get enrollment", description = "Gets a competition enrollment by id.")
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<CompetitionEnrollmentResponse> getEnrollment(
            @PathVariable String competitionId, @PathVariable String enrollmentId) {
        return ResponseEntity.ok(
                competitionEnrollmentService.getEnrollment(competitionId, enrollmentId));
    }

    @Operation(
            summary = "Update enrollment status",
            description = "Updates the status of an existing competition enrollment.")
    @PatchMapping("/{enrollmentId}/status")
    public ResponseEntity<CompetitionEnrollmentResponse> updateEnrollmentStatus(
            @PathVariable String competitionId,
            @PathVariable String enrollmentId,
            @Valid @RequestBody CompetitionEnrollmentStatusUpdateRequest request) {
        log.info(
                "event=COMPETITION_ENROLLMENT_STATUS_UPDATE_REQUEST competitionId={} enrollmentId={} status={}",
                competitionId,
                enrollmentId,
                request.status());
        return ResponseEntity.ok(
                competitionEnrollmentService.updateEnrollmentStatus(
                        competitionId, enrollmentId, request));
    }

    @Operation(
            summary = "Withdraw enrollment",
            description = "Performs a logical removal by setting enrollment status to WITHDRAWN.")
    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<Void> withdrawEnrollment(
            @PathVariable String competitionId, @PathVariable String enrollmentId) {
        log.info(
                "event=COMPETITION_ENROLLMENT_WITHDRAW_REQUEST competitionId={} enrollmentId={}",
                competitionId,
                enrollmentId);
        competitionEnrollmentService.withdrawEnrollment(competitionId, enrollmentId);
        return ResponseEntity.noContent().build();
    }
}
