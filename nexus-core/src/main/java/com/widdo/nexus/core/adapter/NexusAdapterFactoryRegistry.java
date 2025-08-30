package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.entity.NexusEntityMapper;
import com.widdo.nexus.core.exception.NexusConfigurationException;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * NexusAdapterFactoryRegistry.
 * <p>
 * 适配器工厂注册表
 *
 * @author XYL
 * @date 2025/08/27 18:15
 * @since 0.0.1-SNAPSHOT
 */
public class NexusAdapterFactoryRegistry {

    private final List<NexusAdapterFactory> factories = new ArrayList<>();
    private final NexusLogger logger = NexusLogger.getLogger(getClass());

    public void registerFactory(NexusAdapterFactory factory) {
        factories.add(factory);
        logger.debug("Registered adapter factory: {0}", factory.getClass().getSimpleName());
    }

    public NexusDatabaseAdapter createAdapter(NexusProperties properties, NexusEntityMapper entityMapper) {
        String databaseType = properties.getDatabase().getType();

        return factories.stream()
                .filter(factory -> factory.supports(databaseType))
                .findFirst()
                .map(factory -> factory.createAdapter(properties, entityMapper))
                .orElseThrow(() -> new NexusConfigurationException(
                        "Unsupported database type: " + databaseType
                ));
    }
}
