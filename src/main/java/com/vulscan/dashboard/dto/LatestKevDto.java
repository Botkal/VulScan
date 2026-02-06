package com.vulscan.dashboard.dto;

import java.time.LocalDate;

public record LatestKevDto(
        String cveId,
        String vendorProject,
        String product,
        String vulnerabilityName,
        LocalDate dateAdded,
        LocalDate dueDate,
        Boolean knownRansomwareCampaignUse
) {}
