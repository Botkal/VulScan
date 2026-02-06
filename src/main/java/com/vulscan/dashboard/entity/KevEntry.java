package com.vulscan.dashboard.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "kev_entry")

public class KevEntry {
    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getVendorProject() {
        return vendorProject;
    }

    public void setVendorProject(String vendorProject) {
        this.vendorProject = vendorProject;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVulnerabilityName() {
        return vulnerabilityName;
    }

    public void setVulnerabilityName(String vulnerabilityName) {
        this.vulnerabilityName = vulnerabilityName;
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getRequiredAction() {
        return requiredAction;
    }

    public void setRequiredAction(String requiredAction) {
        this.requiredAction = requiredAction;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean getKnownRansomwareCampaignUse() {
        return knownRansomwareCampaignUse;
    }

    public void setKnownRansomwareCampaignUse(Boolean knownRansomwareCampaignUse) {
        this.knownRansomwareCampaignUse = knownRansomwareCampaignUse;
    }

    @Id
    @Column(name = "cve_id", length = 20, nullable = false)
    private String cveId;

    @Column(name = "vendor_project")
    private String vendorProject;

    @Column(name = "product")
    private String product;

    @Column(name="vulnerability_name", columnDefinition = "TEXT")
    private String vulnerabilityName;

    @Column(name="date_added")
    private LocalDate dateAdded;

    @Column(name="short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name="required_action", columnDefinition = "TEXT")
    private String requiredAction;

    @Column(name="due_date")
    private LocalDate dueDate;

    @Column(name="known_ransomware_campaign_use")
    private Boolean knownRansomwareCampaignUse;
}
