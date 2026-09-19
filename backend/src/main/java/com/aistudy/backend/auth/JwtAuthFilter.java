package com.aistudy.backend.auth;

import com.aistudy.backend.user.User;
import com.aistudy.backend.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                UUID userId = jwtService.userId(header.substring(7));
                users.findById(userId).ifPresent(user -> {
                    var auth = new UsernamePasswordAuthenticationToken(
                            new com.aistudy.backend.common.security.CurrentUser.UserPrincipal(
                                    user.getId(), user.getEmail(), user.getRole()),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
