package com.vulscan.dashboard.controller;

import com.vulscan.dashboard.dto.AssetViewDto;
import com.vulscan.dashboard.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
public class AssetController {

    private final AssetService service;

    public AssetController(AssetService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public ResponseEntity<List<AssetViewDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String hostname,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String cveId,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(service.search(q, hostname, product, vendor, version, cveId, minScore, maxScore, limit));
    }

    @GetMapping("/hostnames")
    public ResponseEntity<List<String>> hostnames(
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(service.suggestHostnames(prefix, limit));
    }

    @GetMapping("/products")
    public ResponseEntity<List<String>> products(
            @RequestParam(defaultValue = "") String prefix,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(service.suggestProducts(prefix, limit));
    }
}
