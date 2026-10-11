package com.example.mockserver.controller;

import com.example.mockserver.model.MockRule;
import com.example.mockserver.service.MetricsService;
import com.example.mockserver.service.RuleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class MockController {

    private final RuleService ruleService;
    private final MetricsService metricsService;

    public MockController(RuleService ruleService, MetricsService metricsService) {
        this.ruleService = ruleService;
        this.metricsService = metricsService;
    }

    // "/**" captura qualquer método e qualquer caminho que não tenha um mapeamento mais específico
    // (como os /__admin/...).
    @RequestMapping("/**")
    public ResponseEntity<Object> handle(HttpServletRequest request) {
        long start = System.nanoTime();
        String method = request.getMethod();
        String path = request.getRequestURI();

        Optional<MockRule> match = ruleService.findMatch(method, path);

        if (match.isEmpty()) {
            metricsService.recordMiss(method, path);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Nenhuma regra de mock encontrada");
            error.put("method", method);
            error.put("path", path);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        MockRule rule = match.get();
        simulateDelay(rule.delayMs());

        HttpHeaders headers = new HttpHeaders();
        rule.headers().forEach(headers::add);
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        metricsService.recordHit(rule, elapsedMs);

        return new ResponseEntity<>(rule.body(), headers, HttpStatusCode.valueOf(rule.status()));
    }

    private void simulateDelay(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}