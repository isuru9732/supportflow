package com.supportflow.common.response;

/**
 * Pagination block for list endpoints (Doc 05 §1). Only present on
 * responses where `data` is an array.
 */
public class PaginationMeta {
    public String cursor;
    public String nextCursor;
    public int limit;
    public boolean hasMore;

    public PaginationMeta(String cursor, String nextCursor, int limit, boolean hasMore) {
        this.cursor = cursor;
        this.nextCursor = nextCursor;
        this.limit = limit;
        this.hasMore = hasMore;
    }
}
