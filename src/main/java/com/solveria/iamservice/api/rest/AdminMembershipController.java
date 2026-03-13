package com.solveria.iamservice.api.rest;

import com.solveria.iamservice.api.rest.dto.AdminMembershipCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminMembershipResponse;
import com.solveria.iamservice.api.rest.dto.AdminMembershipUpdateRequest;
import com.solveria.iamservice.application.service.AdminMembershipService;
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

@Tag(name = "Admin Memberships", description = "Administrative tenant membership management")
@RestController
@RequestMapping("/api/v1/admin")
@PlatformAdminOnly
public class AdminMembershipController {

    private static final Logger log = LoggerFactory.getLogger(AdminMembershipController.class);

    private final AdminMembershipService adminMembershipService;

    public AdminMembershipController(AdminMembershipService adminMembershipService) {
        this.adminMembershipService = adminMembershipService;
    }

    @Operation(
            summary = "Create membership",
            description =
                    "Assigns a user to a tenant and reconciles the active primary membership.")
    @PostMapping("/memberships")
    public ResponseEntity<AdminMembershipResponse> createMembership(
            @Valid @RequestBody AdminMembershipCreateRequest request) {
        log.info(
                "event=ADMIN_MEMBERSHIP_CREATE_REQUEST userId={} tenantId={} isPrimary={}",
                request.userId(),
                request.tenantId(),
                request.isPrimary());
        AdminMembershipResponse response = adminMembershipService.createMembership(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/memberships/" + response.id()))
                .body(response);
    }

    @Operation(summary = "Get membership", description = "Gets a tenant membership by id.")
    @GetMapping("/memberships/{id}")
    public ResponseEntity<AdminMembershipResponse> getMembership(@PathVariable String id) {
        return ResponseEntity.ok(adminMembershipService.getMembership(id));
    }

    @Operation(
            summary = "List user memberships",
            description = "Lists memberships for a user, optionally filtered by status.")
    @GetMapping("/users/{userId}/memberships")
    public ResponseEntity<List<AdminMembershipResponse>> listUserMemberships(
            @PathVariable Long userId, @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminMembershipService.listMembershipsByUser(userId, status));
    }

    @Operation(
            summary = "List tenant members",
            description = "Lists memberships for a tenant, optionally filtered by status.")
    @GetMapping("/tenants/{tenantId}/memberships")
    public ResponseEntity<List<AdminMembershipResponse>> listTenantMemberships(
            @PathVariable String tenantId, @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminMembershipService.listMembershipsByTenant(tenantId, status));
    }

    @Operation(
            summary = "Update membership",
            description =
                    "Updates membership status and/or primary flag, preserving a single active primary.")
    @PutMapping("/memberships/{id}")
    public ResponseEntity<AdminMembershipResponse> updateMembership(
            @PathVariable String id, @Valid @RequestBody AdminMembershipUpdateRequest request) {
        log.info(
                "event=ADMIN_MEMBERSHIP_UPDATE_REQUEST membershipId={} status={} isPrimary={}",
                id,
                request.status(),
                request.isPrimary());
        return ResponseEntity.ok(adminMembershipService.updateMembership(id, request));
    }

    @Operation(
            summary = "Deactivate membership",
            description = "Performs a logical removal by setting membership status to LEFT.")
    @DeleteMapping("/memberships/{id}")
    public ResponseEntity<Void> deactivateMembership(@PathVariable String id) {
        log.info("event=ADMIN_MEMBERSHIP_DEACTIVATE_REQUEST membershipId={}", id);
        adminMembershipService.deactivateMembership(id);
        return ResponseEntity.noContent().build();
    }
}
