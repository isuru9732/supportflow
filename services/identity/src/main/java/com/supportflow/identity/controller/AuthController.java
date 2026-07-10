package com.supportflow.identity.controller;

import com.supportflow.common.dto.RequestEnvelope;
import com.supportflow.common.response.ApiResponse;
import com.supportflow.identity.dto.*;
import com.supportflow.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RequestEnvelope<RegisterRequest> envelope) {
        RegisterResponse response = authService.register(envelope.getData());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody RequestEnvelope<LoginRequest> envelope) {
        LoginResponse response = authService.login(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RequestEnvelope<RefreshRequest> envelope) {
        LoginResponse response = authService.refresh(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RequestEnvelope<RefreshRequest> envelope) {
        authService.logout(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody RequestEnvelope<VerifyEmailRequest> envelope) {
        authService.verifyEmail(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody RequestEnvelope<ForgotPasswordRequest> envelope) {
        authService.forgotPassword(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody RequestEnvelope<ResetPasswordRequest> envelope) {
        authService.resetPassword(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody RequestEnvelope<GoogleAuthRequest> envelope) {
        LoginResponse response = authService.googleLogin(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}