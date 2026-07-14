package com.supportflow.organization.config;

import com.supportflow.common.security.CurrentUser;
import com.supportflow.organization.entity.Membership;
import com.supportflow.organization.repository.MembershipRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Resolves org context per request via the X-Org-Id header, verifies the
 * current user actually belongs to that org (membership check), then sets
 * the Postgres session variable RLS policies rely on (Doc 04 §4, Doc 06 §3).
 *
 * Interim decision (see chat log / Doc 03 addendum): this logic will move
 * to the Gateway once it's fully built. For now, each org-scoped service
 * does this itself.
 */
@Component
public class OrgContextInterceptor implements HandlerInterceptor {

    private final MembershipRepository membershipRepository;
    private final EntityManager entityManager;

    public OrgContextInterceptor(MembershipRepository membershipRepository, EntityManager entityManager) {
        this.membershipRepository = membershipRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
            Object handler) {
        String orgIdHeader = request.getHeader("X-Org-Id");
        if (orgIdHeader == null) {
            return true; // endpoints that don't need org context (e.g. POST /orgs) just skip this
        }

        UUID orgId = UUID.fromString(orgIdHeader);
        UUID userId = CurrentUser.getUserId();

        Membership membership = membershipRepository.findByOrgIdAndUserId(orgId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "User does not belong to this organization"));

        entityManager.createNativeQuery("SET LOCAL app.current_org_id = :orgId")
                .setParameter("orgId", orgId.toString())
                .executeUpdate();

        request.setAttribute("membership", membership);
        return true;
    }
}