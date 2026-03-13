package com.solveria.iamservice.api.rest;

import com.solveria.iamservice.api.rest.dto.AdminUserCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminUserResponse;
import com.solveria.iamservice.api.rest.dto.AdminUserUpdateRequest;
import com.solveria.iamservice.application.service.AdminUserService;
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

@Tag(name = "Admin Users", description = "Administrative user lifecycle management")
@RestController
@RequestMapping("/api/v1/admin/users")
@PlatformAdminOnly
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "Create user", description = "Creates a new user account.")
    @PostMapping
    public ResponseEntity<AdminUserResponse> createUser(
            @Valid @RequestBody AdminUserCreateRequest request) {
        log.info(
                "event=ADMIN_USER_CREATE_REQUEST email={} category={}",
                request.email(),
                request.userCategory());
        AdminUserResponse response = adminUserService.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + response.id()))
                .body(response);
    }

    @Operation(
            summary = "List users",
            description = "Lists users, optionally filtered by active state.")
    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listUsers(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(adminUserService.listUsers(active));
    }

    @Operation(summary = "Get user", description = "Gets a user by id.")
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUser(id));
    }

    @Operation(summary = "Update user", description = "Updates an existing user account.")
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest request) {
        log.info("event=ADMIN_USER_UPDATE_REQUEST userId={} email={}", id, request.email());
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @Operation(
            summary = "Deactivate user",
            description = "Performs a logical deactivation of a user.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        log.info("event=ADMIN_USER_DEACTIVATE_REQUEST userId={}", id);
        adminUserService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }
}
