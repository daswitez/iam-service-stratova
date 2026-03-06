package com.solveria.iamservice.api.rest.dto;

import java.util.Set;

public record AuthResponse(String token, UserDto user) {
    public record UserDto(
            Long id,
            String username,
            String email,
            String primaryTenantId,
            String userCategory,
            Set<String> roles) {}
}
