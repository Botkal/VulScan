package com.vulscan.dashboard.dto;

import java.time.OffsetDateTime;

public record HostSummaryDto(
        String hostname,
        int itemCount,
        OffsetDateTime lastSeenAt
) {}
