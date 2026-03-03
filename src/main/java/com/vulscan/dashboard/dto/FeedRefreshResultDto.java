package com.vulscan.dashboard.dto;

public record FeedRefreshResultDto(
        String type,
        boolean executed,
        int itemsRead,
        int upserted,
        String message
) {
}
