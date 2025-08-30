package com.widdo.nexus.core.support.query;

import java.util.Map;

/**
 * NexusQuery
 * <p>
 * Nexus查询对象
 *
 * @author XYL
 * @date 2025/08/27 15:27
 * @since 0.0.1-SNAPSHOT
 */
public class NexusQuery {

    private final String cypher;
    private final Map<String, Object> parameters;

    public NexusQuery(String cypher, Map<String, Object> parameters) {
        this.cypher = cypher;
        this.parameters = parameters;
    }

    public String getCypher() {
        return cypher;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "NexusQuery{" +
                "cypher='" + cypher + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
