package com.vulscan.dashboard.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_item")

public class InventoryItem {
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCpe() {
        return cpe;
    }

    public void setCpe(String cpe) {
        this.cpe = cpe;
    }

    public LocalDate getInstalledOn() {
        return installedOn;
    }

    public void setInstalledOn(LocalDate installedOn) {
        this.installedOn = installedOn;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="source", nullable=false, length=50)
    private String source = "powershell_csv";

    @Column(name="hostname", nullable=false)
    private String hostname;

    @Column(name="asset_tag")
    private String assetTag;

    @Column(name="vendor")
    private String vendor;

    @Column(name="product", nullable=false)
    private String product;

    @Column(name="version")
    private String version;

    @Column(name="cpe")
    private String cpe;

    @Column(name="installed_on")
    private LocalDate installedOn;

    @Column(name="last_seen_at", nullable=false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    void prePersist() {
        if (lastSeenAt == null) lastSeenAt = LocalDateTime.now();
    }
}
