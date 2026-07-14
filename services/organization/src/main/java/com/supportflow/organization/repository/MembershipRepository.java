package com.supportflow.organization.repository;

import com.supportflow.organization.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByOrgIdAndUserId(UUID orgId, UUID userId);
    List<Membership> findByUserId(UUID userId);
}