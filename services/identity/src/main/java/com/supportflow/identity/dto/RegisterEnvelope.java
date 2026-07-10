package com.supportflow.identity.dto;

import jakarta.validation.Valid;

// Matches the {"data": {...}} request envelope convention from Doc 05
public class RegisterEnvelope {
    @Valid
    private RegisterRequest data;

    public RegisterRequest getData() { return data; }
    public void setData(RegisterRequest data) { this.data = data; }
}