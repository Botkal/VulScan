package com.vulscan.dashboard.dto;

import java.time.OffsetDateTime;

public record FeedConfigDto(
        String type,
        boolean enabled,
        String feedUrl,
        OffsetDateTime lastRefreshedAt,
        String lastStatus,
        String lastMessage
) {
}
