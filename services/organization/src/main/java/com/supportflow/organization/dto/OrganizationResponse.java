package com.supportflow.organization.dto;

import java.util.UUID;

public class OrganizationResponse {
    public UUID id;
    public String name;
    public String slug;
    public String brandingColor;
    public String timezone;
    public String role; // the requesting user's role in this org

    public OrganizationResponse(UUID id, String name, String slug, String brandingColor, String timezone, String role) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.brandingColor = brandingColor;
        this.timezone = timezone;
        this.role = role;
    }
}