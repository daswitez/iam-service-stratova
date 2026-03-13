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
import com.solveria.iamservice.api.rest.dto.AdminCompetitionTenantCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionTenantResponse;
import com.solveria.iamservice.application.service.AdminCompetitionTenantService;
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
class AdminCompetitionTenantControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminCompetitionTenantService adminCompetitionTenantService;

    @Test
    void addParticipantTenant_WithoutToken_Returns401() throws Exception {
        String competitionId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        AdminCompetitionTenantCreateRequest request =
                new AdminCompetitionTenantCreateRequest("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(
                        post("/api/v1/admin/competitions/{competitionId}/tenants", competitionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(adminCompetitionTenantService);
    }

    @Test
    void addParticipantTenant_WithPlatformAdminRole_Returns201() throws Exception {
        String competitionId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        AdminCompetitionTenantCreateRequest request =
                new AdminCompetitionTenantCreateRequest("22222222-2222-2222-2222-222222222222");
        when(adminCompetitionTenantService.addParticipantTenant(
                        eq(competitionId), any(AdminCompetitionTenantCreateRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(
                        post("/api/v1/admin/competitions/{competitionId}/tenants", competitionId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.competitionCode").value("biz-sim-2026"))
                .andExpect(jsonPath("$.tenantCode").value("ucb"));
    }

    @Test
    void addParticipantTenant_WithNonPlatformAdminRole_Returns403() throws Exception {
        String competitionId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        AdminCompetitionTenantCreateRequest request =
                new AdminCompetitionTenantCreateRequest("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(
                        post("/api/v1/admin/competitions/{competitionId}/tenants", competitionId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("ACADEMIC_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void listParticipantTenants_WithPlatformAdminRole_Returns200() throws Exception {
        String competitionId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        when(adminCompetitionTenantService.listParticipantTenants(competitionId))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(
                        get("/api/v1/admin/competitions/{competitionId}/tenants", competitionId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantName").value("Universidad Catolica Boliviana"));
    }

    @Test
    void addParticipantTenant_WithInvalidPayload_ReturnsNormalizedValidationError()
            throws Exception {
        String competitionId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        String invalidRequest =
                """
                {
                  "tenantId": ""
                }
                """;

        mockMvc.perform(
                        post("/api/v1/admin/competitions/{competitionId}/tenants", competitionId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/v1/admin/competitions/" + competitionId + "/tenants"))
                .andExpect(jsonPath("$.details.tenantId").exists());

        verifyNoInteractions(adminCompetitionTenantService);
    }

    private AdminCompetitionTenantResponse sampleResponse() {
        return new AdminCompetitionTenantResponse(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "biz-sim-2026",
                "22222222-2222-2222-2222-222222222222",
                "ucb",
                "Universidad Catolica Boliviana",
                "UNIVERSITY",
                "ACTIVE",
                Instant.now());
    }

    private String tokenForRoles(List<String> roles) {
        return jwtService.generateToken(
                "admin@solveria.local", 1L, Map.of("roles", roles, "scopes", List.of()));
    }
}
