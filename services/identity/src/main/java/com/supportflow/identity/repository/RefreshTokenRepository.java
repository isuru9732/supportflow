package com.supportflow.identity.repository;

import com.supportflow.identity.entity.RefreshToken;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
}