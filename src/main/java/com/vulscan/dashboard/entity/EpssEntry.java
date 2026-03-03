package com.vulscan.dashboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "epss_entry")
public class EpssEntry {

    @Id
    @Column(name = "cve_id", length = 20, nullable = false)
    private String cveId;

    @Column(name = "epss_score")
    private Double epssScore;

    @Column(name = "percentile")
    private Double percentile;

    @Column(name = "feed_date")
    private LocalDate feedDate;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public Double getEpssScore() {
        return epssScore;
    }

    public void setEpssScore(Double epssScore) {
        this.epssScore = epssScore;
    }

    public Double getPercentile() {
        return percentile;
    }

    public void setPercentile(Double percentile) {
        this.percentile = percentile;
    }

    public LocalDate getFeedDate() {
        return feedDate;
    }

    public void setFeedDate(LocalDate feedDate) {
        this.feedDate = feedDate;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
