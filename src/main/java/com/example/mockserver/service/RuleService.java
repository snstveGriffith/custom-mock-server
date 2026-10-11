package com.example.mockserver.service;

import com.example.mockserver.model.MockRule;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RuleService {

    private final List<MockRule> rules = new CopyOnWriteArrayList<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    /** Adiciona a regra; substitui a existente com mesmo id ou mesmo método+path. */
    public MockRule add(MockRule rule) {
        rules.removeIf(r -> r.id().equals(rule.id())
                || (r.method().equals(rule.method()) && r.path().equals(rule.path())));
        rules.add(rule);
        return rule;
    }

    public void replaceAll(List<MockRule> newRules) {
        rules.clear();
        newRules.forEach(this::add);
    }

    public List<MockRule> findAll() {
        return List.copyOf(rules);
    }

    public boolean remove(String id) {
        return rules.removeIf(r -> r.id().equals(id));
    }

    public void clear() {
        rules.clear();
    }

    /**
     * Retorna a regra mais específica que casa com método + caminho.
     * Ex.: "/api/users/{id}" vence "/api/**"; método exato vence "*".
     */
    public Optional<MockRule> findMatch(String method, String path) {
        Comparator<String> bySpecificity = matcher.getPatternComparator(path);
        Comparator<MockRule> comparator = Comparator
                .comparing(MockRule::path, bySpecificity)
                .thenComparingInt(r -> "*".equals(r.method()) ? 1 : 0);

        return rules.stream()
                .filter(r -> r.matchesMethod(method) && matcher.match(r.path(), path))
                .min(comparator);
    }
}