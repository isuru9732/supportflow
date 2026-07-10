package com.supportflow.identity.common;

import java.util.Map;

public class ApiResponse<T> {
    private T data;
    private Map<String, Object> meta;

    public static <T> ApiResponse<T> of(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.data = data;
        return r;
    }

    public T getData() { return data; }
    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
}