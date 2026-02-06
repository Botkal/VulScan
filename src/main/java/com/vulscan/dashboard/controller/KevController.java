package com.vulscan.dashboard.controller;

import com.vulscan.dashboard.dto.KevRefreshResultDto;
import com.vulscan.dashboard.dto.KevRefreshStatusDto;
import com.vulscan.dashboard.service.KevRefreshService;
import com.vulscan.dashboard.repository.KevRefreshLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kev")
public class KevController {

    private final KevRefreshService kevRefreshService;
    private final KevRefreshLogRepository logRepo;

    public KevController(KevRefreshService kevRefreshService, KevRefreshLogRepository logRepo) {
        this.kevRefreshService = kevRefreshService;
        this.logRepo = logRepo;
    }

    @PostMapping("/refresh")
    public ResponseEntity<KevRefreshResultDto> refresh() {
        return ResponseEntity.ok(kevRefreshService.refreshKev());
    }

    @GetMapping("/status")
    public ResponseEntity<KevRefreshStatusDto> status() {
        return ResponseEntity.ok(
                logRepo.findFirstByOrderByRefreshedAtDesc()
                        .map(l -> new KevRefreshStatusDto(
                                l.getRefreshedAt(),
                                l.getVulnsRead(),
                                l.getUpserted(),
                                l.getKevCountBefore(),
                                l.getKevCountAfter(),
                                Math.max(0, l.getKevCountAfter() - l.getKevCountBefore())
                        ))
                        .orElse(null)
        );
    }

}


