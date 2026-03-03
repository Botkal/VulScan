package com.vulscan.dashboard.controller;

import com.vulscan.dashboard.dto.FeedConfigDto;
import com.vulscan.dashboard.dto.FeedRefreshJobDto;
import com.vulscan.dashboard.dto.FeedRefreshResultDto;
import com.vulscan.dashboard.dto.UpdateFeedConfigRequestDto;
import com.vulscan.dashboard.entity.FeedType;
import com.vulscan.dashboard.service.FeedManagementService;
import com.vulscan.dashboard.service.FeedRefreshJobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feeds")
public class FeedController {

    private final FeedManagementService service;
    private final FeedRefreshJobService jobService;

    public FeedController(FeedManagementService service,
                          FeedRefreshJobService jobService) {
        this.service = service;
        this.jobService = jobService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
    public ResponseEntity<List<FeedConfigDto>> list() {
        return ResponseEntity.ok(service.list());
    }

    @PutMapping("/{feedType}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or hasAnyAuthority('ADMIN','SUPERADMIN','ROLE_ADMIN','ROLE_SUPERADMIN')")
    public ResponseEntity<FeedConfigDto> update(@PathVariable String feedType,
                                                @RequestBody UpdateFeedConfigRequestDto request) {
        return ResponseEntity.ok(service.update(parse(feedType), request));
    }

    @PostMapping("/{feedType}/refresh")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or hasAnyAuthority('ADMIN','SUPERADMIN','ROLE_ADMIN','ROLE_SUPERADMIN')")
    public ResponseEntity<FeedRefreshResultDto> refresh(@PathVariable String feedType) {
        return ResponseEntity.ok(service.refresh(parse(feedType)));
    }

    @PostMapping("/refresh-enabled")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or hasAnyAuthority('ADMIN','SUPERADMIN','ROLE_ADMIN','ROLE_SUPERADMIN')")
    public ResponseEntity<List<FeedRefreshResultDto>> refreshEnabled() {
        return ResponseEntity.ok(service.refreshEnabled());
    }

    @PostMapping("/{feedType}/refresh-async")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or hasAnyAuthority('ADMIN','SUPERADMIN','ROLE_ADMIN','ROLE_SUPERADMIN')")
    public ResponseEntity<FeedRefreshJobDto> refreshAsync(@PathVariable String feedType) {
        return ResponseEntity.ok(jobService.start(parse(feedType)));
    }

    @PostMapping("/refresh-enabled-async")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or hasAnyAuthority('ADMIN','SUPERADMIN','ROLE_ADMIN','ROLE_SUPERADMIN')")
    public ResponseEntity<List<FeedRefreshJobDto>> refreshEnabledAsync() {
        return ResponseEntity.ok(jobService.startEnabled());
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or hasAnyAuthority('ADMIN','SUPERADMIN','ROLE_ADMIN','ROLE_SUPERADMIN')")
    public ResponseEntity<List<FeedRefreshJobDto>> jobs() {
        return ResponseEntity.ok(jobService.listJobs());
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN') or hasAnyAuthority('ADMIN','SUPERADMIN','ROLE_ADMIN','ROLE_SUPERADMIN')")
    public ResponseEntity<FeedRefreshJobDto> cancel(@PathVariable String jobId) {
        return ResponseEntity.ok(jobService.cancel(jobId));
    }

    private FeedType parse(String raw) {
        return FeedType.valueOf(raw.trim().toUpperCase());
    }
}
