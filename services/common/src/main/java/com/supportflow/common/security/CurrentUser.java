package com.supportflow.common.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Convenience accessor for "who is making this request" — reads the
 * principal set by JwtAuthenticationFilter. Use in services/controllers
 * instead of manually pulling from SecurityContextHolder everywhere.
 */
public class CurrentUser {

    public static UUID getUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString((String) principal);
    }
}