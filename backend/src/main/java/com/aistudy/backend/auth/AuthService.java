package com.aistudy.backend.auth;

import com.aistudy.backend.common.exception.ConflictException;
import com.aistudy.backend.user.User;
import com.aistudy.backend.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(User.Role.USER);
        users.save(user);
        return new AuthDto.AuthResponse(jwtService.generate(user), toDto(user));
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return new AuthDto.AuthResponse(jwtService.generate(user), toDto(user));
    }

    public static AuthDto.UserDto toDto(User u) {
        return new AuthDto.UserDto(u.getId().toString(), u.getName(), u.getEmail(), u.getRole().name());
    }
}
