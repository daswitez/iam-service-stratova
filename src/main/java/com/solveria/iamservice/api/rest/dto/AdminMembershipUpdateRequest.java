package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.Pattern;

public record AdminMembershipUpdateRequest(
        @Pattern(
                        regexp = "^(ACTIVE|INVITED|SUSPENDED|LEFT)$",
                        message = "Status must be ACTIVE, INVITED, SUSPENDED, or LEFT")
                String status,
        Boolean isPrimary) {}
