package com.vulscan.dashboard.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.vulscan.dashboard.dto.ImportResultDto;
import com.vulscan.dashboard.entity.InventoryItem;
import com.vulscan.dashboard.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class InventoryImportService {

    private final InventoryRepository inventoryRepository;

    private static final int MAX_ERRORS = 20;

    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    public InventoryImportService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public ImportResultDto importCsv(MultipartFile file) {
        System.out.println("importCsv called");
        if (file == null || file.isEmpty()) {
            return new ImportResultDto(0, 0, 0, List.of("No file uploaded."));
        }

        System.out.println("File received: " + file.getOriginalFilename() + ", size: " + file.getSize());
        int rowsRead = 0;
        int rowsRejected = 0;
        List<String> errors = new ArrayList<>();
        List<InventoryItem> items = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            System.out.println("CSVReader created");
            String[] header = reader.readNext();
            if (header == null) {
                return new ImportResultDto(0, 0, 0, List.of("Empty CSV."));
            }

            System.out.println("Header: " + String.join(",", header));
            Map<String, Integer> idx = buildHeaderIndex(header);

            if (!idx.containsKey("hostname") || !idx.containsKey("product")) {
                return new ImportResultDto(0, 0, 0, List.of(
                        "Missing required columns. Required: hostname, product. Found: " + String.join(", ", idx.keySet())
                ));
            }

            String[] row;
            int lineNo = 1;
            while ((row = reader.readNext()) != null) {
                lineNo++;

                // üres sor skip
                if (row.length == 0) continue;

                rowsRead++;

                String hostname = get(row, idx, "hostname");
                String product = get(row, idx, "product");

                if (isBlank(hostname)) {
                    rowsRejected++;
                    addError(errors, "line " + lineNo + ": missing hostname");
                    continue;
                }
                if (isBlank(product)) {
                    rowsRejected++;
                    addError(errors, "line " + lineNo + ": missing product");
                    continue;
                }

                InventoryItem item = new InventoryItem();
                item.setHostname(hostname.trim());
                item.setProduct(product.trim());

                item.setVendor(get(row, idx, "vendor"));
                item.setVersion(get(row, idx, "version"));
                item.setCpe(get(row, idx, "cpe"));

                String source = get(row, idx, "source");
                if (!isBlank(source)) item.setSource(source.trim());

                String installedRaw = get(row, idx, "installedon"); // installedOn -> installedon
                item.setInstalledOn(parseDateOrNull(installedRaw));
                
                item.setLastSeenAt(LocalDateTime.now());

                items.add(item);
            }

        } catch (CsvValidationException e) {
            System.out.println("CsvValidationException: " + e.getMessage());
            e.printStackTrace();
            return new ImportResultDto(rowsRead, 0, rowsRead, List.of("Invalid CSV: " + e.getMessage()));
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
            return new ImportResultDto(rowsRead, 0, rowsRead, List.of("Failed to read CSV: " + e.getMessage()));
        }

        System.out.println("Deleting all and saving " + items.size() + " items");
        inventoryRepository.deleteAllInBatch();
        inventoryRepository.saveAll(items);

        System.out.println("Import complete. Rows read: " + rowsRead + ", inserted: " + items.size());
        return new ImportResultDto(rowsRead, items.size(), rowsRejected, errors);
    }

    private static Map<String, Integer> buildHeaderIndex(String[] headerCols) {
        Map<String, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < headerCols.length; i++) {
            String key = headerCols[i];
            if (key == null) continue;

            key = stripBom(key).trim();
            if (key.isEmpty()) continue;

            idx.put(key.toLowerCase(), i);
        }
        return idx;
    }

    private static String stripBom(String s) {
        if (s == null) return null;
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static String get(String[] row, Map<String, Integer> idx, String colNameLower) {
        Integer i = idx.get(colNameLower);
        if (i == null) return null;
        if (i < 0 || i >= row.length) return null;
        String v = row[i];
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void addError(List<String> errors, String msg) {
        if (errors.size() < MAX_ERRORS) errors.add(msg);
    }

    private static LocalDate parseDateOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw.trim(), fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
