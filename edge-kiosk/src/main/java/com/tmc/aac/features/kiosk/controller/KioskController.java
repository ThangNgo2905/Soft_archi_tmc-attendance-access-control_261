package com.tmc.aac.features.kiosk.controller;

import com.tmc.aac.features.kiosk.service.KioskService;
import com.tmc.aac.features.kiosk.service.KioskViews.*;
import com.tmc.aac.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kiosk")
public class KioskController {
    public record ScanRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,100}") String scanId,
                              @NotBlank @Size(max = 2048) String payload) {}
    private final KioskService kiosk;
    public KioskController(KioskService kiosk) { this.kiosk = kiosk; }
    @PostMapping("/scans")
    public ApiResponse<TransactionView> scan(@Valid @RequestBody ScanRequest request) {
        return ApiResponse.ok(kiosk.scan(request.scanId(), request.payload()));
    }
    @GetMapping("/status") public ApiResponse<StatusView> status() { return ApiResponse.ok(kiosk.status()); }
}
