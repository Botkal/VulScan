package com.vulscan.dashboard.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kev_refresh_log")
public class KevRefreshLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refreshed_at", nullable = false)
    private OffsetDateTime refreshedAt;

    @Column(name = "vulns_read", nullable = false)
    private int vulnsRead;

    @Column(name = "upserted", nullable = false)
    private int upserted;

    @Column(name = "kev_count_before", nullable = false)
    private int kevCountBefore;

    @Column(name = "kev_count_after", nullable = false)
    private int kevCountAfter;

    @PrePersist
    public void prePersist() {
        if (refreshedAt == null) refreshedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public OffsetDateTime getRefreshedAt() { return refreshedAt; }
    public int getVulnsRead() { return vulnsRead; }
    public int getUpserted() { return upserted; }
    public int getKevCountBefore() { return kevCountBefore; }
    public int getKevCountAfter() { return kevCountAfter; }

    public void setVulnsRead(int vulnsRead) { this.vulnsRead = vulnsRead; }
    public void setUpserted(int upserted) { this.upserted = upserted; }
    public void setKevCountBefore(int kevCountBefore) { this.kevCountBefore = kevCountBefore; }
    public void setKevCountAfter(int kevCountAfter) { this.kevCountAfter = kevCountAfter; }
}
