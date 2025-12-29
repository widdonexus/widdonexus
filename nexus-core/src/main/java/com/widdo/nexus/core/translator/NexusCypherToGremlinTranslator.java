package com.widdo.nexus.core.translator;

import java.util.Map;

// Gremlin 翻译器示例
public class NexusCypherToGremlinTranslator implements NexusQueryTranslator {
    @Override
    public boolean supports(String databaseType) {
        return "JANUSGRAPH".equals(databaseType) || "NEBULA".equals(databaseType);
    }

    @Override
    public String translateCypherToNative(String cypherQuery, Map<String, Object> parameters) {
        // 将 Cypher 翻译为 Gremlin
        // 这里可以使用第三方库或自定义规则引擎
        return translate(cypherQuery, parameters);
    }

    @Override
    public String translate(String cypherQuery, Map<String, Object> parameters) {
        return NexusRuleEngine.translate(cypherQuery, parameters);
    }
}