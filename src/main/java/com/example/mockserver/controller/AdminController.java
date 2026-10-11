package com.example.mockserver.controller;

import com.example.mockserver.config.RulesLoader;
import com.example.mockserver.model.MetricsSnapshot;
import com.example.mockserver.model.MockRule;
import com.example.mockserver.service.MetricsService;
import com.example.mockserver.service.RuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/__admin")
public class AdminController {

    private final RuleService ruleService;
    private final MetricsService metricsService;
    private final RulesLoader rulesLoader;

    public AdminController(RuleService ruleService, MetricsService metricsService, RulesLoader rulesLoader) {
        this.ruleService = ruleService;
        this.metricsService = metricsService;
        this.rulesLoader = rulesLoader;
    }

    @GetMapping("/rules")
    public List<MockRule> listRules() {
        return ruleService.findAll();
    }

    @PostMapping("/rules")
    public ResponseEntity<MockRule> createRule(@RequestBody MockRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.add(rule));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable String id) {
        return ruleService.remove(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/rules")
    public ResponseEntity<Void> clearRules() {
        ruleService.clear();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload() {
        try {
            return ResponseEntity.ok(Map.of("loaded", rulesLoader.load()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/metrics")
    public MetricsSnapshot metrics() {
        return metricsService.snapshot();
    }

    @DeleteMapping("/metrics")
    public ResponseEntity<Void> resetMetrics() {
        metricsService.reset();
        return ResponseEntity.noContent().build();
    }
}