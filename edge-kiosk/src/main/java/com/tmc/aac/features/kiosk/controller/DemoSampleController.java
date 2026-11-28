package com.tmc.aac.features.kiosk.controller;

import com.tmc.aac.features.kiosk.service.DemoSampleService;
import com.tmc.aac.features.kiosk.service.KioskViews.SampleView;
import com.tmc.aac.shared.api.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile("demo")
@RestController
@RequestMapping("/api/v1/kiosk/demo")
public class DemoSampleController {
    private final DemoSampleService samples;
    public DemoSampleController(DemoSampleService samples) { this.samples = samples; }
    @GetMapping("/samples/{kind}")
    public ApiResponse<SampleView> sample(@PathVariable String kind) { return ApiResponse.ok(samples.sample(kind)); }
}
