package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.entity.EpssEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpssEntryRepository extends JpaRepository<EpssEntry, String> {
}
