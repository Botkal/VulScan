package com.vulscan.dashboard.controller;

import com.vulscan.dashboard.dto.DashboardSummaryDto;
import com.vulscan.dashboard.dto.HostSummaryDto;
import com.vulscan.dashboard.dto.LatestKevDto;
import com.vulscan.dashboard.repository.DashboardRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
public class DashboardController {

    private final DashboardRepository repo;

    public DashboardController(DashboardRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> summary() {
        return ResponseEntity.ok(repo.getSummary());
    }

    @GetMapping("/hosts/recent")
    public ResponseEntity<List<HostSummaryDto>> recentHosts(@RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return ResponseEntity.ok(repo.getRecentHosts(safeLimit));
    }

    @GetMapping("/kev/latest")
    public ResponseEntity<LatestKevDto> latestKev() {
        return ResponseEntity.ok(repo.getLatestKev());
    }
}
