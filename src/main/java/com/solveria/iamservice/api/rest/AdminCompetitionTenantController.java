package com.solveria.iamservice.api.rest;

import com.solveria.iamservice.api.rest.dto.AdminCompetitionTenantCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionTenantResponse;
import com.solveria.iamservice.application.service.AdminCompetitionTenantService;
import com.solveria.iamservice.config.security.PlatformAdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Admin Competition Tenants",
        description = "Administrative competition participant tenant management")
@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/tenants")
@PlatformAdminOnly
public class AdminCompetitionTenantController {

    private static final Logger log =
            LoggerFactory.getLogger(AdminCompetitionTenantController.class);

    private final AdminCompetitionTenantService adminCompetitionTenantService;

    public AdminCompetitionTenantController(
            AdminCompetitionTenantService adminCompetitionTenantService) {
        this.adminCompetitionTenantService = adminCompetitionTenantService;
    }

    @Operation(
            summary = "Add participant tenant",
            description = "Adds a tenant as a participant in a competition.")
    @PostMapping
    public ResponseEntity<AdminCompetitionTenantResponse> addParticipantTenant(
            @PathVariable String competitionId,
            @Valid @RequestBody AdminCompetitionTenantCreateRequest request) {
        log.info(
                "event=ADMIN_COMPETITION_TENANT_CREATE_REQUEST competitionId={} tenantId={}",
                competitionId,
                request.tenantId());
        AdminCompetitionTenantResponse response =
                adminCompetitionTenantService.addParticipantTenant(competitionId, request);
        return ResponseEntity.created(
                        URI.create(
                                "/api/v1/admin/competitions/"
                                        + competitionId
                                        + "/tenants/"
                                        + response.tenantId()))
                .body(response);
    }

    @Operation(
            summary = "List participant tenants",
            description = "Lists all participant tenants in a competition.")
    @GetMapping
    public ResponseEntity<List<AdminCompetitionTenantResponse>> listParticipantTenants(
            @PathVariable String competitionId) {
        return ResponseEntity.ok(
                adminCompetitionTenantService.listParticipantTenants(competitionId));
    }

    @Operation(
            summary = "Remove participant tenant",
            description = "Removes a tenant from a competition.")
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> removeParticipantTenant(
            @PathVariable String competitionId, @PathVariable String tenantId) {
        log.info(
                "event=ADMIN_COMPETITION_TENANT_DELETE_REQUEST competitionId={} tenantId={}",
                competitionId,
                tenantId);
        adminCompetitionTenantService.removeParticipantTenant(competitionId, tenantId);
        return ResponseEntity.noContent().build();
    }
}
