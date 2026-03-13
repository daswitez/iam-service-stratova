package com.solveria.iamservice.api.rest;

import com.solveria.iamservice.api.rest.dto.AdminUniversityCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminUniversityResponse;
import com.solveria.iamservice.api.rest.dto.AdminUniversityUpdateRequest;
import com.solveria.iamservice.application.service.AdminUniversityService;
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

@Tag(name = "Admin Universities", description = "Administrative university management")
@RestController
@RequestMapping("/api/v1/admin/universities")
@PlatformAdminOnly
public class AdminUniversityController {

    private static final Logger log = LoggerFactory.getLogger(AdminUniversityController.class);

    private final AdminUniversityService adminUniversityService;

    public AdminUniversityController(AdminUniversityService adminUniversityService) {
        this.adminUniversityService = adminUniversityService;
    }

    @Operation(summary = "Create university", description = "Creates a new university tenant.")
    @PostMapping
    public ResponseEntity<AdminUniversityResponse> createUniversity(
            @Valid @RequestBody AdminUniversityCreateRequest request) {
        log.info(
                "event=ADMIN_UNIVERSITY_CREATE_REQUEST code={} name={}",
                request.code(),
                request.name());
        AdminUniversityResponse response = adminUniversityService.createUniversity(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/universities/" + response.id()))
                .body(response);
    }

    @Operation(
            summary = "List universities",
            description = "Lists universities, optionally filtered by status.")
    @GetMapping
    public ResponseEntity<List<AdminUniversityResponse>> listUniversities(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminUniversityService.listUniversities(status));
    }

    @Operation(summary = "Get university", description = "Gets a university by id.")
    @GetMapping("/{id}")
    public ResponseEntity<AdminUniversityResponse> getUniversity(@PathVariable String id) {
        return ResponseEntity.ok(adminUniversityService.getUniversity(id));
    }

    @Operation(summary = "Update university", description = "Updates a university code and name.")
    @PutMapping("/{id}")
    public ResponseEntity<AdminUniversityResponse> updateUniversity(
            @PathVariable String id, @Valid @RequestBody AdminUniversityUpdateRequest request) {
        log.info("event=ADMIN_UNIVERSITY_UPDATE_REQUEST id={} code={}", id, request.code());
        return ResponseEntity.ok(adminUniversityService.updateUniversity(id, request));
    }

    @Operation(
            summary = "Deactivate university",
            description = "Performs a logical deactivation of a university.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUniversity(@PathVariable String id) {
        log.info("event=ADMIN_UNIVERSITY_DEACTIVATE_REQUEST id={}", id);
        adminUniversityService.deactivateUniversity(id);
        return ResponseEntity.noContent().build();
    }
}
