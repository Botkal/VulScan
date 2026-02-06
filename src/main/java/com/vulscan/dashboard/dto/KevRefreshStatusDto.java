package com.vulscan.dashboard.dto;

import java.time.OffsetDateTime;

public record KevRefreshStatusDto(
        OffsetDateTime refreshedAt,
        int vulnsRead,
        int upserted,
        int kevCountBefore,
        int kevCountAfter,
        int added
) {}

