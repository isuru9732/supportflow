package com.supportflow.organization.service;

import com.supportflow.organization.dto.CreateOrganizationRequest;
import com.supportflow.organization.dto.OrganizationResponse;

public interface OrganizationService {
    OrganizationResponse create(CreateOrganizationRequest request);

    OrganizationResponse getCurrent(java.util.UUID orgId);
}