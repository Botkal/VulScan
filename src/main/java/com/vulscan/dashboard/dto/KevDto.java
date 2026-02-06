package com.vulscan.dashboard.dto;

public record KevDto(
        String cveID,
        String vendorProject,
        String product,
        String vulnerabilityName,
        String dateAdded,
        String shortDescription,
        String requiredAction,
        String dueDate,
        String knownRansomwareCampaignUse
) {}
