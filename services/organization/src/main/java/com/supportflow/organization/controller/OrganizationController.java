package com.supportflow.organization.controller;

import com.supportflow.common.dto.RequestEnvelope;
import com.supportflow.common.response.ApiResponse;
import com.supportflow.organization.dto.CreateOrganizationRequest;
import com.supportflow.organization.dto.OrganizationResponse;
import com.supportflow.organization.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orgs")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @Valid @RequestBody RequestEnvelope<CreateOrganizationRequest> envelope) {
        OrganizationResponse response = organizationService.create(envelope.getData());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getCurrent(
            @RequestHeader("X-Org-Id") UUID orgId) {
        OrganizationResponse response = organizationService.getCurrent(orgId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }
}