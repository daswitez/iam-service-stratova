package com.solveria.iamservice.config.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthorityExtractor {

    public Collection<GrantedAuthority> extract(Map<String, Object> claims) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        addClaimAuthorities(claims.get("roles"), "ROLE_", authorities);
        addClaimAuthorities(claims.get("scopes"), "SCOPE_", authorities);
        return authorities;
    }

    private void addClaimAuthorities(
            Object claimValue, String prefix, Set<GrantedAuthority> authorities) {
        if (claimValue instanceof Collection<?> collection) {
            collection.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(value -> new SimpleGrantedAuthority(prefix + value))
                    .forEach(authorities::add);
            return;
        }

        if (claimValue instanceof String stringValue) {
            Arrays.stream(stringValue.split("[,\\s]+"))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(value -> new SimpleGrantedAuthority(prefix + value))
                    .forEach(authorities::add);
        }
    }
}
