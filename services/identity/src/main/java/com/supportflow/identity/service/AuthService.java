package com.supportflow.identity.service;

import com.supportflow.identity.dto.*;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(RefreshRequest request);

    void logout(RefreshRequest request);

    void verifyEmail(VerifyEmailRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    LoginResponse googleLogin(GoogleAuthRequest request);
}