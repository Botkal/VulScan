package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.entity.KevEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KevEntryRepository extends JpaRepository<KevEntry, String> {}
