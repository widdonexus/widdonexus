package com.widdo.nexus.neo4j.adapter;

import com.widdo.nexus.core.adapter.AbstractNexusGraphAdapterFactory;
import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.util.NexusPropertiesValidator;

import java.util.Optional;

/**
 * NexusNeo4JAdapterFactory.
 *
 * @author XYL
 * @date 2025/08/27 17:08
 * @since 0.0.1-SNAPSHOT
 */
public class NexusNeo4JAdapterFactory extends AbstractNexusGraphAdapterFactory {

    protected final NexusLogger logger = NexusLogger.getLogger(getClass());

    @Override
    protected String getSupportedDatabaseType() {
        return "NEO4J";
    }

    @Override
    public NexusGraphAdapter createAdapter(NexusProperties properties) {
        logger.info("Creating Nexus   Neo4j adapter");

        //校验配置
        NexusPropertiesValidator.validate(properties);

        final NexusProperties.Graph.Neo4j neo4j = properties.getGraph().getNeo4j();

        //如果配置文件中没有指定neo4j的数据库，默认使用neo4j
        final String database = Optional.ofNullable(properties.getGraph().getDatabase().getName()).orElse(neo4j.getDatabase());

        return new NexusNeo4jAdapter(
                neo4j.getUri(),
                neo4j.getUsername(),
                neo4j.getPassword(),
                database
        );
    }
}
