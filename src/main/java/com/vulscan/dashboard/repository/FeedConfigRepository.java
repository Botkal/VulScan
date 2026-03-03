package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.entity.FeedConfig;
import com.vulscan.dashboard.entity.FeedType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedConfigRepository extends JpaRepository<FeedConfig, Long> {
    Optional<FeedConfig> findByFeedType(FeedType feedType);
}
