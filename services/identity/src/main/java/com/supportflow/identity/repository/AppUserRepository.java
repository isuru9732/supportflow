package com.supportflow.identity.repository;

import com.supportflow.identity.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByGoogleId(String googleId);

    boolean existsByEmail(String email);
}