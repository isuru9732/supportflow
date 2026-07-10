package com.supportflow.identity.service.impl;

import com.supportflow.identity.dto.*;
import com.supportflow.identity.entity.AppUser;
import com.supportflow.identity.entity.EmailVerificationToken;
import com.supportflow.identity.entity.RefreshToken;
import com.supportflow.identity.exception.EmailAlreadyRegisteredException;
import com.supportflow.identity.exception.EmailNotVerifiedException;
import com.supportflow.identity.exception.InvalidCredentialsException;
import com.supportflow.identity.exception.InvalidRefreshTokenException;
import com.supportflow.identity.exception.InvalidVerificationTokenException;
import com.supportflow.identity.repository.AppUserRepository;
import com.supportflow.identity.repository.EmailVerificationTokenRepository;
import com.supportflow.identity.repository.RefreshTokenRepository;
import com.supportflow.identity.security.JwtService;
import com.supportflow.identity.service.AuthService;
import com.supportflow.identity.service.EmailSender;
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
    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSender emailSender;

    public AuthServiceImpl(AppUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationTokenRepository verificationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailSender emailSender) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailSender = emailSender;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }
        String hash = passwordEncoder.encode(request.getPassword());
        AppUser user = new AppUser(request.getEmail(), hash);
        userRepository.save(user);

        String rawToken = generateRawToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken(
                user.getId(),
                sha256(rawToken),
                Instant.now().plusSeconds(VERIFICATION_TOKEN_TTL_HOURS * 60 * 60));
        verificationTokenRepository.save(verificationToken);
        emailSender.sendVerificationEmail(user.getEmail(), rawToken);

        return new RegisterResponse(user.getId(), user.getEmail(), "pending_verification");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken(
                user.getId(), sha256(rawRefreshToken),
                Instant.now().plusSeconds(REFRESH_TOKEN_TTL_DAYS * 24 * 60 * 60));
        refreshTokenRepository.save(refreshToken);
        return new LoginResponse(accessToken, rawRefreshToken, ACCESS_TOKEN_TTL_SECONDS);
    }

    @Override
    public LoginResponse refresh(RefreshRequest request) {
        String incomingHash = sha256(request.getRefreshToken());
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!existingToken.isValid()) {
            throw new InvalidRefreshTokenException();
        }
        existingToken.revoke();
        refreshTokenRepository.save(existingToken);

        AppUser user = userRepository.findById(existingToken.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String newRawRefreshToken = generateRawToken();
        RefreshToken newRefreshToken = new RefreshToken(
                user.getId(), sha256(newRawRefreshToken),
                Instant.now().plusSeconds(REFRESH_TOKEN_TTL_DAYS * 24 * 60 * 60));
        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponse(newAccessToken, newRawRefreshToken, ACCESS_TOKEN_TTL_SECONDS);
    }

    @Override
    public void logout(RefreshRequest request) {
        String incomingHash = sha256(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(incomingHash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public void verifyEmail(VerifyEmailRequest request) {
        String incomingHash = sha256(request.getToken());
        EmailVerificationToken token = verificationTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(InvalidVerificationTokenException::new);

        if (!token.isValid()) {
            throw new InvalidVerificationTokenException();
        }

        AppUser user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidVerificationTokenException::new);
        user.markEmailVerified();
        userRepository.save(user);

        token.markUsed();
        verificationTokenRepository.save(token);
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