package com.widdo.nexus.hadoop.templete;

import com.widdo.nexus.core.adapter.NexusHadoopAdapter;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.core.support.template.AbstractNexusHadoopAdvancedTemplate;

/**
 * NexusHdfsAdvancedTemplate
 *
 * @author XYL
 * @date 2025/12/09 15:23
 * @since 0.0.1-SNAPSHOT
 */
public class NexusHdfsAdvancedTemplate extends AbstractNexusHadoopAdvancedTemplate {

    public NexusHdfsAdvancedTemplate(
            NexusHadoopAdapter adapter,
            NexusQueryLoader queryLoader,
            NexusProperties properties) {
        super(adapter, queryLoader, properties);
    }
}
