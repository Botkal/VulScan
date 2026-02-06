package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.dto.TopRiskDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class RiskRepository {

    private final JdbcTemplate jdbc;

    public RiskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TopRiskDto> findTopRisks(int limit) {
        String sql = """
            SELECT
              i.hostname                                   AS hostname,
              i.product                                    AS inventory_product,
              i.vendor                                     AS inventory_vendor,
              k.cve_id                                     AS cve_id,
              k.vendor_project                             AS kev_vendor_project,
              k.product                                    AS kev_product,
              k.due_date                                   AS due_date,
              k.known_ransomware_campaign_use              AS known_ransomware_campaign_use,
              (
                2
                + CASE WHEN k.known_ransomware_campaign_use IS TRUE THEN 3 ELSE 0 END
                + CASE WHEN k.due_date IS NOT NULL AND k.due_date < CURRENT_DATE THEN 1 ELSE 0 END
              )::int                                       AS score
            FROM inventory_item i
            JOIN kev_entry k
              ON LOWER(i.product) LIKE ('%' || LOWER(k.product) || '%')
            ORDER BY score DESC, k.due_date NULLS LAST, i.hostname
            LIMIT ?
            """;

        return jdbc.query(sql, (rs, rowNum) -> new TopRiskDto(
                rs.getString("hostname"),
                rs.getString("inventory_product"),
                rs.getString("inventory_vendor"),
                rs.getString("cve_id"),
                rs.getString("kev_vendor_project"),
                rs.getString("kev_product"),
                toLocalDate(rs.getDate("due_date")),
                (Boolean) rs.getObject("known_ransomware_campaign_use"),
                rs.getInt("score")
        ), limit);
    }

    private static LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}
