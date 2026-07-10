package com.supportflow.identity.dto;

import jakarta.validation.Valid;

public class RefreshEnvelope {
    @Valid
    private RefreshRequest data;
    public RefreshRequest getData() { return data; }
    public void setData(RefreshRequest data) { this.data = data; }
}