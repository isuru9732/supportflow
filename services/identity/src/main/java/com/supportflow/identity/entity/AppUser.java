package com.supportflow.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash; // nullable now — Google-only users have none

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
    }

    // Password-based signup
    public AppUser(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // Google-only signup — no password
    public static AppUser fromGoogle(String email, String googleId) {
        AppUser user = new AppUser();
        user.email = email;
        user.googleId = googleId;
        user.emailVerified = true; // Google already verified this email
        return user;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getGoogleId() {
        return googleId;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void linkGoogleAccount(String googleId) {
        this.googleId = googleId;
        this.emailVerified = true; // trust Google's verification even on a linked account
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }
}