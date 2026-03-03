
package com.vulscan.dashboard.controller;

import com.vulscan.dashboard.dto.ImportResultDto;
import com.vulscan.dashboard.service.InventoryImportService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasAnyRole('USER','ADMIN','SUPERADMIN')")
public class InventoryController {

    private final InventoryImportService inventoryImportService;

    public InventoryController(InventoryImportService inventoryImportService) {
        this.inventoryImportService = inventoryImportService;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultDto importInventory(@RequestPart("file") MultipartFile file) {
        System.out.println("InventoryController.importInventory called");
        System.out.println("File: " + file.getOriginalFilename() + ", size: " + file.getSize());
        ImportResultDto result = inventoryImportService.importCsv(file);
        System.out.println("Result: " + result.getRowsRead() + " rows read, " + result.getRowsInserted() + " inserted");
        return result;
    }
}
