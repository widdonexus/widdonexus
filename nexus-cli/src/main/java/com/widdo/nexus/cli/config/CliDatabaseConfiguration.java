package com.widdo.nexus.cli.config;

import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.entity.NexusEntityMapper;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.neo4j.adapter.NexusNeo4jAdapter;
import com.widdo.nexus.neo4j.template.NexusNeo4jAdvancedTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CliDatabaseConfiguration {

    @Bean
    @ConditionalOnProperty(name = "widdo.nexus.database.type", havingValue = "NEO4J")
    public NexusGraphAdapter neo4jAdapter(NexusProperties properties) {
        final NexusProperties.Graph.Neo4j config = properties.getGraph().getNeo4j();
        return new NexusNeo4jAdapter(
                config.getUri(),
                config.getUsername(),
                config.getPassword(),
                properties.getGraph().getDatabase().getName()
        );
    }

    @Bean
    public NexusNeo4jAdvancedTemplate nexusAdvancedTemplate(
            NexusGraphAdapter adapter,
            NexusEntityMapper entityMapper,
            NexusQueryLoader queryLoader,
            NexusProperties properties) {
        return new NexusNeo4jAdvancedTemplate(adapter, entityMapper, queryLoader, properties);
    }
}