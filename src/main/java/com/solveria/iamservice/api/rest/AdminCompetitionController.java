package com.solveria.iamservice.api.rest;

import com.solveria.iamservice.api.rest.dto.AdminCompetitionCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionResponse;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionUpdateRequest;
import com.solveria.iamservice.application.service.AdminCompetitionService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Competitions", description = "Administrative competition management")
@RestController
@RequestMapping("/api/v1/admin/competitions")
@PlatformAdminOnly
public class AdminCompetitionController {

    private static final Logger log = LoggerFactory.getLogger(AdminCompetitionController.class);

    private final AdminCompetitionService adminCompetitionService;

    public AdminCompetitionController(AdminCompetitionService adminCompetitionService) {
        this.adminCompetitionService = adminCompetitionService;
    }

    @Operation(summary = "Create competition", description = "Creates a new competition.")
    @PostMapping
    public ResponseEntity<AdminCompetitionResponse> createCompetition(
            @Valid @RequestBody AdminCompetitionCreateRequest request) {
        log.info(
                "event=ADMIN_COMPETITION_CREATE_REQUEST code={} hostTenantId={}",
                request.code(),
                request.hostTenantId());
        AdminCompetitionResponse response = adminCompetitionService.createCompetition(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/competitions/" + response.id()))
                .body(response);
    }

    @Operation(
            summary = "List competitions",
            description = "Lists competitions, optionally filtered by status.")
    @GetMapping
    public ResponseEntity<List<AdminCompetitionResponse>> listCompetitions(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminCompetitionService.listCompetitions(status));
    }

    @Operation(summary = "Get competition", description = "Gets a competition by id.")
    @GetMapping("/{id}")
    public ResponseEntity<AdminCompetitionResponse> getCompetition(@PathVariable String id) {
        return ResponseEntity.ok(adminCompetitionService.getCompetition(id));
    }

    @Operation(summary = "Update competition", description = "Updates a competition.")
    @PutMapping("/{id}")
    public ResponseEntity<AdminCompetitionResponse> updateCompetition(
            @PathVariable String id, @Valid @RequestBody AdminCompetitionUpdateRequest request) {
        log.info("event=ADMIN_COMPETITION_UPDATE_REQUEST id={} code={}", id, request.code());
        return ResponseEntity.ok(adminCompetitionService.updateCompetition(id, request));
    }

    @Operation(
            summary = "Archive competition",
            description = "Performs a logical archive of a competition.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveCompetition(@PathVariable String id) {
        log.info("event=ADMIN_COMPETITION_ARCHIVE_REQUEST id={}", id);
        adminCompetitionService.archiveCompetition(id);
        return ResponseEntity.noContent().build();
    }
}
