package com.example.mockserver.model;

import java.util.Map;
import java.util.UUID;

/**
 * Regra de mapeamento: "quando chegar [method] em [path], responda com [status]/[headers]/[body]
 * depois de [delayMs] milissegundos".
 */
public record MockRule(
        String id,
        String method,          // GET, POST, ... ou "*" para qualquer método
        String path,            // padrão Ant: /api/users/{id}, /api/**, /files/*.json
        Integer status,
        Map<String, String> headers,
        Object body,            // objeto JSON, lista ou texto
        Long delayMs) {

    // Construtor compacto: aplica valores padrão e valida
    public MockRule {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("O campo 'path' é obrigatório");
        }
        id = (id == null || id.isBlank()) ? UUID.randomUUID().toString() : id;
        method = (method == null || method.isBlank()) ? "*" : method.toUpperCase();
        status = (status == null) ? 200 : status;
        headers = (headers == null) ? Map.of() : Map.copyOf(headers);
        delayMs = (delayMs == null || delayMs < 0) ? 0L : delayMs;
    }

    public boolean matchesMethod(String requestMethod) {
        return "*".equals(method) || method.equalsIgnoreCase(requestMethod);
    }
}