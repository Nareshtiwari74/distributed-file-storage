package com.dfs.health.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
        String status,
        String application,
        String version,
        Map<String, String> components,
        Instant timestamp) {
}
