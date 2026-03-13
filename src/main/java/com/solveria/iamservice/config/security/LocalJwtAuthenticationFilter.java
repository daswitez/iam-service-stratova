package com.solveria.iamservice.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solveria.iamservice.api.exception.ErrorCodes;
import com.solveria.iamservice.api.exception.dto.ApiErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LocalJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ADMIN_API_PREFIX = "/api/v1/admin/";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final JwtAuthorityExtractor jwtAuthorityExtractor;
    private final ObjectMapper objectMapper;

    public LocalJwtAuthenticationFilter(
            JwtService jwtService,
            JwtAuthorityExtractor jwtAuthorityExtractor,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.jwtAuthorityExtractor = jwtAuthorityExtractor;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!StringUtils.hasText(path)) {
            path = request.getServletPath();
        }
        return !StringUtils.hasText(path) || !path.startsWith(ADMIN_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, request);
            return;
        }

        try {
            var claims = jwtService.parseClaims(token);
            var authorities = jwtAuthorityExtractor.extract(claims);
            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), token, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, request);
        }
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                new ApiErrorResponse(
                        ErrorCodes.UNAUTHORIZED, Instant.now(), request.getRequestURI()));
    }
}
