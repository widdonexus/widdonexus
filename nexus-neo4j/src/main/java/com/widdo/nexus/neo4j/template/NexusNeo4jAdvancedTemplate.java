package com.widdo.nexus.neo4j.template;

import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.entity.NexusEntityMapper;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.core.support.template.AbstractNexusAdvancedTemplate;

/**
 * NexusNeo4jAdvancedTemplate
 *
 * @author XYL
 * @date 2025/12/09 17:35
 * @since 0.0.1-SNAPSHOT
 */
public class NexusNeo4jAdvancedTemplate extends AbstractNexusAdvancedTemplate {

    public NexusNeo4jAdvancedTemplate(
            NexusGraphAdapter adapter,
                                      NexusEntityMapper entityMapper,
                                      NexusQueryLoader queryLoader,
                                      NexusProperties properties) {
        super(adapter, entityMapper, queryLoader, properties);
    }
}
