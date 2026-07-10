package com.supportflow.identity.exception;

public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("Reset link is invalid or has expired");
    }
}