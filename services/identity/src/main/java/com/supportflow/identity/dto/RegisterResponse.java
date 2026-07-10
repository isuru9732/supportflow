package com.supportflow.identity.dto;

import java.util.UUID;

public class RegisterResponse {
    public UUID userId;
    public String email;
    public String status;

    public RegisterResponse(UUID userId, String email, String status) {
        this.userId = userId;
        this.email = email;
        this.status = status;
    }
}