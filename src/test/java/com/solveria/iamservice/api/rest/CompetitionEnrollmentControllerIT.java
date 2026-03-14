package com.solveria.iamservice.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solveria.iamservice.api.rest.dto.CompetitionEnrollmentResponse;
import com.solveria.iamservice.api.rest.dto.CompetitionEnrollmentStatusUpdateRequest;
import com.solveria.iamservice.api.rest.dto.CompetitionStaffEnrollmentCreateRequest;
import com.solveria.iamservice.application.service.CompetitionEnrollmentService;
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
class CompetitionEnrollmentControllerIT {

    private static final String COMPETITION_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String ENROLLMENT_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;

    @MockitoBean private CompetitionEnrollmentService competitionEnrollmentService;

    @Test
    void createStaffEnrollment_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(
                        post(
                                        "/api/v1/competitions/{competitionId}/enrollments/staff",
                                        COMPETITION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(competitionEnrollmentService);
    }

    @Test
    void createStaffEnrollment_WithPlatformAdminRole_Returns201() throws Exception {
        when(competitionEnrollmentService.createStaffEnrollment(
                        any(String.class), any(CompetitionStaffEnrollmentCreateRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(
                        post(
                                        "/api/v1/competitions/{competitionId}/enrollments/staff",
                                        COMPETITION_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ENROLLMENT_ID))
                .andExpect(jsonPath("$.participantType").value("JUDGE"))
                .andExpect(jsonPath("$.originTenantCode").value("umsa"));
    }

    @Test
    void createStaffEnrollment_WithNonPlatformAdminRole_Returns403() throws Exception {
        mockMvc.perform(
                        post(
                                        "/api/v1/competitions/{competitionId}/enrollments/staff",
                                        COMPETITION_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("ACADEMIC_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void createStaffEnrollment_WithMalformedJson_Returns400ValidationError() throws Exception {
        String malformedRequest =
                """
                {
                  "userId": 2,
                  "originTenantId": "92f501ab-0d72-4511-af43-a83199773f40",
                  "participantType": "JUDGE",
                }
                """;

        mockMvc.perform(
                        post(
                                        "/api/v1/competitions/{competitionId}/enrollments/staff",
                                        COMPETITION_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(malformedRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.body").value("Malformed JSON request"));

        verifyNoInteractions(competitionEnrollmentService);
    }

    @Test
    void getEnrollment_WithPlatformAdminRole_Returns200() throws Exception {
        when(competitionEnrollmentService.getEnrollment(COMPETITION_ID, ENROLLMENT_ID))
                .thenReturn(sampleResponse());

        mockMvc.perform(
                        get(
                                        "/api/v1/competitions/{competitionId}/enrollments/{enrollmentId}",
                                        COMPETITION_ID,
                                        ENROLLMENT_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competitionCode").value("biz-sim-2026"))
                .andExpect(jsonPath("$.status").value("INVITED"));
    }

    @Test
    void updateEnrollmentStatus_WithPlatformAdminRole_Returns200() throws Exception {
        CompetitionEnrollmentResponse approvedResponse =
                new CompetitionEnrollmentResponse(
                        ENROLLMENT_ID,
                        COMPETITION_ID,
                        "biz-sim-2026",
                        10L,
                        "11111111-1111-1111-1111-111111111111",
                        "umsa",
                        "Universidad Mayor de San Andres",
                        "JUDGE",
                        "APPROVED",
                        null,
                        Instant.parse("2026-03-14T00:00:00Z"),
                        Instant.parse("2026-03-13T23:00:00Z"));
        when(competitionEnrollmentService.updateEnrollmentStatus(
                        any(String.class),
                        any(String.class),
                        any(CompetitionEnrollmentStatusUpdateRequest.class)))
                .thenReturn(approvedResponse);

        mockMvc.perform(
                        patch(
                                        "/api/v1/competitions/{competitionId}/enrollments/{enrollmentId}/status",
                                        COMPETITION_ID,
                                        ENROLLMENT_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new CompetitionEnrollmentStatusUpdateRequest(
                                                        "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedAt").exists());
    }

    @Test
    void withdrawEnrollment_WithPlatformAdminRole_Returns204() throws Exception {
        mockMvc.perform(
                        delete(
                                        "/api/v1/competitions/{competitionId}/enrollments/{enrollmentId}",
                                        COMPETITION_ID,
                                        ENROLLMENT_ID)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenForRoles(List.of("PLATFORM_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    private CompetitionStaffEnrollmentCreateRequest validCreateRequest() {
        return new CompetitionStaffEnrollmentCreateRequest(
                10L, "11111111-1111-1111-1111-111111111111", "JUDGE");
    }

    private CompetitionEnrollmentResponse sampleResponse() {
        return new CompetitionEnrollmentResponse(
                ENROLLMENT_ID,
                COMPETITION_ID,
                "biz-sim-2026",
                10L,
                "11111111-1111-1111-1111-111111111111",
                "umsa",
                "Universidad Mayor de San Andres",
                "JUDGE",
                "INVITED",
                null,
                null,
                Instant.parse("2026-03-13T23:00:00Z"));
    }

    private String tokenForRoles(List<String> roles) {
        return jwtService.generateToken(
                "admin@solveria.local", 1L, Map.of("roles", roles, "scopes", List.of()));
    }
}
