package com.widdo.nexus.hadoop.adapter;

import com.widdo.nexus.core.adapter.AbstractNexusHadoopAdapterFactory;
import com.widdo.nexus.core.adapter.NexusHadoopAdapter;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.util.NexusPropertiesValidator;

import java.util.Optional;

/**
 * NexusNeo4jAdapterFactory.
 * <p>
 * HDFS自有的服务适配器
 *
 * @author XYL
 * @date 2025/08/27 17:08
 * @since 0.0.1-SNAPSHOT
 */
public class NexusHdfsAdapterFactory extends AbstractNexusHadoopAdapterFactory {

    protected final NexusLogger logger = NexusLogger.getLogger(getClass());

    @Override
    protected String getSupportedDatabaseType() {
        return "HDFS";
    }

    @Override
    public NexusHadoopAdapter createAdapter(NexusProperties properties) {
        logger.info("Creating Nexus Hadoop adapter");

        //校验配置
        NexusPropertiesValidator.validate(properties);

        final NexusProperties.Hadoop.Hdfs hdfs = properties.getHadoop().getHdfs();

        //如果配置文件中没有指定neo4j的数据库，默认使用neo4j
        final String nnaddr = Optional.ofNullable(properties.getHadoop().getHdfs().getNn().getInsideAddr()).orElse("");

        return new NexusHdfsAdapter(
                nnaddr,
                hdfs.getUsername()
        );
    }
}
