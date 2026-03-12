package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Email is required") @Email(message = "Email should be valid")
                String email,
        @NotBlank(message = "Password is required") String password,
        @NotBlank(message = "User category is required")
                @Pattern(
                        regexp = "^(STUDENT|PROFESSOR|ACADEMIC_ADMIN|FOUNDER|EXECUTIVE)$",
                        message =
                                "Invalid user category. Allowed values: STUDENT, PROFESSOR, ACADEMIC_ADMIN, FOUNDER, EXECUTIVE")
                String userCategory,
        String tenantId,
        String tenantName) {}
