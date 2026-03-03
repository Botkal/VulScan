package com.vulscan.dashboard.service;

import com.vulscan.dashboard.dto.AssetViewDto;
import com.vulscan.dashboard.repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetService {

    private final AssetRepository repository;

    public AssetService(AssetRepository repository) {
        this.repository = repository;
    }

    public List<AssetViewDto> search(String q,
                                     String hostname,
                                     String product,
                                     String vendor,
                                     String version,
                                     String cveId,
                                     Integer minScore,
                                     Integer maxScore,
                                     int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return repository.search(q, hostname, product, vendor, version, cveId, minScore, maxScore, safeLimit);
    }

    public List<String> suggestHostnames(String prefix, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return repository.suggestHostnames(prefix, safeLimit);
    }

    public List<String> suggestProducts(String prefix, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return repository.suggestProducts(prefix, safeLimit);
    }
}
