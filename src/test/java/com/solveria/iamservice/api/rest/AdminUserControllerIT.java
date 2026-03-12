package com.solveria.iamservice.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solveria.iamservice.api.rest.dto.AdminUserCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminUserResponse;
import com.solveria.iamservice.application.service.AdminUserService;
import com.solveria.iamservice.config.security.JwtService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminUserService adminUserService;

    @Test
    void createUser_WithoutToken_Returns401() throws Exception {
        AdminUserCreateRequest request =
                new AdminUserCreateRequest(
                        "test.admin",
                        "test.admin@solveria.local",
                        "SecurePass123",
                        "ACADEMIC_ADMIN",
                        null,
                        Set.of("PLATFORM_ADMIN"));

        mockMvc.perform(
                        post("/api/v1/admin/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(adminUserService);
    }

    @Test
    void createUser_WithNonPlatformAdminRole_Returns403() throws Exception {
        AdminUserCreateRequest request =
                new AdminUserCreateRequest(
                        "test.admin",
                        "test.admin@solveria.local",
                        "SecurePass123",
                        "ACADEMIC_ADMIN",
                        null,
                        Set.of("PLATFORM_ADMIN"));

        mockMvc.perform(
                        post("/api/v1/admin/users")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("ACADEMIC_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        verifyNoInteractions(adminUserService);
    }

    @Test
    void createUser_WithPlatformAdminRole_Returns201() throws Exception {
        AdminUserCreateRequest request =
                new AdminUserCreateRequest(
                        "test.admin",
                        "test.admin@solveria.local",
                        "SecurePass123",
                        "ACADEMIC_ADMIN",
                        null,
                        Set.of("PLATFORM_ADMIN"));

        AdminUserResponse response =
                new AdminUserResponse(
                        5L,
                        "test.admin",
                        "test.admin@solveria.local",
                        "ACADEMIC_ADMIN",
                        true,
                        null,
                        Set.of("PLATFORM_ADMIN"),
                        LocalDateTime.now(),
                        null);
        when(adminUserService.createUser(any(AdminUserCreateRequest.class))).thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/admin/users")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.roleNames[0]").value("PLATFORM_ADMIN"));
    }

    @Test
    void listUsers_WithPlatformAdminRole_Returns200() throws Exception {
        when(adminUserService.listUsers(null))
                .thenReturn(
                        List.of(
                                new AdminUserResponse(
                                        1L,
                                        "platform.admin",
                                        "admin@solveria.local",
                                        "ACADEMIC_ADMIN",
                                        true,
                                        null,
                                        Set.of("PLATFORM_ADMIN"),
                                        LocalDateTime.now(),
                                        null)));

        mockMvc.perform(
                        get("/api/v1/admin/users")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@solveria.local"));
    }

    @Test
    void deactivateUser_WithPlatformAdminRole_Returns204() throws Exception {
        mockMvc.perform(
                        delete("/api/v1/admin/users/7")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    private String tokenForRoles(List<String> roles) {
        return jwtService.generateToken(
                "admin@solveria.local", 1L, Map.of("roles", roles, "scopes", List.of()));
    }
}
