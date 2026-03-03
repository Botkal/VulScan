package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.entity.CveAdvisory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CveAdvisoryRepository extends JpaRepository<CveAdvisory, Long> {
    void deleteByCveId(String cveId);
}
