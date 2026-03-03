package com.vulscan.dashboard.dto;

public record UpdateFeedConfigRequestDto(
        Boolean enabled,
        String feedUrl
) {
}
