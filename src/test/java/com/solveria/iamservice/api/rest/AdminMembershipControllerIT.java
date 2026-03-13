package com.solveria.iamservice.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solveria.core.shared.exceptions.BusinessRuleViolationException;
import com.solveria.iamservice.api.rest.dto.AdminMembershipCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminMembershipResponse;
import com.solveria.iamservice.application.service.AdminMembershipService;
import com.solveria.iamservice.config.security.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
class AdminMembershipControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminMembershipService adminMembershipService;

    @Test
    void createMembership_WithoutToken_Returns401() throws Exception {
        AdminMembershipCreateRequest request =
                new AdminMembershipCreateRequest(
                        10L, "11111111-1111-1111-1111-111111111111", "ACTIVE", true);

        mockMvc.perform(
                        post("/api/v1/admin/memberships")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(adminMembershipService);
    }

    @Test
    void createMembership_WithPlatformAdminRole_Returns201() throws Exception {
        AdminMembershipCreateRequest request =
                new AdminMembershipCreateRequest(
                        10L, "11111111-1111-1111-1111-111111111111", "ACTIVE", true);
        AdminMembershipResponse response =
                new AdminMembershipResponse(
                        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        10L,
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres",
                        "UNIVERSITY",
                        "PRIMARY",
                        true,
                        "ACTIVE",
                        Instant.now());

        when(adminMembershipService.createMembership(any(AdminMembershipCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/admin/memberships")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipType").value("PRIMARY"))
                .andExpect(jsonPath("$.isPrimary").value(true));
    }

    @Test
    void createMembership_WithNonPlatformAdminRole_Returns403() throws Exception {
        AdminMembershipCreateRequest request =
                new AdminMembershipCreateRequest(
                        10L, "11111111-1111-1111-1111-111111111111", "ACTIVE", true);

        mockMvc.perform(
                        post("/api/v1/admin/memberships")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("ACADEMIC_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void listUserMemberships_WithPlatformAdminRole_Returns200() throws Exception {
        Long userId = 10L;
        when(adminMembershipService.listMembershipsByUser(eq(userId), eq(null)))
                .thenReturn(
                        List.of(
                                new AdminMembershipResponse(
                                        "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                                        userId,
                                        "11111111-1111-1111-1111-111111111111",
                                        "umsa",
                                        "Universidad Mayor de San Andres",
                                        "UNIVERSITY",
                                        "PRIMARY",
                                        true,
                                        "ACTIVE",
                                        Instant.now())));

        mockMvc.perform(
                        get("/api/v1/admin/users/{userId}/memberships", userId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantCode").value("umsa"))
                .andExpect(jsonPath("$[0].isPrimary").value(true));
    }

    @Test
    void createMembership_WithInvalidPayload_ReturnsNormalizedValidationError() throws Exception {
        String invalidRequest =
                """
                {
                  "userId": null,
                  "tenantId": "",
                  "status": "BAD_STATUS",
                  "isPrimary": true
                }
                """;

        mockMvc.perform(
                        post("/api/v1/admin/memberships")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/memberships"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.details.userId").exists())
                .andExpect(jsonPath("$.details.tenantId").exists())
                .andExpect(jsonPath("$.details.status").exists());

        verifyNoInteractions(adminMembershipService);
    }

    @Test
    void createMembership_WhenServiceRejectsRequest_ReturnsNormalizedBusinessError()
            throws Exception {
        AdminMembershipCreateRequest request =
                new AdminMembershipCreateRequest(
                        10L, "11111111-1111-1111-1111-111111111111", "ACTIVE", true);
        when(adminMembershipService.createMembership(any(AdminMembershipCreateRequest.class)))
                .thenThrow(new BusinessRuleViolationException("membership.already.exists"));

        mockMvc.perform(
                        post("/api/v1/admin/memberships")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("error.business.rule.violation"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/admin/memberships"));
    }

    private String tokenForRoles(List<String> roles) {
        return jwtService.generateToken(
                "admin@solveria.local", 1L, Map.of("roles", roles, "scopes", List.of()));
    }
}
