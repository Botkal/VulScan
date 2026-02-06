package com.vulscan.dashboard.controller;

import com.vulscan.dashboard.dto.TopRiskDto;
import com.vulscan.dashboard.service.RiskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskService service;

    public RiskController(RiskService service) {
        this.service = service;
    }

    @GetMapping("/top")
    public ResponseEntity<List<TopRiskDto>> top(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(service.top(limit));
    }
}
