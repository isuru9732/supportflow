package com.supportflow.common.dto;

import jakarta.validation.Valid;

/**
 * Generic request wrapper matching the {"data": {...}} convention (Doc 05 §1).
 * Use RequestEnvelope&lt;RegisterRequest&gt;, RequestEnvelope&lt;LoginRequest&gt;, etc.
 * instead of writing a separate XxxEnvelope class per endpoint.
 */
public class RequestEnvelope<T> {

    @Valid
    private T data;

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
