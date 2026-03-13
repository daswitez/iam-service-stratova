package com.solveria.iamservice.api.rest;

import com.solveria.iamservice.api.rest.dto.AdminSubTenantCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminSubTenantResponse;
import com.solveria.iamservice.api.rest.dto.AdminSubTenantUpdateRequest;
import com.solveria.iamservice.application.service.AdminSubTenantService;
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

@Tag(name = "Admin Sub-Tenants", description = "Administrative faculty and program management")
@RestController
@RequestMapping("/api/v1/admin")
@PlatformAdminOnly
public class AdminSubTenantController {

    private static final Logger log = LoggerFactory.getLogger(AdminSubTenantController.class);

    private final AdminSubTenantService adminSubTenantService;

    public AdminSubTenantController(AdminSubTenantService adminSubTenantService) {
        this.adminSubTenantService = adminSubTenantService;
    }

    @Operation(
            summary = "Create child tenant",
            description = "Creates a FACULTY under a UNIVERSITY or a PROGRAM under a FACULTY.")
    @PostMapping("/tenants/{parentId}/children")
    public ResponseEntity<AdminSubTenantResponse> createChildTenant(
            @PathVariable String parentId,
            @Valid @RequestBody AdminSubTenantCreateRequest request) {
        log.info(
                "event=ADMIN_SUB_TENANT_CREATE_REQUEST parentId={} type={} code={}",
                parentId,
                request.type(),
                request.code());
        AdminSubTenantResponse response = adminSubTenantService.createSubTenant(parentId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/sub-tenants/" + response.id()))
                .body(response);
    }

    @Operation(
            summary = "List child tenants",
            description = "Lists children of a tenant, filtered by type and status when provided.")
    @GetMapping("/tenants/{parentId}/children")
    public ResponseEntity<List<AdminSubTenantResponse>> listChildren(
            @PathVariable String parentId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminSubTenantService.listChildren(parentId, type, status));
    }

    @Operation(summary = "Get sub-tenant", description = "Gets a FACULTY or PROGRAM by id.")
    @GetMapping("/sub-tenants/{id}")
    public ResponseEntity<AdminSubTenantResponse> getSubTenant(@PathVariable String id) {
        return ResponseEntity.ok(adminSubTenantService.getSubTenant(id));
    }

    @Operation(
            summary = "Update sub-tenant",
            description = "Updates code and name of a FACULTY or PROGRAM.")
    @PutMapping("/sub-tenants/{id}")
    public ResponseEntity<AdminSubTenantResponse> updateSubTenant(
            @PathVariable String id, @Valid @RequestBody AdminSubTenantUpdateRequest request) {
        log.info("event=ADMIN_SUB_TENANT_UPDATE_REQUEST id={} code={}", id, request.code());
        return ResponseEntity.ok(adminSubTenantService.updateSubTenant(id, request));
    }

    @Operation(
            summary = "Deactivate sub-tenant",
            description = "Performs a logical deactivation of a FACULTY or PROGRAM.")
    @DeleteMapping("/sub-tenants/{id}")
    public ResponseEntity<Void> deactivateSubTenant(@PathVariable String id) {
        log.info("event=ADMIN_SUB_TENANT_DEACTIVATE_REQUEST id={}", id);
        adminSubTenantService.deactivateSubTenant(id);
        return ResponseEntity.noContent().build();
    }
}
