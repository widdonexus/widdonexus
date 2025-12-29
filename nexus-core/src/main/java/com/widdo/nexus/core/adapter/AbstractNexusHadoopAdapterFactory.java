package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.log.NexusLogger;

/**
 * AbstractNexusHadoopAdapterFactory
 *
 * @author XYL
 * @date 2025/12/14 18:18
 * @since 0.0.1-SNAPSHOT
 */
public abstract class AbstractNexusHadoopAdapterFactory implements NexusHadoopAdapterFactory {

    protected final NexusLogger logger = NexusLogger.getLogger(getClass());

    @Override
    public boolean supports(String databaseType) {
        return getSupportedDatabaseType().equalsIgnoreCase(databaseType);
    }

    protected abstract String getSupportedDatabaseType();
}
