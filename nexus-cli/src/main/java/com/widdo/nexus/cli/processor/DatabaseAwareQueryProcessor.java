package com.widdo.nexus.cli.processor;

import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.result.NexusGraphResultSet;
import com.widdo.nexus.core.translator.NexusCypherToGremlinTranslator;
import com.widdo.nexus.core.translator.NexusQueryTranslator;
import com.widdo.nexus.core.translator.NexusRuleEngine;

import java.util.HashMap;
import java.util.Map;

public class DatabaseAwareQueryProcessor {

    private final NexusGraphAdapter adapter;
    private final Map<String, NexusQueryTranslator> translators;

    public DatabaseAwareQueryProcessor(NexusGraphAdapter adapter) {
        this.adapter = adapter;
        this.translators = new HashMap<>();
        // Neo4j 不需要转换
        this.translators.put("NEO4J", new IdentityTranslatorNexus());
        this.translators.put("JANUSGRAPH", new NexusCypherToGremlinTranslator());
        this.translators.put("NEPTUNE", new NexusCypherToGremlinTranslator());
    }

    public NexusGraphResultSet executeQuery(String query, Map<String, Object> parameters) {
        String databaseType = adapter.getDatabaseType();
        final NexusQueryTranslator translator = translators.get(databaseType);

        if (translator == null) {
            throw new UnsupportedOperationException(
                    "No query translator available for database: " + databaseType);
        }

        String translatedQuery = translator.translate(query, parameters);
        return adapter.executeQuery(translatedQuery, parameters);
    }

    // 简单的恒等转换器（用于 Neo4j）
    private static class IdentityTranslatorNexus implements NexusQueryTranslator {
        @Override
        public boolean supports(String databaseType) {
            return "NEO4j".equals(databaseType);
        }

        @Override
        public String translateCypherToNative(String cypherQuery, Map<String, Object> parameters) {
            return translate(cypherQuery, parameters);
        }

        @Override
        public String translate(String cypherQuery, Map<String, Object> parameters) {
            return NexusRuleEngine.translate(cypherQuery, parameters);
        }
    }
}