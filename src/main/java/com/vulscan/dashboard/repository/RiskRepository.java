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
              e.epss_score                                 AS epss_score,
              260                                          AS kev_base_score,
              CASE WHEN k.known_ransomware_campaign_use IS TRUE THEN 220 ELSE 0 END AS ransomware_bonus,
              CASE
                WHEN k.due_date IS NULL THEN 60
                WHEN k.due_date < CURRENT_DATE - INTERVAL '30 days' THEN 260
                WHEN k.due_date < CURRENT_DATE THEN 220
                WHEN k.due_date <= CURRENT_DATE + INTERVAL '7 days' THEN 170
                WHEN k.due_date <= CURRENT_DATE + INTERVAL '30 days' THEN 120
                ELSE 80
              END                                          AS due_urgency_score,
              CASE
                WHEN k.date_added IS NULL THEN 25
                WHEN k.date_added >= CURRENT_DATE - INTERVAL '14 days' THEN 120
                WHEN k.date_added >= CURRENT_DATE - INTERVAL '60 days' THEN 70
                WHEN k.date_added >= CURRENT_DATE - INTERVAL '180 days' THEN 35
                ELSE 15
              END                                          AS kev_recency_score,
              CASE
                WHEN e.epss_score IS NULL THEN 40
                ELSE LEAST(220, GREATEST(0, ROUND(e.epss_score * 220)::int))
              END                                          AS epss_exploit_score,
              CASE
                WHEN LOWER(COALESCE(i.hostname, '') || ' ' || COALESCE(i.vendor, '') || ' ' || COALESCE(i.product, ''))
                       SIMILAR TO '%(domain controller|dc-|prod|payment|finance|sql|oracle|exchange|vcenter|hyper-v|backup|firewall|vpn|gateway|auth|idp|sso)%' THEN 120
                WHEN LOWER(COALESCE(i.hostname, '') || ' ' || COALESCE(i.vendor, '') || ' ' || COALESCE(i.product, ''))
                       SIMILAR TO '%(server|db|api|web|core|app)%' THEN 70
                ELSE 30
              END
              + LEAST(20, GREATEST(0, 20 - ABS(LENGTH(COALESCE(i.hostname, '')) - 12)))
                                                           AS asset_criticality_score,
              (
                20
                + CASE
                    WHEN LOWER(COALESCE(i.product, '')) = LOWER(COALESCE(k.product, '')) THEN 20
                    ELSE 0
                  END
                + LEAST(20, GREATEST(0, 20 - ABS(LENGTH(COALESCE(i.product, '')) - LENGTH(COALESCE(k.product, '')))))
                + CASE
                    WHEN LOWER(COALESCE(i.vendor, '')) LIKE ('%' || LOWER(COALESCE(k.vendor_project, '')) || '%')
                      OR LOWER(COALESCE(k.vendor_project, '')) LIKE ('%' || LOWER(COALESCE(i.vendor, '')) || '%') THEN 20
                    ELSE 0
                  END
              )                                            AS match_confidence_score,
              COALESCE(adv.advisory_count, 0)::int         AS advisory_count,
              COALESCE(adv.patch_available, FALSE)         AS patch_available,
              adv.advisory_url                             AS advisory_url,
              CASE WHEN k.due_date IS NOT NULL THEN (k.due_date - CURRENT_DATE)::int ELSE NULL END AS days_to_due,
              LEAST(1000, GREATEST(0,
                260
                + CASE WHEN k.known_ransomware_campaign_use IS TRUE THEN 220 ELSE 0 END
                + CASE
                    WHEN k.due_date IS NULL THEN 60
                    WHEN k.due_date < CURRENT_DATE - INTERVAL '30 days' THEN 260
                    WHEN k.due_date < CURRENT_DATE THEN 220
                    WHEN k.due_date <= CURRENT_DATE + INTERVAL '7 days' THEN 170
                    WHEN k.due_date <= CURRENT_DATE + INTERVAL '30 days' THEN 120
                    ELSE 80
                  END
                + CASE
                    WHEN k.date_added IS NULL THEN 25
                    WHEN k.date_added >= CURRENT_DATE - INTERVAL '14 days' THEN 120
                    WHEN k.date_added >= CURRENT_DATE - INTERVAL '60 days' THEN 70
                    WHEN k.date_added >= CURRENT_DATE - INTERVAL '180 days' THEN 35
                    ELSE 15
                  END
                + CASE
                    WHEN e.epss_score IS NULL THEN 40
                    ELSE LEAST(220, GREATEST(0, ROUND(e.epss_score * 220)::int))
                  END
                + CASE
                    WHEN LOWER(COALESCE(i.hostname, '') || ' ' || COALESCE(i.vendor, '') || ' ' || COALESCE(i.product, ''))
                           SIMILAR TO '%(domain controller|dc-|prod|payment|finance|sql|oracle|exchange|vcenter|hyper-v|backup|firewall|vpn|gateway|auth|idp|sso)%' THEN 120
                    WHEN LOWER(COALESCE(i.hostname, '') || ' ' || COALESCE(i.vendor, '') || ' ' || COALESCE(i.product, ''))
                           SIMILAR TO '%(server|db|api|web|core|app)%' THEN 70
                    ELSE 30
                  END
                + LEAST(20, GREATEST(0, 20 - ABS(LENGTH(COALESCE(i.hostname, '')) - 12)))
                + (
                    20
                    + CASE
                        WHEN LOWER(COALESCE(i.product, '')) = LOWER(COALESCE(k.product, '')) THEN 20
                        ELSE 0
                      END
                    + LEAST(20, GREATEST(0, 20 - ABS(LENGTH(COALESCE(i.product, '')) - LENGTH(COALESCE(k.product, '')))))
                    + CASE
                        WHEN LOWER(COALESCE(i.vendor, '')) LIKE ('%' || LOWER(COALESCE(k.vendor_project, '')) || '%')
                          OR LOWER(COALESCE(k.vendor_project, '')) LIKE ('%' || LOWER(COALESCE(i.vendor, '')) || '%') THEN 20
                        ELSE 0
                      END
                  )
              ))::int                                      AS score
            FROM inventory_item i
            JOIN kev_entry k
              ON LOWER(i.product) LIKE ('%' || LOWER(k.product) || '%')
            LEFT JOIN epss_entry e
              ON e.cve_id = k.cve_id
            LEFT JOIN LATERAL (
              SELECT
                COUNT(*) AS advisory_count,
                BOOL_OR(ca.patch_available) AS patch_available,
                MIN(ca.url) AS advisory_url
              FROM cve_advisory ca
              WHERE ca.cve_id = k.cve_id
            ) adv ON TRUE
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
                rs.getInt("score"),
                rs.getInt("kev_base_score"),
                rs.getInt("ransomware_bonus"),
                rs.getInt("due_urgency_score"),
                rs.getInt("kev_recency_score"),
                rs.getInt("epss_exploit_score"),
                (Double) rs.getObject("epss_score"),
                rs.getInt("asset_criticality_score"),
                rs.getInt("match_confidence_score"),
                rs.getInt("advisory_count"),
                (Boolean) rs.getObject("patch_available"),
                rs.getString("advisory_url"),
                (Integer) rs.getObject("days_to_due")
        ), limit);
    }

    private static LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}
