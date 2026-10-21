package com.kiki.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "kiki-ai-backend",
            "version", "1.0.0",
            "javaVersion", System.getProperty("java.version"),
            "timestamp", Instant.now().toString(),
            "message", "Kiki AI Backend Engine đang hoạt động ổn định!"
        ));
    }
}
