package com.tmc.aac.features.kiosk.controller;

import com.tmc.aac.features.kiosk.service.DemoControlService;
import com.tmc.aac.features.kiosk.service.DemoControlService.MockView;
import com.tmc.aac.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("demo")
@RequestMapping("/api/v1/kiosk/demo")
public class DemoControlController {
    public record ModeRequest(@NotBlank @Size(max = 30) String mode) {}
    public record ResolveRequest(@NotBlank @Size(max = 30) String outcome) {}
    private final DemoControlService demo;
    public DemoControlController(DemoControlService demo) { this.demo = demo; }
    @GetMapping("/mocks") public ApiResponse<List<MockView>> states() { return ApiResponse.ok(demo.states()); }
    @PostMapping("/mocks/{service}")
    public ApiResponse<MockView> mode(@PathVariable String service, @Valid @RequestBody ModeRequest request) {
        return ApiResponse.ok(demo.mode(service, request.mode()));
    }
    @PostMapping("/transactions/{id}/resolve")
    public ApiResponse<Void> resolve(@PathVariable String id, @Valid @RequestBody ResolveRequest request) {
        demo.resolve(id, request.outcome());
        return ApiResponse.ok(null);
    }
}
