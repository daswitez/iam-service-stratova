package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AdminUserUpdateRequest(
        @NotBlank(message = "Username is required")
                @Size(max = 100, message = "Username must not exceed 100 characters")
                String username,
        @NotBlank(message = "Email is required")
                @Email(message = "Email should be valid")
                @Size(max = 255, message = "Email must not exceed 255 characters")
                String email,
        @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
                String password,
        @NotBlank(message = "User category is required")
                @Pattern(
                        regexp = "^(STUDENT|PROFESSOR|ACADEMIC_ADMIN|FOUNDER|EXECUTIVE)$",
                        message =
                                "Invalid user category. Allowed values: STUDENT, PROFESSOR, ACADEMIC_ADMIN, FOUNDER, EXECUTIVE")
                String userCategory,
        Boolean active,
        String primaryTenantId,
        Set<String> roleNames) {

    public AdminUserUpdateRequest {
        roleNames = roleNames == null ? Set.of() : Set.copyOf(roleNames);
    }
}
