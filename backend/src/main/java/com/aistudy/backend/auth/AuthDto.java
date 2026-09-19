package com.aistudy.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDto {
    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 120) String name,
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record AuthResponse(String token, UserDto user) {}

    public record UserDto(String id, String name, String email, String role) {}

    public record MeResponse(String id, String name, String email, String role) {}
}
