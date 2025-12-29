package com.widdo.nexus.core.translator;

import java.util.Map;

public interface NexusQueryTranslator {
    boolean supports(String databaseType);
    String translateCypherToNative(String cypherQuery, Map<String, Object> parameters);
    String translate(String cypherQuery, Map<String, Object> parameters);
}