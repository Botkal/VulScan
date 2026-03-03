package com.vulscan.dashboard.repository;

import com.vulscan.dashboard.dto.AssetViewDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AssetRepository {

    private final JdbcTemplate jdbc;

    public AssetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AssetViewDto> search(String q,
                                     String hostname,
                                     String product,
                                     String vendor,
                                     String version,
                                     String cveId,
                                     Integer minScore,
                                     Integer maxScore,
                                     int limit) {

        String baseSql = """
            SELECT *
            FROM (
              SELECT
                i.hostname                                  AS hostname,
                i.product                                   AS product,
                i.vendor                                    AS vendor,
                i.version                                   AS version,
                i.source                                    AS source,
                k.cve_id                                    AS cve_id,
                e.epss_score                                AS epss_score,
                k.due_date                                  AS due_date,
                CASE WHEN k.due_date IS NOT NULL THEN (k.due_date - CURRENT_DATE)::int ELSE NULL END AS days_to_due,
                COALESCE(adv.advisory_count, 0)::int        AS advisory_count,
                COALESCE(adv.patch_available, FALSE)        AS patch_available,
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
                ))::int                                     AS score
              FROM inventory_item i
              JOIN kev_entry k
                ON LOWER(i.product) LIKE ('%' || LOWER(k.product) || '%')
              LEFT JOIN epss_entry e
                ON e.cve_id = k.cve_id
              LEFT JOIN LATERAL (
                SELECT
                  COUNT(*) AS advisory_count,
                  BOOL_OR(ca.patch_available) AS patch_available
                FROM cve_advisory ca
                WHERE ca.cve_id = k.cve_id
              ) adv ON TRUE
            ) x
            WHERE 1 = 1
            """;

        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> args = new ArrayList<>();

        if (notBlank(q)) {
            String like = '%' + q.trim().toLowerCase() + '%';
            sql.append(" AND (LOWER(x.hostname) LIKE ? OR LOWER(x.product) LIKE ? OR LOWER(COALESCE(x.vendor, '')) LIKE ? OR LOWER(COALESCE(x.version, '')) LIKE ? OR LOWER(x.cve_id) LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }

        appendLike(sql, args, "x.hostname", hostname);
        appendLike(sql, args, "x.product", product);
        appendLike(sql, args, "COALESCE(x.vendor, '')", vendor);
        appendLike(sql, args, "COALESCE(x.version, '')", version);
        appendLike(sql, args, "x.cve_id", cveId);

        if (minScore != null) {
            sql.append(" AND x.score >= ?");
            args.add(minScore);
        }
        if (maxScore != null) {
            sql.append(" AND x.score <= ?");
            args.add(maxScore);
        }

        sql.append(" ORDER BY x.score DESC, x.hostname ASC, x.product ASC LIMIT ?");
        args.add(limit);

        return jdbc.query(sql.toString(), (rs, rowNum) -> new AssetViewDto(
                rs.getString("hostname"),
                rs.getString("product"),
                rs.getString("vendor"),
                rs.getString("version"),
                rs.getString("source"),
                rs.getString("cve_id"),
                (Integer) rs.getObject("score"),
                (Double) rs.getObject("epss_score"),
                toLocalDate(rs.getDate("due_date")),
                (Integer) rs.getObject("days_to_due"),
                (Integer) rs.getObject("advisory_count"),
                (Boolean) rs.getObject("patch_available")
        ), args.toArray());
    }

      public List<String> suggestHostnames(String prefix, int limit) {
        String sql = """
              SELECT i.hostname
          FROM inventory_item i
          WHERE i.hostname IS NOT NULL
            AND i.hostname <> ''
            AND LOWER(i.hostname) LIKE ?
              GROUP BY i.hostname
              ORDER BY COUNT(*) DESC, i.hostname ASC
          LIMIT ?
          """;

        String like = (prefix == null ? "" : prefix.trim().toLowerCase()) + "%";
        return jdbc.query(sql, (rs, rowNum) -> rs.getString(1), like, limit);
      }

      public List<String> suggestProducts(String prefix, int limit) {
          String sql = """
              SELECT i.product
              FROM inventory_item i
              WHERE i.product IS NOT NULL
                AND i.product <> ''
                AND LOWER(i.product) LIKE ?
              GROUP BY i.product
              ORDER BY COUNT(*) DESC, i.product ASC
              LIMIT ?
              """;

          String like = (prefix == null ? "" : prefix.trim().toLowerCase()) + "%";
          return jdbc.query(sql, (rs, rowNum) -> rs.getString(1), like, limit);
      }

    private static void appendLike(StringBuilder sql, List<Object> args, String field, String value) {
        if (!notBlank(value)) {
            return;
        }
        sql.append(" AND LOWER(").append(field).append(") LIKE ?");
        args.add('%' + value.trim().toLowerCase() + '%');
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }
}
