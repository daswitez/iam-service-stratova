package com.solveria.iamservice.api.rest;

import com.solveria.iamservice.api.exception.ErrorCodes;
import com.solveria.iamservice.api.rest.dto.AuthResponse;
import com.solveria.iamservice.api.rest.dto.LoginRequest;
import com.solveria.iamservice.api.rest.dto.RegisterRequest;
import com.solveria.iamservice.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Administrative multi-tenant login")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Public registration disabled",
            description =
                    "Public self-registration is disabled. User accounts must be created by an authenticated administrator.")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info(
                "event=PUBLIC_REGISTER_BLOCKED email={} category={} tenantId={}",
                request.email(),
                request.userCategory(),
                request.tenantId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        Map.of(
                                "errorCode",
                                ErrorCodes.FORBIDDEN,
                                "message",
                                "Public registration is disabled. An authenticated administrator must create users."));
    }

    @Operation(
            summary = "Login",
            description = "Authenticates a user and returns a JWT token with multi-tenant claims.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("event=LOGIN_REQUEST email={}", request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
