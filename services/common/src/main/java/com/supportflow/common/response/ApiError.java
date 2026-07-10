package com.supportflow.common.response;

/**
 * Standard error envelope (Doc 05 §1, Doc 08). Deliberately NOT nested under
 * `data` so clients can branch on structure without checking status code first.
 */
public class ApiError {
    private final ErrorBody error;

    public ApiError(String code, String message, String field) {
        this.error = new ErrorBody(code, message, field);
    }

    public ErrorBody getError() { return error; }

    public static class ErrorBody {
        public String code;
        public String message;
        public String field;

        public ErrorBody(String code, String message, String field) {
            this.code = code;
            this.message = message;
            this.field = field;
        }
    }
}
