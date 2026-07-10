package com.supportflow.identity.service;

import com.supportflow.identity.dto.LoginRequest;
import com.supportflow.identity.dto.LoginResponse;
import com.supportflow.identity.dto.RegisterRequest;
import com.supportflow.identity.dto.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}