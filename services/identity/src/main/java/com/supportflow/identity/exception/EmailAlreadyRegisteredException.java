package com.supportflow.identity.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("Email is already registered: " + email);
    }
}