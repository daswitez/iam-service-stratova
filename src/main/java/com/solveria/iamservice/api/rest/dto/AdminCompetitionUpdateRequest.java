package com.solveria.iamservice.api.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record AdminCompetitionUpdateRequest(
        @NotBlank(message = "Code is required")
                @Size(max = 80, message = "Code must not exceed 80 characters")
                @Pattern(
                        regexp = "^[A-Za-z0-9-]+$",
                        message = "Code may only contain letters, numbers, and hyphens")
                String code,
        @NotBlank(message = "Name is required")
                @Size(max = 255, message = "Name must not exceed 255 characters")
                String name,
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
                String description,
        @NotBlank(message = "Scope is required")
                @Pattern(
                        regexp = "^(INTRA_TENANT|CROSS_TENANT)$",
                        message = "Scope must be INTRA_TENANT or CROSS_TENANT")
                String scope,
        @NotBlank(message = "Host tenant id is required") String hostTenantId,
        @NotBlank(message = "Product name is required")
                @Size(max = 255, message = "Product name must not exceed 255 characters")
                String productName,
        @NotBlank(message = "Industry code is required")
                @Size(max = 80, message = "Industry code must not exceed 80 characters")
                String industryCode,
        @NotBlank(message = "Industry name is required")
                @Size(max = 255, message = "Industry name must not exceed 255 characters")
                String industryName,
        @NotNull(message = "Initial capital amount is required")
                @DecimalMin(
                        value = "0.01",
                        message = "Initial capital amount must be greater than zero")
                BigDecimal initialCapitalAmount,
        @NotNull(message = "Min team size is required") Integer minTeamSize,
        @NotNull(message = "Max team size is required") Integer maxTeamSize,
        @NotBlank(message = "Role assignment method is required")
                @Pattern(
                        regexp = "^(ADMIN_ASSIGNMENT|DEMOCRATIC_ELECTION|SELF_DECLARED)$",
                        message =
                                "Role assignment method must be ADMIN_ASSIGNMENT, DEMOCRATIC_ELECTION, or SELF_DECLARED")
                String roleAssignmentMethod,
        @NotNull(message = "Allow optional COO is required") Boolean allowOptionalCoo,
        @Pattern(
                        regexp =
                                "^(DRAFT|PUBLISHED|ENROLLMENT_OPEN|ENROLLMENT_CLOSED|ACTIVE|FINISHED|ARCHIVED)$",
                        message =
                                "Status must be DRAFT, PUBLISHED, ENROLLMENT_OPEN, ENROLLMENT_CLOSED, ACTIVE, FINISHED, or ARCHIVED")
                String status,
        Instant startsAt,
        Instant endsAt) {}
