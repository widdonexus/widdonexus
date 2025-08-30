package com.widdo.nexus.core.query;

import com.widdo.nexus.core.enums.QueryExecutionType;
import com.widdo.nexus.core.result.NexusGraphResultSet;

import java.util.Map;

/**
 * NexusQueryExecutionStrategy.
 * <p>
 * 查询执行策略接口
 *
 * @author XYL
 * @date 2025/08/27 18:17
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusQueryExecutionStrategy {

    NexusGraphResultSet execute(String query, Map<String, Object> parameters);

    QueryExecutionType getExecutionType();
}
