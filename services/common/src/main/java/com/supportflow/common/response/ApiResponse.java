package com.supportflow.common.response;

import java.util.Map;

/**
 * Generic response wrapper matching the {"data": ..., "meta": ..., "pagination": ...}
 * convention (Doc 05 §1). Used by every service for every 2xx response.
 */
public class ApiResponse<T> {
    private T data;
    private Map<String, Object> meta;
    private PaginationMeta pagination;

    public static <T> ApiResponse<T> of(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> of(T data, PaginationMeta pagination) {
        ApiResponse<T> r = new ApiResponse<>();
        r.data = data;
        r.pagination = pagination;
        return r;
    }

    public static <T> ApiResponse<T> of(T data, Map<String, Object> meta) {
        ApiResponse<T> r = new ApiResponse<>();
        r.data = data;
        r.meta = meta;
        return r;
    }

    public T getData() { return data; }
    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
    public PaginationMeta getPagination() { return pagination; }
}
