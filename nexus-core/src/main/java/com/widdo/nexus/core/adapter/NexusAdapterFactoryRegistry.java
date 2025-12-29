package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.exception.NexusConfigurationException;
import com.widdo.nexus.core.log.LogMessages;
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

    private final List<NexusGraphAdapterFactory> graphFactories = new ArrayList<>();
    private final List<NexusHadoopAdapterFactory> hadoopFactories = new ArrayList<>();
    private final NexusLogger logger = NexusLogger.getLogger(getClass());

    public void registerFactory(NexusAdapterFactory factory) {

        //图数据库适配器工厂
        if (factory instanceof NexusGraphAdapterFactory) {
            graphFactories.add((NexusGraphAdapterFactory) factory);
        }

        //Hadoop适配器工厂
        if (factory instanceof NexusHadoopAdapterFactory) {
            hadoopFactories.add((NexusHadoopAdapterFactory) factory);
        }

        logger.debug(LogMessages.DATABASE_ADAPTER_REGISTER, factory.getClass().getSimpleName());
    }

    public NexusGraphAdapter createGraphAdapter(NexusProperties properties) {

        //分别处理graph和hadoop的adapter
        String databaseType = properties.getGraph().getDatabase().getType();

        //分别处理graph和hadoop的配置
        return graphFactories.stream()
                .filter(factory -> factory.supports(databaseType))
                .findFirst()
                .map(factory -> factory.createAdapter(properties))
                .orElseThrow(() -> new NexusConfigurationException(
                        "Unsupported database type: " + databaseType
                ));
    }

    public NexusHadoopAdapter createHadoopAdapter(NexusProperties properties) {

        //分别处理graph和hadoop的adapter
        String databaseType = properties.getHadoop().getDatabase().getType();

        //分别处理graph和hadoop的配置
        return hadoopFactories.stream()
                .filter(factory -> factory.supports(databaseType))
                .findFirst()
                .map(factory -> factory.createAdapter(properties))
                .orElseThrow(() -> new NexusConfigurationException(
                        "Unsupported database type: " + databaseType
                ));
    }
}
