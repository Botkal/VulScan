package com.vulscan.dashboard.service;

import com.vulscan.dashboard.dto.FeedConfigDto;
import com.vulscan.dashboard.dto.FeedRefreshResultDto;
import com.vulscan.dashboard.dto.KevRefreshResultDto;
import com.vulscan.dashboard.dto.UpdateFeedConfigRequestDto;
import com.vulscan.dashboard.entity.FeedConfig;
import com.vulscan.dashboard.entity.FeedType;
import com.vulscan.dashboard.repository.FeedConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

@Service
public class FeedManagementService {

    private final FeedConfigRepository feedConfigRepository;
    private final JdbcTemplate jdbcTemplate;
    private final KevRefreshService kevRefreshService;
    private final EpssRefreshService epssRefreshService;
    private final NvdAdvisoryRefreshService nvdAdvisoryRefreshService;
    private final String defaultKevUrl;
    private final String defaultEpssUrl;
    private final String defaultNvdUrl;
    private volatile boolean feedTypeConstraintEnsured = false;

    public FeedManagementService(FeedConfigRepository feedConfigRepository,
                                 JdbcTemplate jdbcTemplate,
                                 KevRefreshService kevRefreshService,
                                 EpssRefreshService epssRefreshService,
                                 NvdAdvisoryRefreshService nvdAdvisoryRefreshService,
                                 @Value("${app.kev-feed-url:https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json}") String defaultKevUrl,
                                 @Value("${app.epss-feed-url:https://api.first.org/data/v1/epss}") String defaultEpssUrl,
                                 @Value("${app.nvd-feed-url:https://services.nvd.nist.gov/rest/json/cves/2.0}") String defaultNvdUrl) {
        this.feedConfigRepository = feedConfigRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.kevRefreshService = kevRefreshService;
        this.epssRefreshService = epssRefreshService;
        this.nvdAdvisoryRefreshService = nvdAdvisoryRefreshService;
        this.defaultKevUrl = defaultKevUrl;
        this.defaultEpssUrl = defaultEpssUrl;
        this.defaultNvdUrl = defaultNvdUrl;
    }

    @Transactional
    public List<FeedConfigDto> list() {
        ensureDefaults();
        ensureAllEnabled();
        return feedConfigRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public FeedConfigDto update(FeedType feedType, UpdateFeedConfigRequestDto request) {
        ensureDefaults();
        ensureAllEnabled();
        FeedConfig cfg = feedConfigRepository.findByFeedType(feedType)
                .orElseThrow(() -> new IllegalArgumentException("Feed not found: " + feedType));

        if (request.feedUrl() != null && !request.feedUrl().isBlank()) {
            cfg.setFeedUrl(request.feedUrl().trim());
        }

        return toDto(feedConfigRepository.save(cfg));
    }

    @Transactional
    public FeedRefreshResultDto refresh(FeedType feedType) {
        ensureDefaults();
        FeedConfig cfg = feedConfigRepository.findByFeedType(feedType)
                .orElseThrow(() -> new IllegalArgumentException("Feed not found: " + feedType));

        if (!cfg.isEnabled()) {
            cfg.setLastStatus("SKIPPED");
            cfg.setLastMessage("Feed is disabled");
            cfg.setLastRefreshedAt(OffsetDateTime.now());
            feedConfigRepository.save(cfg);
            return new FeedRefreshResultDto(feedType.name(), false, 0, 0, "Feed is disabled");
        }

        cfg.setLastStatus("RUNNING");
        cfg.setLastMessage("Refresh in progress...");
        cfg.setLastRefreshedAt(OffsetDateTime.now());
        feedConfigRepository.save(cfg);

        try {
            FeedRefreshResultDto result;
            if (feedType == FeedType.KEV) {
                KevRefreshResultDto kevResult = kevRefreshService.refreshKev(cfg.getFeedUrl());
                result = new FeedRefreshResultDto(feedType.name(), true, kevResult.vulnsRead(), kevResult.upserted(), "OK");
            } else if (feedType == FeedType.EPSS) {
                EpssRefreshService.RefreshStats stats = epssRefreshService.refreshFromKevCves(cfg.getFeedUrl());
                result = new FeedRefreshResultDto(feedType.name(), true, stats.itemsRead(), stats.upserted(), "OK");
            } else if (feedType == FeedType.NVD) {
                NvdAdvisoryRefreshService.RefreshStats stats = nvdAdvisoryRefreshService.refreshFromKevCves(cfg.getFeedUrl());
                String message = (stats.skippedCves() > 0 ? "PARTIAL (skipped=" + stats.skippedCves() + ")" : "OK")
                    + " processed=" + stats.processedCves() + "/" + stats.totalCves();
                result = new FeedRefreshResultDto(feedType.name(), true, stats.itemsRead(), stats.upserted(), message);
            } else {
                throw new IllegalArgumentException("Unsupported feed: " + feedType);
            }

            cfg.setLastStatus(result.message().startsWith("PARTIAL") ? "WARN" : "OK");
            cfg.setLastMessage(result.message() + " (read=" + result.itemsRead() + ", upserted=" + result.upserted() + ")");
            cfg.setLastRefreshedAt(OffsetDateTime.now());
            feedConfigRepository.save(cfg);
            return result;
        } catch (CancellationException ex) {
            cfg.setLastStatus("CANCELLED");
            cfg.setLastMessage("Refresh cancelled");
            cfg.setLastRefreshedAt(OffsetDateTime.now());
            feedConfigRepository.save(cfg);
            throw ex;
        } catch (Exception ex) {
            cfg.setLastStatus("ERROR");
            cfg.setLastMessage(truncate(ex.getMessage(), 500));
            cfg.setLastRefreshedAt(OffsetDateTime.now());
            feedConfigRepository.save(cfg);
            throw ex;
        }
    }

    @Transactional
    public List<FeedRefreshResultDto> refreshEnabled() {
        ensureDefaults();
        ensureAllEnabled();
        List<FeedType> orderedTypes = new ArrayList<>(List.of(FeedType.KEV, FeedType.EPSS, FeedType.NVD));
        return orderedTypes.stream()
            .map(feedConfigRepository::findByFeedType)
            .flatMap(java.util.Optional::stream)
            .map(FeedConfig::getFeedType)
            .map(this::refresh)
            .toList();
    }

    private void ensureDefaults() {
        ensureFeedTypeConstraint();
        for (FeedType type : Arrays.asList(FeedType.KEV, FeedType.EPSS, FeedType.NVD)) {
            feedConfigRepository.findByFeedType(type).orElseGet(() -> {
                FeedConfig cfg = new FeedConfig();
                cfg.setFeedType(type);
                cfg.setEnabled(true);
            cfg.setFeedUrl(
                type == FeedType.KEV ? defaultKevUrl
                    : type == FeedType.EPSS ? defaultEpssUrl
                    : defaultNvdUrl
            );
                cfg.setLastStatus("NEVER");
                cfg.setLastMessage("Not refreshed yet");
                return feedConfigRepository.save(cfg);
            });
        }
        ensureAllEnabled();
    }

    private void ensureAllEnabled() {
        for (FeedType type : List.of(FeedType.KEV, FeedType.EPSS, FeedType.NVD)) {
            feedConfigRepository.findByFeedType(type).ifPresent(cfg -> {
                if (!cfg.isEnabled()) {
                    cfg.setEnabled(true);
                    feedConfigRepository.save(cfg);
                }
            });
        }
    }

    private synchronized void ensureFeedTypeConstraint() {
        if (feedTypeConstraintEnsured) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE feed_config DROP CONSTRAINT IF EXISTS feed_config_feed_type_check");
        jdbcTemplate.execute("ALTER TABLE feed_config ADD CONSTRAINT feed_config_feed_type_check CHECK (feed_type IN ('KEV','EPSS','NVD'))");
        feedTypeConstraintEnsured = true;
    }

    private FeedConfigDto toDto(FeedConfig cfg) {
        return new FeedConfigDto(
                cfg.getFeedType().name(),
                cfg.isEnabled(),
                cfg.getFeedUrl(),
                cfg.getLastRefreshedAt(),
                cfg.getLastStatus(),
                cfg.getLastMessage()
        );
    }

    private static String truncate(String input, int max) {
        if (input == null) {
            return null;
        }
        if (input.length() <= max) {
            return input;
        }
        return input.substring(0, max);
    }
}
