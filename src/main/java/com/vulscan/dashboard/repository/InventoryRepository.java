
package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
}


