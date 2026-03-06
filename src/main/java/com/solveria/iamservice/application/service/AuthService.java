package com.solveria.iamservice.application.service;

import com.solveria.core.iam.application.port.UserRepositoryPort;
import com.solveria.core.iam.domain.model.User;
import com.solveria.iamservice.api.rest.dto.AuthResponse;
import com.solveria.iamservice.api.rest.dto.LoginRequest;
import com.solveria.iamservice.api.rest.dto.RegisterRequest;
import com.solveria.iamservice.config.security.JwtService;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepositoryPort userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Registers a new user with multi-tenant support. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Email already taken");
        }

        // Handle tenant mapping or create a new internal tenant logic here if required
        String tenantId =
                request.tenantId() != null
                        ? request.tenantId()
                        : "system-" + System.currentTimeMillis();

        User newUser =
                new User(
                        request.username(),
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.userCategory(),
                        true);
        newUser.setTenantId(tenantId);

        User savedUser = userRepository.save(newUser);

        // Convert Roles (For now defaulting to an empty list since roles mapping can be complex)
        Set<String> roles = Collections.emptySet();

        String token =
                jwtService.generateToken(
                        savedUser.getEmail(), savedUser.getId(), savedUser.getTenantId(), roles);

        AuthResponse.UserDto userDto =
                new AuthResponse.UserDto(
                        savedUser.getId(),
                        savedUser.getUsername(),
                        savedUser.getEmail(),
                        savedUser.getTenantId(),
                        savedUser.getUserCategory(),
                        roles);

        return new AuthResponse(token, userDto);
    }

    /** Authenticates a user and returns a JWT token. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is inactive");
        }

        Set<String> roles = Collections.emptySet(); // Simplify role translation for now
        String token =
                jwtService.generateToken(user.getEmail(), user.getId(), user.getTenantId(), roles);

        AuthResponse.UserDto userDto =
                new AuthResponse.UserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getTenantId(),
                        user.getUserCategory(),
                        roles);

        return new AuthResponse(token, userDto);
    }
}
