package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompetitionEnrollmentStatusUpdateRequest(
        @NotBlank(message = "Status is required")
                @Pattern(
                        regexp = "^(INVITED|APPROVED|REJECTED|WAITLISTED|WITHDRAWN)$",
                        message =
                                "Status must be INVITED, APPROVED, REJECTED, WAITLISTED, or WITHDRAWN")
                String status) {}
