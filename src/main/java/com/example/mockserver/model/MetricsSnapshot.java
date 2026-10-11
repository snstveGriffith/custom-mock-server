package com.example.mockserver.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MetricsSnapshot(
        Instant startedAt,
        long totalRequests,
        long matchedRequests,
        long unmatchedRequests,
        List<RuleMetrics> rules,
        Map<String, Long> unmatchedPaths) {
}