package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CompetitionStaffEnrollmentCreateRequest(
        @NotNull(message = "User id is required") Long userId,
        @NotBlank(message = "Origin tenant id is required") String originTenantId,
        @NotBlank(message = "Participant type is required")
                @Pattern(
                        regexp = "^(JUDGE|INVESTOR|MENTOR|MANAGER)$",
                        message = "Participant type must be JUDGE, INVESTOR, MENTOR, or MANAGER")
                String participantType) {}
