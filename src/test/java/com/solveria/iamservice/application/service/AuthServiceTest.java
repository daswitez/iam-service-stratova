package com.solveria.iamservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.solveria.core.iam.application.port.UserRepositoryPort;
import com.solveria.core.iam.domain.model.User;
import com.solveria.iamservice.api.rest.dto.AuthResponse;
import com.solveria.iamservice.api.rest.dto.LoginRequest;
import com.solveria.iamservice.api.rest.dto.RegisterRequest;
import com.solveria.iamservice.application.exception.InactiveUserException;
import com.solveria.iamservice.application.exception.InvalidCredentialsException;
import com.solveria.iamservice.application.exception.UserAlreadyExistsException;
import com.solveria.iamservice.config.security.JwtService;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantJpaEntity;
import com.solveria.iamservice.multitenancy.persistence.entity.TenantType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TenantProvisioningService tenantProvisioningService;
    @Mock private UserContextService userContextService;

    @InjectMocks private AuthService authService;

    private User activeUser;
    private User inactiveUser;
    private UserSessionContext sessionContext;
    private TenantJpaEntity primaryTenant;

    @BeforeEach
    void setUp() {
        activeUser = new User("testuser", "test@example.com", "encodedPassword", "STUDENT", true);
        activeUser.setTenantId("tenant-1");

        inactiveUser =
                new User("inactive", "inactive@example.com", "encodedPassword", "STUDENT", false);
        inactiveUser.setTenantId("tenant-1");

        sessionContext =
                new UserSessionContext(
                        "tenant-1",
                        "tenant-1",
                        java.util.Set.of("STUDENT"),
                        java.util.List.of(),
                        java.util.List.of());

        primaryTenant = new TenantJpaEntity("tenant-1", "Tenant 1", TenantType.UNIVERSITY, null);
        ReflectionTestUtils.setField(primaryTenant, "id", UUID.randomUUID());
    }

    @Test
    void register_Success() {
        RegisterRequest request =
                new RegisterRequest(
                        "newuser", "new@example.com", "password", "STUDENT", "tenant-1", null);

        when(tenantProvisioningService.resolveOrCreateTenant(
                        request.tenantId(),
                        request.tenantName(),
                        request.userCategory(),
                        request.username()))
                .thenReturn(primaryTenant);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");

        User savedUser =
                new User(
                        1L,
                        "newuser",
                        "new@example.com",
                        "hashedPassword",
                        "STUDENT",
                        true,
                        java.util.Set.of(),
                        "tenant-1",
                        null,
                        null,
                        null,
                        null,
                        null);
        savedUser.setTenantId("tenant-1");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userContextService.buildContext(savedUser)).thenReturn(sessionContext);

        when(jwtService.generateToken(anyString(), any(), any())).thenReturn("mockJwtToken");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mockJwtToken");
        assertThat(response.user().email()).isEqualTo("new@example.com");
        assertThat(response.user().primaryTenantId()).isEqualTo("tenant-1");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_Fail_EmailTaken() {
        RegisterRequest request =
                new RegisterRequest(
                        "newuser", "test@example.com", "password", "STUDENT", "tenant-1", null);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_Success_GeneratesTenantIdIfNull() {
        RegisterRequest request =
                new RegisterRequest(
                        "newuser", "new@example.com", "password", "STUDENT", null, null);

        when(tenantProvisioningService.resolveOrCreateTenant(
                        request.tenantId(),
                        request.tenantName(),
                        request.userCategory(),
                        request.username()))
                .thenReturn(primaryTenant);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");

        User savedUser =
                new User(
                        1L,
                        "newuser",
                        "new@example.com",
                        "hashedPassword",
                        "STUDENT",
                        true,
                        java.util.Set.of(),
                        UUID.randomUUID().toString(),
                        null,
                        null,
                        null,
                        null,
                        null);
        savedUser.setTenantId("system-12345");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userContextService.buildContext(savedUser))
                .thenReturn(
                        new UserSessionContext(
                                "system-12345",
                                "system-12345",
                                java.util.Set.of("STUDENT"),
                                java.util.List.of(),
                                java.util.List.of()));

        when(jwtService.generateToken(anyString(), any(), any())).thenReturn("mockJwtToken");

        AuthResponse response = authService.register(request);

        assertThat(response.user().primaryTenantId()).startsWith("system-");
    }

    // Login Tests
    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(request.password(), activeUser.getPassword()))
                .thenReturn(true);
        when(userContextService.buildContext(activeUser)).thenReturn(sessionContext);
        when(jwtService.generateToken(anyString(), any(), any())).thenReturn("mockJwtToken");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mockJwtToken");
        assertThat(response.user().email()).isEqualTo("test@example.com");
    }

    @Test
    void login_Fail_InvalidEmail() {
        LoginRequest request = new LoginRequest("wrong@example.com", "password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_Fail_InvalidPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(request.password(), activeUser.getPassword()))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_Fail_InactiveUser() {
        LoginRequest request = new LoginRequest("inactive@example.com", "password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches(request.password(), inactiveUser.getPassword()))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InactiveUserException.class);
    }
}
