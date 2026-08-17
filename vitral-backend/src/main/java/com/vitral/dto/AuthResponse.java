package com.vitral.dto;

public record AuthResponse(
        String token,
        long expiresIn,
        AccountResponse account) {
}
