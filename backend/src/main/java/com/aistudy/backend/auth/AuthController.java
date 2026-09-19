package com.aistudy.backend.auth;

import com.aistudy.backend.common.security.CurrentUser;
import com.aistudy.backend.user.User;
import com.aistudy.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDto.MeResponse> me() {
        User u = users.findById(CurrentUser.id()).orElseThrow();
        return ResponseEntity.ok(new AuthDto.MeResponse(
                u.getId().toString(), u.getName(), u.getEmail(), u.getRole().name()));
    }
}
