package com.widdo.nexus.core.query;

import com.widdo.nexus.core.enums.QueryExecutionType;
import com.widdo.nexus.core.result.NexusResult;

import java.util.Map;

/**
 * NexusHadoopExecutionStrategy
 *
 * @author XYL
 * @date 2025/12/14 20:35
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusHadoopExecutionStrategy {

    NexusResult execute(String query, Map<String, Object> parameters);

    QueryExecutionType getExecutionType();
}
