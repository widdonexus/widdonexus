package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.log.NexusLogger;

/**
 * AbstractNexusAdapterFactory.
 * <p>
 * 抽象数据库适配器工厂
 *
 * @author XYL
 * @date 2025/08/27 17:07
 * @since 0.0.1-SNAPSHOT
 */
public abstract class AbstractNexusAdapterFactory implements NexusAdapterFactory {

    protected final NexusLogger logger = NexusLogger.getLogger(getClass());

    @Override
    public boolean supports(String databaseType) {
        return getSupportedDatabaseType().equalsIgnoreCase(databaseType);
    }

    protected abstract String getSupportedDatabaseType();
}
