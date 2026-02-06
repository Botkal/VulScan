package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.entity.KevRefreshLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KevRefreshLogRepository extends JpaRepository<KevRefreshLog, Long> {
    Optional<KevRefreshLog> findFirstByOrderByRefreshedAtDesc();
}
