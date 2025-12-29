package com.widdo.nexus.core.query;

import com.widdo.nexus.core.adapter.NexusDatabaseAdapter;
import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.adapter.NexusHadoopAdapter;
import com.widdo.nexus.core.properties.NexusProperties;

/**
 * NexusExecutionStrategyFactory.
 * <p>
 * 执行策略工厂
 *
 * @author XYL
 * @date 2025/08/27 18:20
 * @since 0.0.1-SNAPSHOT
 */
public record NexusExecutionStrategyFactory(NexusProperties properties) {

    public NexusQueryExecutionStrategy createStrategy(NexusDatabaseAdapter adapter) {

        if (adapter instanceof NexusGraphAdapter) {
            final NexusGraphAdapter nexusAdapter = (NexusGraphAdapter) adapter;

            if (properties.getGraph().getExecution().isRetryEnabled()) {
                return new NexusRetryExecutionStrategy(
                        nexusAdapter,
                        properties.getGraph().getExecution().getMaxRetries(),
                        properties.getGraph().getExecution().getRetryDelayMs()
                );
            }

            return new NexusDirectExecutionStrategy(nexusAdapter);
        }

        if (adapter instanceof NexusHadoopAdapter) {
            final NexusHadoopAdapter nexusAdapter = (NexusHadoopAdapter) adapter;
            return new NexusHadoopDirectExecutionStrategy(nexusAdapter);
        }

        return null;
    }
}
