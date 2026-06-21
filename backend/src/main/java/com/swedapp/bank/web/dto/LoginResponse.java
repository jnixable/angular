package com.swedapp.bank.web.dto;

public record LoginResponse(String token, long expiresInSeconds) {
}
