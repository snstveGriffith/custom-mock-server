package com.example.mockserver.config;

import com.example.mockserver.model.MockRule;
import com.example.mockserver.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Carrega as regras do arquivo JSON na inicialização (e sob demanda via /__admin/reload). */
@Component
public class RulesLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RulesLoader.class);

    private final JsonMapper jsonMapper;
    private final RuleService ruleService;
    private final Resource rulesFile;

    public RulesLoader(JsonMapper jsonMapper,
                       RuleService ruleService,
                       @Value("${mock.rules-file}") Resource rulesFile) {
        this.jsonMapper = jsonMapper;
        this.ruleService = ruleService;
        this.rulesFile = rulesFile;
    }

    @Override
    public void run(ApplicationArguments args) {
        int count = load();
        log.info("{} regra(s) carregada(s) de {}", count, rulesFile);
    }

    public int load() {
        if (!rulesFile.exists()) {
            log.warn("Arquivo de regras não encontrado: {}", rulesFile);
            return 0;
        }
        try (InputStream in = rulesFile.getInputStream()) {
            List<MockRule> rules = jsonMapper.readValue(in, new TypeReference<List<MockRule>>() {});
            ruleService.replaceAll(rules);
            return rules.size();
        } catch (IOException | JacksonException e) {
            throw new IllegalStateException("Falha ao ler o arquivo de regras: " + e.getMessage(), e);
        }
    }
}