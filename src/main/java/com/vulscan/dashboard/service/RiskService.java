package com.vulscan.dashboard.service;

import com.vulscan.dashboard.dto.TopRiskDto;
import com.vulscan.dashboard.repository.RiskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskService {

    private final RiskRepository repo;

    public RiskService(RiskRepository repo) {
        this.repo = repo;
    }

    public List<TopRiskDto> top(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200)); // 1..200
        return repo.findTopRisks(safeLimit);
    }
}
