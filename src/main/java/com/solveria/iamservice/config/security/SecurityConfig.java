package com.solveria.iamservice.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solveria.iamservice.api.exception.ErrorCodes;
import com.solveria.iamservice.api.exception.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration for IAM Service.
 *
 * <p>Provides two security filter chains: 1. JWT-enabled mode (security.jwt.enabled=true): Requires
 * JWT authentication for /api/** 2. JWT-disabled mode (security.jwt.enabled=false, default): No
 * authentication required (DEV mode)
 *
 * <p>Both modes allow public access to: - /actuator/health/**, /actuator/info/** - /v3/api-docs/**,
 * /swagger-ui/**, /swagger-ui.html
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtProperties jwtProperties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        if (jwtProperties.enabled()) {
            log.info("event=SECURITY_CONFIG_JWT_ENABLED enabled=true");
        } else {
            log.info("event=SECURITY_CONFIG_JWT_DISABLED enabled=false");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain for JWT-enabled mode. Requires JWT authentication for /api/**
     * endpoints.
     */
    @Bean
    @ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true")
    public SecurityFilterChain jwtSecurityFilterChain(
            HttpSecurity http, JwtAuthorityExtractor jwtAuthorityExtractor) throws Exception {
        log.info("event=SECURITY_CONFIG_JWT_ENABLED");

        http
                // Disable CSRF for stateless REST API
                // REST APIs are stateless and use JWT tokens, so CSRF protection is not needed
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management (JWT tokens, no server-side sessions)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // Public endpoints: Actuator health/info
                                        .requestMatchers(
                                                new AntPathRequestMatcher("/actuator/health/**"),
                                                new AntPathRequestMatcher("/actuator/info/**"))
                                        .permitAll()

                                        // Protected endpoint: Prometheus requires JWT +
                                        // SCOPE_operations.read
                                        .requestMatchers(
                                                new AntPathRequestMatcher("/actuator/prometheus"))
                                        .hasAuthority("SCOPE_operations.read")

                                        // Public endpoints: OpenAPI/Swagger documentation
                                        .requestMatchers(
                                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                                new AntPathRequestMatcher("/swagger-ui/**"),
                                                new AntPathRequestMatcher("/swagger-ui.html"))
                                        .permitAll()

                                        // Public endpoint: Error handling
                                        .requestMatchers(new AntPathRequestMatcher("/error"))
                                        .permitAll()

                                        // Public endpoints: Auth registration and login
                                        .requestMatchers(
                                                new AntPathRequestMatcher("/api/v1/auth/**"))
                                        .permitAll()
                                        .requestMatchers(
                                                new AntPathRequestMatcher("/api/v1/admin/**"))
                                        .authenticated()

                                        // Protected endpoints: All /api/** require authentication
                                        .requestMatchers(new AntPathRequestMatcher("/api/**"))
                                        .authenticated()

                                        // Deny all other requests
                                        .anyRequest()
                                        .denyAll())

                // Configure OAuth2 Resource Server with JWT
                // JWT decoder will be auto-configured from
                // spring.security.oauth2.resourceserver.jwt.issuer-uri
                // or spring.security.oauth2.resourceserver.jwt.jwk-set-uri properties
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(
                                        jwt ->
                                                jwt.jwtAuthenticationConverter(
                                                        jwtAuthenticationConverter(
                                                                jwtAuthorityExtractor))))

                // Configure exception handling to return consistent ApiErrorResponse
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(authenticationEntryPoint())
                                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    /**
     * Security filter chain for JWT-disabled mode (DEV mode). Allows all requests without
     * authentication.
     */
    @Bean
    @ConditionalOnProperty(
            name = "security.jwt.enabled",
            havingValue = "false",
            matchIfMissing = true)
    public SecurityFilterChain devSecurityFilterChain(
            HttpSecurity http, LocalJwtAuthenticationFilter localJwtAuthenticationFilter)
            throws Exception {
        log.info("event=SECURITY_CONFIG_JWT_DISABLED mode=DEV");

        http
                // Disable CSRF for stateless REST API
                // In DEV mode, we still disable CSRF for consistency with JWT mode
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        localJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                new AntPathRequestMatcher("/actuator/health/**"),
                                                new AntPathRequestMatcher("/actuator/info/**"),
                                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                                new AntPathRequestMatcher("/swagger-ui/**"),
                                                new AntPathRequestMatcher("/swagger-ui.html"),
                                                new AntPathRequestMatcher("/error"),
                                                new AntPathRequestMatcher("/api/v1/auth/**"))
                                        .permitAll()
                                        .requestMatchers(
                                                new AntPathRequestMatcher("/api/v1/admin/**"))
                                        .authenticated()
                                        // Keep non-admin APIs open in DEV until the rest of the
                                        // modules are migrated to JWT.
                                        .anyRequest()
                                        .permitAll())
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(authenticationEntryPoint())
                                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(
            JwtAuthorityExtractor jwtAuthorityExtractor) {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        return jwt -> {
            var authorities = new LinkedHashSet<>(scopesConverter.convert(jwt));
            authorities.addAll(jwtAuthorityExtractor.extract(jwt.getClaims()));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    /**
     * AuthenticationEntryPoint for 401 Unauthorized responses. Returns ApiErrorResponse with
     * errorCode="UNAUTHORIZED".
     */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn(
                    "event=SECURITY_AUTHENTICATION_FAILED path={}",
                    request.getRequestURI(),
                    authException);
            writeErrorResponse(
                    response,
                    request,
                    ErrorCodes.UNAUTHORIZED,
                    HttpServletResponse.SC_UNAUTHORIZED);
        };
    }

    /**
     * AccessDeniedHandler for 403 Forbidden responses. Returns ApiErrorResponse with
     * errorCode="FORBIDDEN".
     */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn(
                    "event=SECURITY_ACCESS_DENIED path={}",
                    request.getRequestURI(),
                    accessDeniedException);
            writeErrorResponse(
                    response, request, ErrorCodes.FORBIDDEN, HttpServletResponse.SC_FORBIDDEN);
        };
    }

    /** Writes ApiErrorResponse as JSON to the HTTP response. */
    private void writeErrorResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            String errorCode,
            int httpStatus)
            throws IOException {
        ApiErrorResponse errorResponse =
                new ApiErrorResponse(errorCode, Instant.now(), request.getRequestURI());

        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
