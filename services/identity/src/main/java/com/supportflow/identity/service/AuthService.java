package com.supportflow.identity.service;

import com.supportflow.identity.dto.LoginRequest;
import com.supportflow.identity.dto.LoginResponse;
import com.supportflow.identity.dto.RefreshRequest;
import com.supportflow.identity.dto.RegisterRequest;
import com.supportflow.identity.dto.RegisterResponse;
import com.supportflow.identity.dto.VerifyEmailRequest;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse refresh(RefreshRequest request);
    void logout(RefreshRequest request);
    void verifyEmail(VerifyEmailRequest request);
}