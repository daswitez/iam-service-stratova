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
import com.solveria.iamservice.api.rest.dto.AdminSubTenantCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminSubTenantResponse;
import com.solveria.iamservice.application.service.AdminSubTenantService;
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
class AdminSubTenantControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminSubTenantService adminSubTenantService;

    @Test
    void createChildTenant_WithoutToken_Returns401() throws Exception {
        AdminSubTenantCreateRequest request =
                new AdminSubTenantCreateRequest(
                        "facultad-ingenieria", "Facultad de Ingenieria", "FACULTY");

        mockMvc.perform(
                        post("/api/v1/admin/tenants/11111111-1111-1111-1111-111111111111/children")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(adminSubTenantService);
    }

    @Test
    void createChildTenant_WithPlatformAdminRole_Returns201() throws Exception {
        String parentId = "11111111-1111-1111-1111-111111111111";
        AdminSubTenantCreateRequest request =
                new AdminSubTenantCreateRequest(
                        "facultad-ingenieria", "Facultad de Ingenieria", "FACULTY");
        AdminSubTenantResponse response =
                new AdminSubTenantResponse(
                        "22222222-2222-2222-2222-222222222222",
                        "facultad-ingenieria",
                        "Facultad de Ingenieria",
                        "FACULTY",
                        "ACTIVE",
                        parentId,
                        "UNIVERSITY",
                        Instant.now());

        when(adminSubTenantService.createSubTenant(
                        eq(parentId), any(AdminSubTenantCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/admin/tenants/{parentId}/children", parentId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FACULTY"))
                .andExpect(jsonPath("$.parentTenantType").value("UNIVERSITY"));
    }

    @Test
    void listChildren_WithPlatformAdminRole_Returns200() throws Exception {
        String parentId = "11111111-1111-1111-1111-111111111111";
        when(adminSubTenantService.listChildren(parentId, null, null))
                .thenReturn(
                        List.of(
                                new AdminSubTenantResponse(
                                        "22222222-2222-2222-2222-222222222222",
                                        "facultad-ingenieria",
                                        "Facultad de Ingenieria",
                                        "FACULTY",
                                        "ACTIVE",
                                        parentId,
                                        "UNIVERSITY",
                                        Instant.now())));

        mockMvc.perform(
                        get("/api/v1/admin/tenants/{parentId}/children", parentId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("facultad-ingenieria"));
    }

    private String tokenForRoles(List<String> roles) {
        return jwtService.generateToken(
                "admin@solveria.local", 1L, Map.of("roles", roles, "scopes", List.of()));
    }
}
