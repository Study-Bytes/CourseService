package org.studyplatform.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "System", description = "Technical health and readiness checks")
public class HealthController {

    @Operation(summary = "Health check", description = "Returns service health status.")
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @Operation(summary = "Readiness check", description = "Returns service readiness status.")
    @GetMapping("/ready")
    public Map<String, String> ready() {
        return Map.of("status", "UP");
    }
}
