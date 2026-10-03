package com.eadalat.auth.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {}
