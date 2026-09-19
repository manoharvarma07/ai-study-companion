package com.aistudy.backend.auth;

import com.aistudy.backend.common.config.AppProperties;
import com.aistudy.backend.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(AppProperties props) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.expirationMs = props.jwt().expirationMs();
    }

    public String generate(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public UUID userId(String token) {
        return UUID.fromString(Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject());
    }
}
