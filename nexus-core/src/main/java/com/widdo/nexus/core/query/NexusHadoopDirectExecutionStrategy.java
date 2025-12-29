package com.widdo.nexus.core.query;

import com.widdo.nexus.core.adapter.NexusHadoopAdapter;
import com.widdo.nexus.core.enums.QueryExecutionType;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.support.template.NexusQueryContext;

import java.util.Map;

/**
 * NexusHadoopDirectExecutionStrategy
 *
 * @author XYL
 * @date 2025/12/14 21:28
 * @since 0.0.1-SNAPSHOT
 */
public record NexusHadoopDirectExecutionStrategy(
        NexusHadoopAdapter adapter) implements NexusQueryExecutionStrategy<NexusHadoopAdapter, NexusResult> {

    @Override
    public NexusResult execute(String query, Map<String, Object> parameters, NexusQueryContext context) {
        return adapter.execute(query, parameters, context);
    }

    @Override
    public QueryExecutionType getExecutionType() {
        return QueryExecutionType.DIRECT;
    }
}
