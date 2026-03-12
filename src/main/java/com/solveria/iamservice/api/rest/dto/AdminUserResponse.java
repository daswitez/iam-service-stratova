package com.solveria.iamservice.api.rest.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String userCategory,
        boolean active,
        String primaryTenantId,
        Set<String> roleNames,
        LocalDateTime createdAt,
        LocalDateTime lastModifiedAt) {}
