package com.widdo.nexus.core.query;

import com.widdo.nexus.core.adapter.NexusDatabaseAdapter;
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
public class NexusExecutionStrategyFactory {

    private final NexusProperties properties;

    public NexusExecutionStrategyFactory(NexusProperties properties) {
        this.properties = properties;
    }

    public NexusQueryExecutionStrategy createStrategy(NexusDatabaseAdapter adapter) {
        if (properties.getExecution().isRetryEnabled()) {
            return new NexusRetryExecutionStrategy(
                    adapter,
                    properties.getExecution().getMaxRetries(),
                    properties.getExecution().getRetryDelayMs()
            );
        }

        return new NexusDirectExecutionStrategy(adapter);
    }
}
