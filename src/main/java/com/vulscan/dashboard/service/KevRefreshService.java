package com.vulscan.dashboard.service;

import com.vulscan.dashboard.dto.KevCatalogDto;
import com.vulscan.dashboard.dto.KevDto;
import com.vulscan.dashboard.dto.KevRefreshResultDto;
import com.vulscan.dashboard.entity.KevEntry;
import com.vulscan.dashboard.entity.KevRefreshLog;
import com.vulscan.dashboard.repository.KevEntryRepository;
import com.vulscan.dashboard.repository.KevRefreshLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CancellationException;

@Service
public class KevRefreshService {

    private final String defaultKevUrl;

    private final WebClient webClient;
    private final KevEntryRepository kevRepo;
    private final KevRefreshLogRepository logRepo;

    public KevRefreshService(WebClient.Builder webClientBuilder,
                             @Value("${app.kev-feed-url:https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json}") String defaultKevUrl,
                             KevEntryRepository kevRepo,
                             KevRefreshLogRepository logRepo) {
        this.webClient = webClientBuilder.build();
        this.defaultKevUrl = defaultKevUrl;
        this.kevRepo = kevRepo;
        this.logRepo = logRepo;
    }

    @Transactional
    public KevRefreshResultDto refreshKev() {
        return refreshKev(defaultKevUrl);
    }

    @Transactional
    public KevRefreshResultDto refreshKev(String feedUrl) {
        ensureNotInterrupted();
        int before = safeCountKev();

        KevCatalogDto catalog = webClient.get()
                .uri(feedUrl)
                .retrieve()
                .bodyToMono(KevCatalogDto.class)
                .block();

        if (catalog == null || catalog.vulnerabilities() == null) {
            int after = safeCountKev();
            saveLog(0, 0, before, after);
            return new KevRefreshResultDto(0, 0);
        }

        int upserted = 0;

        for (KevDto v : catalog.vulnerabilities()) {
            ensureNotInterrupted();
            if (v == null || v.cveID() == null || v.cveID().isBlank()) continue;

            KevEntry e = new KevEntry();
            e.setCveId(v.cveID().trim());
            e.setVendorProject(trimToNull(v.vendorProject()));
            e.setProduct(trimToNull(v.product()));
            e.setVulnerabilityName(v.vulnerabilityName());
            e.setDateAdded(parseDateSafe(v.dateAdded()));
            e.setShortDescription(v.shortDescription());
            e.setRequiredAction(v.requiredAction());
            e.setDueDate(parseDateSafe(v.dueDate()));
            e.setKnownRansomwareCampaignUse(parseRansomwareUse(v.knownRansomwareCampaignUse()));

            kevRepo.save(e); // PK=cve_id miatt insert/update
            upserted++;
        }

        int after = safeCountKev();
        saveLog(catalog.vulnerabilities().size(), upserted, before, after);

        return new KevRefreshResultDto(catalog.vulnerabilities().size(), upserted);
    }

    private static void ensureNotInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Refresh was cancelled");
        }
    }

    private int safeCountKev() {
        long c = kevRepo.count();
        return c > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) c;
    }

    private void saveLog(int vulnsRead, int upserted, int before, int after) {
        KevRefreshLog log = new KevRefreshLog();
        log.setVulnsRead(vulnsRead);
        log.setUpserted(upserted);
        log.setKevCountBefore(before);
        log.setKevCountAfter(after);
        logRepo.save(log);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static LocalDate parseDateSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); }
        catch (DateTimeParseException ex) { return null; }
    }

    private static Boolean parseRansomwareUse(Object s) {
        if (s == null) return null;
        if (s instanceof Boolean b) return b;
        String str = s.toString().trim().toLowerCase();
        return switch (str) {
            case "true", "yes" -> Boolean.TRUE;
            case "false", "no" -> Boolean.FALSE;
            default -> null; // e.g. "unknown"
        };
    }
}
