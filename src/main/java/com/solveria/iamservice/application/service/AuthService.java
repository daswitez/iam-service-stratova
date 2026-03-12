package com.solveria.iamservice.application.service;

import com.solveria.core.iam.application.port.UserRepositoryPort;
import com.solveria.core.iam.domain.model.User;
import com.solveria.iamservice.api.rest.dto.AuthResponse;
import com.solveria.iamservice.api.rest.dto.LoginRequest;
import com.solveria.iamservice.api.rest.dto.RegisterRequest;
import com.solveria.iamservice.config.security.JwtService;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantProvisioningService tenantProvisioningService;
    private final UserContextService userContextService;

    public AuthService(
            UserRepositoryPort userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TenantProvisioningService tenantProvisioningService,
            UserContextService userContextService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tenantProvisioningService = tenantProvisioningService;
        this.userContextService = userContextService;
    }

    /** Registers a new user with multi-tenant support. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            throw new com.solveria.iamservice.application.exception.UserAlreadyExistsException(
                    request.email());
        }

        var tenant =
                tenantProvisioningService.resolveOrCreateTenant(
                        request.tenantId(),
                        request.tenantName(),
                        request.userCategory(),
                        request.username());

        User newUser =
                new User(
                        request.username(),
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.userCategory(),
                        true);
        newUser.setTenantId(tenant.getId().toString());

        User savedUser = userRepository.save(newUser);
        tenantProvisioningService.ensurePrimaryMembership(savedUser.getId(), tenant);

        UserSessionContext context = userContextService.buildContext(savedUser);
        String token =
                jwtService.generateToken(
                        savedUser.getEmail(), savedUser.getId(), context.toClaims());

        AuthResponse.UserDto userDto =
                new AuthResponse.UserDto(
                        savedUser.getId(),
                        savedUser.getUsername(),
                        savedUser.getEmail(),
                        context.primaryTenantId(),
                        savedUser.getUserCategory(),
                        context.roles());

        AuthResponse.ContextDto contextDto =
                new AuthResponse.ContextDto(
                        context.activeTenantId(),
                        context.memberships(),
                        context.teamCompetitions());

        return new AuthResponse(token, userDto, contextDto);
    }

    /** Authenticates a user and returns a JWT token. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(
                                () ->
                                        new com.solveria.iamservice.application.exception
                                                .InvalidCredentialsException());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new com.solveria.iamservice.application.exception.InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new com.solveria.iamservice.application.exception.InactiveUserException();
        }

        UserSessionContext context = userContextService.buildContext(user);
        String token = jwtService.generateToken(user.getEmail(), user.getId(), context.toClaims());

        AuthResponse.UserDto userDto =
                new AuthResponse.UserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        context.primaryTenantId(),
                        user.getUserCategory(),
                        context.roles());

        AuthResponse.ContextDto contextDto =
                new AuthResponse.ContextDto(
                        context.activeTenantId(),
                        context.memberships(),
                        context.teamCompetitions());

        return new AuthResponse(token, userDto, contextDto);
    }
}
