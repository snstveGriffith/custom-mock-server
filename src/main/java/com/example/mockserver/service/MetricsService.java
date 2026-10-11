package com.example.mockserver.service;

import com.example.mockserver.model.MetricsSnapshot;
import com.example.mockserver.model.MockRule;
import com.example.mockserver.model.RuleMetrics;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class MetricsService {

    private static final int MAX_UNMATCHED_KEYS = 200; // evita crescer sem limite

    private final Instant startedAt = Instant.now();
    private final LongAdder total = new LongAdder();
    private final LongAdder matched = new LongAdder();
    private final LongAdder unmatched = new LongAdder();
    private final Map<String, RuleStats> byRule = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> unmatchedPaths = new ConcurrentHashMap<>();

    public void recordHit(MockRule rule, long latencyMs) {
        total.increment();
        matched.increment();
        byRule.computeIfAbsent(rule.id(), id -> new RuleStats(rule.method(), rule.path()))
              .record(latencyMs);
    }

    public void recordMiss(String method, String path) {
        total.increment();
        unmatched.increment();
        String key = method + " " + path;
        if (unmatchedPaths.size() < MAX_UNMATCHED_KEYS || unmatchedPaths.containsKey(key)) {
            unmatchedPaths.computeIfAbsent(key, k -> new LongAdder()).increment();
        }
    }

    public MetricsSnapshot snapshot() {
        List<RuleMetrics> rules = byRule.entrySet().stream().map(e -> {
            RuleStats s = e.getValue();
            long hits = s.hits.sum();
            double avg = hits == 0 ? 0 : (double) s.totalLatencyMs.sum() / hits;
            return new RuleMetrics(e.getKey(), s.method, s.path, hits,
                    Math.round(avg * 100.0) / 100.0, s.lastAccess);
        }).sorted(Comparator.comparingLong(RuleMetrics::hits).reversed()).toList();

        Map<String, Long> misses = new TreeMap<>();
        unmatchedPaths.forEach((k, v) -> misses.put(k, v.sum()));

        return new MetricsSnapshot(startedAt, total.sum(), matched.sum(), unmatched.sum(), rules, misses);
    }

    public void reset() {
        total.reset();
        matched.reset();
        unmatched.reset();
        byRule.clear();
        unmatchedPaths.clear();
    }

    private static final class RuleStats {
        final String method;
        final String path;
        final LongAdder hits = new LongAdder();
        final LongAdder totalLatencyMs = new LongAdder();
        volatile Instant lastAccess;

        RuleStats(String method, String path) {
            this.method = method;
            this.path = path;
        }

        void record(long latencyMs) {
            hits.increment();
            totalLatencyMs.add(latencyMs);
            lastAccess = Instant.now();
        }
    }
}