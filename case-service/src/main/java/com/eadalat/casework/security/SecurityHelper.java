package com.eadalat.casework.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reads the JWT-backed principal installed by {@link JwtAuthFilter}.
 * The principal is the user id (String) and the authority is ROLE_<role>.
 */
@Component("securityHelper")
public class SecurityHelper {

    public String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? String.valueOf(auth.getPrincipal()) : null;
    }

    public Long currentUserIdAsLong() {
        String id = currentUserId();
        try {
            return id != null ? Long.valueOf(id) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }

    public boolean hasAnyRole(String... roles) {
        String current = currentRole();
        if (current == null) {
            return false;
        }
        for (String role : roles) {
            if (current.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
