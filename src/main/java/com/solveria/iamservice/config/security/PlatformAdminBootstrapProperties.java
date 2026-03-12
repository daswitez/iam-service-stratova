package com.solveria.iamservice.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bootstrap.admin")
public record PlatformAdminBootstrapProperties(
        boolean enabled, String username, String email, String password) {}
