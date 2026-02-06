package com.vulscan.dashboard.dto;

import java.time.LocalDate;

public record TopRiskDto(
        String hostname,
        String inventoryProduct,
        String inventoryVendor,
        String cveId,
        String kevVendorProject,
        String kevProduct,
        LocalDate dueDate,
        Boolean knownRansomwareCampaignUse,
        int score
) {}
