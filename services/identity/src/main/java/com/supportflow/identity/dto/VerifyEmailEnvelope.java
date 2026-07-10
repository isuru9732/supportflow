package com.supportflow.identity.dto;

import jakarta.validation.Valid;

public class VerifyEmailEnvelope {
    @Valid
    private VerifyEmailRequest data;
    public VerifyEmailRequest getData() { return data; }
    public void setData(VerifyEmailRequest data) { this.data = data; }
}