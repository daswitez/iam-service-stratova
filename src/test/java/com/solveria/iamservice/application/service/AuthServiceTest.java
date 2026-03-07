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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepositoryPort userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = new User("testuser", "test@example.com", "encodedPassword", "STUDENT", true);
        activeUser.setTenantId("tenant-1");

        inactiveUser =
                new User("inactive", "inactive@example.com", "encodedPassword", "STUDENT", false);
        inactiveUser.setTenantId("tenant-1");
    }

    @Test
    void register_Success() {
        RegisterRequest request =
                new RegisterRequest(
                        "newuser", "new@example.com", "password", "STUDENT", "tenant-1", null);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");

        User savedUser = new User("newuser", "new@example.com", "hashedPassword", "STUDENT", true);
        savedUser.setTenantId("tenant-1");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtService.generateToken(anyString(), any(), anyString(), any()))
                .thenReturn("mockJwtToken");

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

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");

        User savedUser = new User("newuser", "new@example.com", "hashedPassword", "STUDENT", true);
        savedUser.setTenantId("system-12345");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtService.generateToken(anyString(), any(), anyString(), any()))
                .thenReturn("mockJwtToken");

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
        when(jwtService.generateToken(anyString(), any(), anyString(), any()))
                .thenReturn("mockJwtToken");

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
