package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.properties.NexusProperties;

/**
 * NexusHadoopAdapterFactory
 *
 * @author XYL
 * @date 2025/12/09 18:01
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusHadoopAdapterFactory extends NexusAdapterFactory {
    NexusHadoopAdapter createAdapter(NexusProperties properties);
}
