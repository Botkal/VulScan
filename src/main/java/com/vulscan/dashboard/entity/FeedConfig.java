package com.vulscan.dashboard.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "feed_config")
public class FeedConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type", nullable = false, unique = true, length = 20)
    private FeedType feedType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "feed_url", nullable = false, columnDefinition = "TEXT")
    private String feedUrl;

    @Column(name = "last_refreshed_at")
    private OffsetDateTime lastRefreshedAt;

    @Column(name = "last_status", length = 20)
    private String lastStatus;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    public Long getId() {
        return id;
    }

    public FeedType getFeedType() {
        return feedType;
    }

    public void setFeedType(FeedType feedType) {
        this.feedType = feedType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    public OffsetDateTime getLastRefreshedAt() {
        return lastRefreshedAt;
    }

    public void setLastRefreshedAt(OffsetDateTime lastRefreshedAt) {
        this.lastRefreshedAt = lastRefreshedAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
