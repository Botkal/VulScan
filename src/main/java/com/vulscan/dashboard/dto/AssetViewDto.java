package com.vulscan.dashboard.dto;

import java.time.LocalDate;

public record AssetViewDto(
        String hostname,
        String product,
        String vendor,
        String version,
        String source,
        String cveId,
        Integer score,
        Double epssScore,
        LocalDate dueDate,
        Integer daysToDue,
        Integer advisoryCount,
        Boolean patchAvailable
) {
}
