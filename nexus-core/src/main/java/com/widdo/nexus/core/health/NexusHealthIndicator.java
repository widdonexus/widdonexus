package com.widdo.nexus.core.health;

import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * NexusHealthIndicator.
 *
 * @author XYL
 * @date 2025/08/27 18:03
 * @since 0.0.1-SNAPSHOT
 */
public class NexusHealthIndicator implements HealthIndicator {

    private NexusGraphAdapter adapter;

    public NexusHealthIndicator(NexusGraphAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public Health health() {
        return adapter.healthCheck();
    }
}
