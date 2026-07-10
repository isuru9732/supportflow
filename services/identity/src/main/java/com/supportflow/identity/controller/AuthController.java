package com.supportflow.identity.controller;

import com.supportflow.identity.common.ApiResponse;
import com.supportflow.identity.dto.LoginEnvelope;
import com.supportflow.identity.dto.LoginResponse;
import com.supportflow.identity.dto.RefreshEnvelope;
import com.supportflow.identity.dto.RegisterEnvelope;
import com.supportflow.identity.dto.RegisterResponse;
import com.supportflow.identity.dto.VerifyEmailEnvelope;
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
            @Valid @RequestBody RegisterEnvelope envelope) {
        System.out.println(envelope);
        RegisterResponse response = authService.register(envelope.getData());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginEnvelope envelope) {
        LoginResponse response = authService.login(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshEnvelope envelope) {
        LoginResponse response = authService.refresh(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshEnvelope envelope) {
        authService.logout(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailEnvelope envelope) {
        authService.verifyEmail(envelope.getData());
        return ResponseEntity.ok(ApiResponse.of(null));
    }
}