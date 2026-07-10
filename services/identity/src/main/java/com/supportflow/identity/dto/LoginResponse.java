package com.supportflow.identity.dto;

public class LoginResponse {
    public String accessToken;
    public String refreshToken;
    public String tokenType = "Bearer";
    public long expiresInSeconds;

    public LoginResponse(String accessToken, String refreshToken, long expiresInSeconds) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresInSeconds = expiresInSeconds;
    }
}