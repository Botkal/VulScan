package com.vulscan.dashboard.dto;

import java.time.OffsetDateTime;

public record DashboardSummaryDto(
        int inventoryRows,
        int hostCount,
        int kevCount,
        OffsetDateTime lastInventorySeenAt
) {}
