package com.supportflow.identity.service.impl;

import com.supportflow.identity.dto.*;
import com.supportflow.identity.entity.AppUser;
import com.supportflow.identity.entity.RefreshToken;
import com.supportflow.identity.exception.EmailAlreadyRegisteredException;
import com.supportflow.identity.exception.InvalidCredentialsException;
import com.supportflow.identity.repository.AppUserRepository;
import com.supportflow.identity.repository.RefreshTokenRepository;
import com.supportflow.identity.security.JwtService;
import com.supportflow.identity.service.AuthService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthServiceImpl implements AuthService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 30;
    private static final long ACCESS_TOKEN_TTL_SECONDS = 15 * 60;

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(AppUserRepository userRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }
        String hash = passwordEncoder.encode(request.getPassword());
        AppUser user = new AppUser(request.getEmail(), hash);
        userRepository.save(user);
        // TODO (Epic 1, next task): trigger verification email via Notification Service
        return new RegisterResponse(user.getId(), user.getEmail(), "pending_verification");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        String rawRefreshToken = generateRawToken();
        String refreshTokenHash = sha256(rawRefreshToken);
        RefreshToken refreshToken = new RefreshToken(
                user.getId(),
                refreshTokenHash,
                Instant.now().plusSeconds(REFRESH_TOKEN_TTL_DAYS * 24 * 60 * 60)
        );
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(accessToken, rawRefreshToken, ACCESS_TOKEN_TTL_SECONDS);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}