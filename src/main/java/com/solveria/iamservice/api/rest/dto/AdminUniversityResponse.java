package com.solveria.iamservice.api.rest.dto;

import java.time.Instant;

public record AdminUniversityResponse(
        String id, String code, String name, String type, String status, Instant createdAt) {}
