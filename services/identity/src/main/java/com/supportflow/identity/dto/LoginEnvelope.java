package com.supportflow.identity.dto;

import jakarta.validation.Valid;

public class LoginEnvelope {
    @Valid
    private LoginRequest data;
    public LoginRequest getData() { return data; }
    public void setData(LoginRequest data) { this.data = data; }
}