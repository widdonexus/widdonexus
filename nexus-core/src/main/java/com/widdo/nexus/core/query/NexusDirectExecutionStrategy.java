package com.widdo.nexus.core.query;

import com.widdo.nexus.core.adapter.NexusDatabaseAdapter;
import com.widdo.nexus.core.enums.QueryExecutionType;
import com.widdo.nexus.core.result.NexusGraphResultSet;

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
public class NexusDirectExecutionStrategy implements NexusQueryExecutionStrategy {

    private final NexusDatabaseAdapter adapter;

    public NexusDirectExecutionStrategy(NexusDatabaseAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public NexusGraphResultSet execute(String query, Map<String, Object> parameters) {
        return adapter.executeQuery(query, parameters);
    }

    @Override
    public QueryExecutionType getExecutionType() {
        return QueryExecutionType.DIRECT;
    }
}
