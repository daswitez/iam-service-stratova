package com.solveria.iamservice.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solveria.iamservice.api.rest.dto.AdminUniversityCreateRequest;
import com.solveria.iamservice.api.rest.dto.AdminUniversityResponse;
import com.solveria.iamservice.application.service.AdminUniversityService;
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
class AdminUniversityControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private AdminUniversityService adminUniversityService;

    @Test
    void createUniversity_WithoutToken_Returns401() throws Exception {
        AdminUniversityCreateRequest request =
                new AdminUniversityCreateRequest("umsa", "Universidad Mayor de San Andres");

        mockMvc.perform(
                        post("/api/v1/admin/universities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(adminUniversityService);
    }

    @Test
    void createUniversity_WithPlatformAdminRole_Returns201() throws Exception {
        AdminUniversityCreateRequest request =
                new AdminUniversityCreateRequest("umsa", "Universidad Mayor de San Andres");
        AdminUniversityResponse response =
                new AdminUniversityResponse(
                        "6dfece42-f772-4f2a-a084-af4f11223344",
                        "umsa",
                        "Universidad Mayor de San Andres",
                        "UNIVERSITY",
                        "ACTIVE",
                        Instant.now());

        when(adminUniversityService.createUniversity(any(AdminUniversityCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/v1/admin/universities")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("umsa"))
                .andExpect(jsonPath("$.type").value("UNIVERSITY"));
    }

    @Test
    void createUniversity_WithNonPlatformAdminRole_Returns403() throws Exception {
        AdminUniversityCreateRequest request =
                new AdminUniversityCreateRequest("umsa", "Universidad Mayor de San Andres");

        mockMvc.perform(
                        post("/api/v1/admin/universities")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("ACADEMIC_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void listUniversities_WithPlatformAdminRole_Returns200() throws Exception {
        when(adminUniversityService.listUniversities(null))
                .thenReturn(
                        List.of(
                                new AdminUniversityResponse(
                                        "6dfece42-f772-4f2a-a084-af4f11223344",
                                        "umsa",
                                        "Universidad Mayor de San Andres",
                                        "UNIVERSITY",
                                        "ACTIVE",
                                        Instant.now())));

        mockMvc.perform(
                        get("/api/v1/admin/universities")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Universidad Mayor de San Andres"));
    }

    private String tokenForRoles(List<String> roles) {
        return jwtService.generateToken(
                "admin@solveria.local", 1L, Map.of("roles", roles, "scopes", List.of()));
    }
}
