package com.solveria.iamservice.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminCompetitionResponse;
import com.solveria.iamservice.application.service.AdminCompetitionService;
import com.solveria.iamservice.config.security.JwtService;
import java.math.BigDecimal;
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
class AdminCompetitionControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminCompetitionService adminCompetitionService;

    @Test
    void createCompetition_WithoutToken_Returns401() throws Exception {
        AdminCompetitionCreateRequest request = validRequest();

        mockMvc.perform(
                        post("/api/v1/admin/competitions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(adminCompetitionService);
    }

    @Test
    void createCompetition_WithPlatformAdminRole_Returns201() throws Exception {
        when(adminCompetitionService.createCompetition(any(AdminCompetitionCreateRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(
                        post("/api/v1/admin/competitions")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("biz-sim-2026"))
                .andExpect(jsonPath("$.hostTenantCode").value("umsa"))
                .andExpect(jsonPath("$.teamCreationMode").value("ADMIN_MANAGED"));
    }

    @Test
    void createCompetition_WithNonPlatformAdminRole_Returns403() throws Exception {
        mockMvc.perform(
                        post("/api/v1/admin/competitions")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("ACADEMIC_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void listCompetitions_WithPlatformAdminRole_Returns200() throws Exception {
        when(adminCompetitionService.listCompetitions(null, null, null))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(
                        get("/api/v1/admin/competitions")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Business Simulation 2026"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void listCompetitions_WithFilters_Returns200() throws Exception {
        when(adminCompetitionService.listCompetitions(
                        "ACTIVE", "11111111-1111-1111-1111-111111111111", "2026-S1"))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(
                        get("/api/v1/admin/competitions")
                                .param("status", "ACTIVE")
                                .param("hostTenantId", "11111111-1111-1111-1111-111111111111")
                                .param("cycle", "2026-S1")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("biz-sim-2026"))
                .andExpect(jsonPath("$[0].hostTenantCode").value("umsa"));
    }

    @Test
    void createCompetition_WithInvalidPayload_ReturnsNormalizedValidationError() throws Exception {
        String invalidRequest =
                """
                {
                  "code": "",
                  "name": "",
                  "scope": "INVALID",
                  "hostTenantId": "",
                  "productName": "",
                  "industryCode": "",
                  "industryName": "",
                  "initialCapitalAmount": 0,
                  "minTeamSize": null,
                  "maxTeamSize": null,
                  "roleAssignmentMethod": "BAD",
                  "allowOptionalCoo": null
                }
                """;

        mockMvc.perform(
                        post("/api/v1/admin/competitions")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/competitions"))
                .andExpect(jsonPath("$.details.code").exists())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.scope").exists())
                .andExpect(jsonPath("$.details.hostTenantId").exists())
                .andExpect(jsonPath("$.details.productName").exists())
                .andExpect(jsonPath("$.details.industryCode").exists())
                .andExpect(jsonPath("$.details.industryName").exists())
                .andExpect(jsonPath("$.details.initialCapitalAmount").exists())
                .andExpect(jsonPath("$.details.minTeamSize").exists())
                .andExpect(jsonPath("$.details.maxTeamSize").exists())
                .andExpect(jsonPath("$.details.roleAssignmentMethod").exists())
                .andExpect(jsonPath("$.details.allowOptionalCoo").exists());

        verifyNoInteractions(adminCompetitionService);
    }

    private AdminCompetitionCreateRequest validRequest() {
        return new AdminCompetitionCreateRequest(
                "biz-sim-2026",
                "Business Simulation 2026",
                "Competencia interuniversitaria",
                "CROSS_TENANT",
                "11111111-1111-1111-1111-111111111111",
                "Smart Retail",
                "retail-tech",
                "Retail Technology",
                new BigDecimal("100000.00"),
                4,
                6,
                "ADMIN_ASSIGNMENT",
                true,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-06-01T00:00:00Z"));
    }

    private AdminCompetitionResponse sampleResponse() {
        return new AdminCompetitionResponse(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                "biz-sim-2026",
                "Business Simulation 2026",
                "Competencia interuniversitaria",
                "CROSS_TENANT",
                "DRAFT",
                "11111111-1111-1111-1111-111111111111",
                "umsa",
                "Universidad Mayor de San Andres",
                "Smart Retail",
                "retail-tech",
                "Retail Technology",
                new BigDecimal("100000.00"),
                "USD",
                4,
                6,
                "ADMIN_MANAGED",
                "ADMIN_ASSIGNMENT",
                true,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.now());
    }

    private String tokenForRoles(List<String> roles) {
        return jwtService.generateToken(
                "admin@solveria.local", 1L, Map.of("roles", roles, "scopes", List.of()));
    }
}
