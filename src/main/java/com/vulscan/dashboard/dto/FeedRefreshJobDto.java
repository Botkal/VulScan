package com.vulscan.dashboard.dto;

import java.time.OffsetDateTime;

public record FeedRefreshJobDto(
        String jobId,
        String type,
        String status,
        String message,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Integer itemsRead,
        Integer upserted
) {
}
