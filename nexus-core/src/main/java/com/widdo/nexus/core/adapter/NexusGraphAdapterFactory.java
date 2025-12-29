package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.properties.NexusProperties;

/**
 * NexusGraphAdapterFactory
 *
 * @author XYL
 * @date 2025/12/14 18:27
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusGraphAdapterFactory extends NexusAdapterFactory {
    NexusGraphAdapter createAdapter(NexusProperties properties);
}
