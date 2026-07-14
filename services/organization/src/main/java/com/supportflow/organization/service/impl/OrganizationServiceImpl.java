package com.supportflow.organization.service.impl;

import com.supportflow.common.security.CurrentUser;
import com.supportflow.organization.dto.CreateOrganizationRequest;
import com.supportflow.organization.dto.OrganizationResponse;
import com.supportflow.organization.entity.Membership;
import com.supportflow.organization.entity.Organization;
import com.supportflow.organization.repository.MembershipRepository;
import com.supportflow.organization.repository.OrganizationRepository;
import com.supportflow.organization.service.OrganizationService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public OrganizationResponse create(CreateOrganizationRequest request) {
        UUID userId = CurrentUser.getUserId();

        String slug = generateUniqueSlug(request.getName());
        Organization org = new Organization(request.getName(), slug);
        organizationRepository.save(org);

        Membership membership = new Membership(org.getId(), userId, "owner");
        membershipRepository.save(membership);

        return new OrganizationResponse(org.getId(), org.getName(), org.getSlug(),
                org.getBrandingColor(), org.getTimezone(), membership.getRole());
    }

    @Override
    public OrganizationResponse getCurrent(UUID orgId) {
        UUID userId = CurrentUser.getUserId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        Membership membership = membershipRepository.findByOrgIdAndUserId(orgId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member of this organization"));

        return new OrganizationResponse(org.getId(), org.getName(), org.getSlug(),
                org.getBrandingColor(), org.getTimezone(), membership.getRole());
    }

    private String generateUniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String slug = base;
        int suffix = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }
}