package com.vulscan.dashboard.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cve_advisory", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cve_advisory_cve_url", columnNames = {"cve_id", "url"})
})
public class CveAdvisory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cve_id", nullable = false, length = 20)
    private String cveId;

    @Column(name = "source", length = 200)
    private String source;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "patch_available", nullable = false)
    private boolean patchAvailable;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isPatchAvailable() {
        return patchAvailable;
    }

    public void setPatchAvailable(boolean patchAvailable) {
        this.patchAvailable = patchAvailable;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
