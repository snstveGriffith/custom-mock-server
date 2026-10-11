package com.example.mockserver.model;

import java.time.Instant;

public record RuleMetrics(
        String ruleId,
        String method,
        String path,
        long hits,
        double avgLatencyMs,
        Instant lastAccess) {
}