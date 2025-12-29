package com.widdo.nexus.core.query;

import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.enums.QueryExecutionType;
import com.widdo.nexus.core.result.NexusGraphResultSet;
import com.widdo.nexus.core.support.template.NexusQueryContext;

import java.util.Map;

/**
 * NexusDirectExecutionStrategy.
 * <p>
 * 直接执行策略
 *
 * @author XYL
 * @date 2025/08/27 18:19
 * @since 0.0.1-SNAPSHOT
 */
public record NexusDirectExecutionStrategy(
        NexusGraphAdapter adapter) implements NexusQueryExecutionStrategy<NexusGraphAdapter, NexusGraphResultSet> {

    @Override
    public NexusGraphResultSet execute(String query, Map<String, Object> parameters, NexusQueryContext context) {
        return adapter.executeQuery(query, parameters);
    }

    @Override
    public QueryExecutionType getExecutionType() {
        return QueryExecutionType.DIRECT;
    }
}
