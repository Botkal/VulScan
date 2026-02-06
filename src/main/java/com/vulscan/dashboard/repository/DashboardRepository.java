package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.dto.DashboardSummaryDto;
import com.vulscan.dashboard.dto.HostSummaryDto;
import com.vulscan.dashboard.dto.LatestKevDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public DashboardSummaryDto getSummary() {
        Integer inventoryRows = jdbc.queryForObject("SELECT COUNT(*) FROM inventory_item", Integer.class);
        Integer hostCount = jdbc.queryForObject("SELECT COUNT(DISTINCT hostname) FROM inventory_item", Integer.class);
        Integer kevCount = jdbc.queryForObject("SELECT COUNT(*) FROM kev_entry", Integer.class);

        Timestamp lastSeenTs = jdbc.queryForObject("SELECT MAX(last_seen_at) FROM inventory_item", Timestamp.class);
        OffsetDateTime lastSeen = lastSeenTs == null ? null
                : lastSeenTs.toInstant().atOffset(OffsetDateTime.now().getOffset());

        return new DashboardSummaryDto(
                inventoryRows == null ? 0 : inventoryRows,
                hostCount == null ? 0 : hostCount,
                kevCount == null ? 0 : kevCount,
                lastSeen
        );
    }

    public List<HostSummaryDto> getRecentHosts(int limit) {
        String sql = """
            SELECT hostname,
                   COUNT(*) AS item_count,
                   MAX(last_seen_at) AS last_seen_at
            FROM inventory_item
            GROUP BY hostname
            ORDER BY MAX(last_seen_at) DESC
            LIMIT ?
            """;

        return jdbc.query(sql, (rs, rowNum) -> new HostSummaryDto(
                rs.getString("hostname"),
                rs.getInt("item_count"),
                rs.getObject("last_seen_at", OffsetDateTime.class)
        ), limit);
    }

    public LatestKevDto getLatestKev() {
        String sql = """
            SELECT cve_id, vendor_project, product, vulnerability_name,
                   date_added, due_date, known_ransomware_campaign_use
            FROM kev_entry
            ORDER BY date_added DESC NULLS LAST, cve_id
            LIMIT 1
            """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) return null;
            return new LatestKevDto(
                    rs.getString("cve_id"),
                    rs.getString("vendor_project"),
                    rs.getString("product"),
                    rs.getString("vulnerability_name"),
                    rs.getObject("date_added", java.time.LocalDate.class),
                    rs.getObject("due_date", java.time.LocalDate.class),
                    (Boolean) rs.getObject("known_ransomware_campaign_use")
            );
        });
    }
}
