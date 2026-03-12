package com.solveria.iamservice.config.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey key;

    // Default expiration: 1 day (or could be externalized)
    private static final long JWT_EXPIRATION_MS = 86400000;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        // Either use the provided secret or fallback to a long secure local dev key
        String secret =
                jwtProperties.secretKey() != null
                        ? jwtProperties.secretKey()
                        : "c2VjdXJlX2tleV9kZXYtbG9jYWxfaWFtX3NlcnZpY2VfMzJieXRlX2xvbmdfeHh4eA==";

        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /** Generate a new JWT token for the authenticated user */
    public String generateToken(
            String email, Long userId, String tenantId, Iterable<String> roles) {
        return generateToken(
                email,
                userId,
                Map.of(
                        "tenantId", tenantId,
                        "roles", roles));
    }

    public String generateToken(String email, Long userId, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + JWT_EXPIRATION_MS))
                .signWith(key)
                .compact();
    }
}
