package com.vulscan.dashboard.service;

import com.vulscan.dashboard.entity.CveAdvisory;
import com.vulscan.dashboard.repository.CveAdvisoryRepository;
import com.vulscan.dashboard.repository.KevEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;

@Service
public class NvdAdvisoryRefreshService {

    private final WebClient webClient;
    private final KevEntryRepository kevRepo;
    private final CveAdvisoryRepository advisoryRepo;
    private final JdbcTemplate jdbcTemplate;
    private final String nvdApiKey;
    private final long requestDelayMs;
    private final int maxCvesPerRun;

    public NvdAdvisoryRefreshService(WebClient.Builder webClientBuilder,
                                     KevEntryRepository kevRepo,
                                     CveAdvisoryRepository advisoryRepo,
                                     JdbcTemplate jdbcTemplate,
                                     @Value("${app.nvd-api-key:}") String nvdApiKey,
                                     @Value("${app.nvd-request-delay-ms:700}") long requestDelayMs,
                                     @Value("${app.nvd-max-cves-per-run:120}") int maxCvesPerRun) {
        this.webClient = webClientBuilder.build();
        this.kevRepo = kevRepo;
        this.advisoryRepo = advisoryRepo;
        this.jdbcTemplate = jdbcTemplate;
        this.nvdApiKey = nvdApiKey == null ? "" : nvdApiKey.trim();
        this.requestDelayMs = Math.max(0L, requestDelayMs);
        this.maxCvesPerRun = Math.max(1, maxCvesPerRun);
    }

    @Transactional
    public RefreshStats refreshFromKevCves(String nvdBaseUrl) {
        ensureNotInterrupted();
        List<String> allCves = kevRepo.findAll().stream()
                .map(k -> k.getCveId())
                .filter(id -> id != null && !id.isBlank())
                .distinct()
            .sorted(Comparator.naturalOrder())
                .toList();

        if (allCves.isEmpty()) {
            return new RefreshStats(0, 0, 0, 0, 0);
        }

        int totalCves = allCves.size();
        int processCount = Math.min(totalCves, maxCvesPerRun);
        List<String> cves = allCves.subList(0, processCount);

        int read = 0;
        int upserted = 0;
        int skippedCves = 0;

        for (String cveId : cves) {
            ensureNotInterrupted();
            String url = UriComponentsBuilder.fromUriString(nvdBaseUrl)
                    .queryParam("cveId", cveId)
                    .toUriString();

                NvdApiResponse root = fetchWithRetry(url);
                if (root == null) {
                    skippedCves++;
                    sleepQuietly(requestDelayMs);
                    continue;
                }

            advisoryRepo.deleteByCveId(cveId);

            List<CveAdvisory> parsed = parseAdvisories(cveId, root);
            read += parsed.size();
            if (!parsed.isEmpty()) {
                upserted += saveAdvisoriesSafely(parsed);
            }

            sleepQuietly(requestDelayMs);
        }

        return new RefreshStats(read, upserted, skippedCves, processCount, totalCves);
    }

    private NvdApiResponse fetchWithRetry(String url) {
        int maxAttempts = 5;
        long backoffMs = 1200L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            ensureNotInterrupted();
            try {
                WebClient.RequestHeadersSpec<?> req = webClient.get().uri(url);
                if (!nvdApiKey.isBlank()) {
                    req = req.header("apiKey", nvdApiKey);
                }

                return req
                        .retrieve()
                        .bodyToMono(NvdApiResponse.class)
                        .block();
            } catch (WebClientResponseException.TooManyRequests ex) {
                if (attempt == maxAttempts) {
                    return null;
                }
                sleepQuietly(backoffMs);
                backoffMs = Math.min(15000L, backoffMs * 2);
            } catch (WebClientResponseException ex) {
                if ((ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429) && attempt < maxAttempts) {
                    sleepQuietly(backoffMs);
                    backoffMs = Math.min(15000L, backoffMs * 2);
                    continue;
                }
                if (ex.getStatusCode().value() == 429 || ex.getStatusCode().is5xxServerError()) {
                    return null;
                }
                throw ex;
            }
        }

        return null;
    }

    private static void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Refresh was cancelled");
        }
    }

    private static void ensureNotInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Refresh was cancelled");
        }
    }

    private List<CveAdvisory> parseAdvisories(String cveId, NvdApiResponse root) {
        Map<String, CveAdvisory> advisoryByUrl = new LinkedHashMap<>();
        if (root == null || root.vulnerabilities() == null) {
            return List.of();
        }

        for (NvdVulnerability vulnerability : root.vulnerabilities()) {
            if (vulnerability == null || vulnerability.cve() == null || vulnerability.cve().references() == null) {
                continue;
            }

            for (NvdReference ref : vulnerability.cve().references()) {
                if (ref == null) {
                    continue;
                }

                String refUrl = textOrNull(ref.url());
                if (refUrl == null || refUrl.isBlank()) {
                    continue;
                }

                String source = textOrNull(ref.source());
                List<String> tagsList = new ArrayList<>();
                if (ref.tags() != null) {
                    for (String t : ref.tags()) {
                        if (t != null && !t.isBlank()) {
                            tagsList.add(t.trim());
                        }
                    }
                }

                String tagsText = tagsList.isEmpty() ? null : String.join(",", tagsList);
                boolean patchAvailable = hasPatchSignal(tagsList, refUrl);

                CveAdvisory advisory = advisoryByUrl.get(refUrl);
                if (advisory == null) {
                    advisory = new CveAdvisory();
                    advisory.setCveId(cveId);
                    advisory.setSource(source);
                    advisory.setUrl(refUrl);
                    advisory.setTags(tagsText);
                    advisory.setPatchAvailable(patchAvailable);
                    advisoryByUrl.put(refUrl, advisory);
                } else {
                    advisory.setPatchAvailable(advisory.isPatchAvailable() || patchAvailable);
                    if ((advisory.getTags() == null || advisory.getTags().isBlank()) && tagsText != null && !tagsText.isBlank()) {
                        advisory.setTags(tagsText);
                    }
                    if ((advisory.getSource() == null || advisory.getSource().isBlank()) && source != null && !source.isBlank()) {
                        advisory.setSource(source);
                    }
                }
            }
        }

        return new ArrayList<>(advisoryByUrl.values());
    }

    private static boolean hasPatchSignal(List<String> tags, String url) {
        boolean tagHit = tags.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .anyMatch(t -> t.contains("patch") || t.contains("vendor advisory") || t.contains("mitigation"));
        if (tagHit) {
            return true;
        }

        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return false;
            }
            String h = host.toLowerCase(Locale.ROOT);
            return h.contains("microsoft")
                    || h.contains("redhat")
                    || h.contains("oracle")
                    || h.contains("cisco")
                    || h.contains("adobe")
                    || h.contains("vmware")
                    || h.contains("support")
                    || h.contains("security");
        } catch (Exception ex) {
            return false;
        }
    }

    private static String textOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int saveAdvisoriesSafely(List<CveAdvisory> advisories) {
        int saved = 0;
        for (CveAdvisory advisory : advisories) {
            int rows = jdbcTemplate.update(
                    """
                    INSERT INTO cve_advisory (cve_id, patch_available, source, tags, updated_at, url)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
                    ON CONFLICT (cve_id, url) DO NOTHING
                    """,
                    advisory.getCveId(),
                    advisory.isPatchAvailable(),
                    advisory.getSource(),
                    advisory.getTags(),
                    advisory.getUrl()
            );
            saved += rows;
        }
        return saved;
    }

    public record NvdApiResponse(List<NvdVulnerability> vulnerabilities) {
    }

    public record NvdVulnerability(NvdCve cve) {
    }

    public record NvdCve(List<NvdReference> references) {
    }

    public record NvdReference(String url, String source, List<String> tags) {
    }

    public record RefreshStats(int itemsRead, int upserted, int skippedCves, int processedCves, int totalCves) {
    }
}
