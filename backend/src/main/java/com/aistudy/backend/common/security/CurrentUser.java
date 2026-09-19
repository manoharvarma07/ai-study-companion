package com.aistudy.backend.common.security;

import com.aistudy.backend.user.User;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentUser {
    private CurrentUser() {}

    public static UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p)) {
            // Mapped to 401 by GlobalExceptionHandler (never leak a 500 here).
            throw new BadCredentialsException("Authentication required");
        }
        return p.userId();
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public record UserPrincipal(UUID userId, String email, User.Role role) {}
}
