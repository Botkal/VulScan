package com.vulscan.dashboard.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vulscan.dashboard.entity.EpssEntry;
import com.vulscan.dashboard.repository.EpssEntryRepository;
import com.vulscan.dashboard.repository.KevEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

@Service
public class EpssRefreshService {

    private final WebClient webClient;
    private final KevEntryRepository kevRepo;
    private final EpssEntryRepository epssRepo;

    public EpssRefreshService(WebClient.Builder webClientBuilder,
                              KevEntryRepository kevRepo,
                              EpssEntryRepository epssRepo) {
        this.webClient = webClientBuilder.build();
        this.kevRepo = kevRepo;
        this.epssRepo = epssRepo;
    }

    @Transactional
    public RefreshStats refreshFromKevCves(String epssBaseUrl) {
        ensureNotInterrupted();
        List<String> cves = kevRepo.findAll().stream()
                .map(k -> k.getCveId())
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (cves.isEmpty()) {
            return new RefreshStats(0, 0);
        }

        final int batchSize = 50;
        int read = 0;
        int upserted = 0;

        for (int i = 0; i < cves.size(); i += batchSize) {
            ensureNotInterrupted();
            int end = Math.min(i + batchSize, cves.size());
            List<String> batch = cves.subList(i, end);

            String cveParam = String.join(",", batch);
            String url = UriComponentsBuilder.fromUriString(epssBaseUrl)
                    .queryParam("cve", cveParam)
                    .toUriString();

            EpssApiResponse response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(EpssApiResponse.class)
                    .block();

            if (response == null || response.data() == null) {
                continue;
            }

            read += response.data().size();
            for (EpssItem item : response.data()) {
                ensureNotInterrupted();
                if (item == null || item.cve() == null || item.cve().isBlank()) {
                    continue;
                }

                EpssEntry entry = new EpssEntry();
                entry.setCveId(item.cve().trim());
                entry.setEpssScore(parseDouble(item.epss()));
                entry.setPercentile(parseDouble(item.percentile()));
                entry.setFeedDate(parseDate(item.date()));
                epssRepo.save(entry);
                upserted++;
            }
        }

        return new RefreshStats(read, upserted);
    }

    private static void ensureNotInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Refresh was cancelled");
        }
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public record RefreshStats(int itemsRead, int upserted) {
    }

    public record EpssApiResponse(List<EpssItem> data) {
    }

    public record EpssItem(
            String cve,
            String epss,
            String percentile,
            @JsonProperty("date") String date
    ) {
    }
}
