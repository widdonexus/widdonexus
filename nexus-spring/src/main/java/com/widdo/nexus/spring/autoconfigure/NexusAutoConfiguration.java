package com.widdo.nexus.spring.autoconfigure;

import com.widdo.nexus.core.adapter.NexusAdapterFactoryRegistry;
import com.widdo.nexus.core.adapter.NexusDatabaseAdapter;
import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.adapter.NexusHadoopAdapter;
import com.widdo.nexus.core.entity.NexusEntityMapper;
import com.widdo.nexus.core.health.NexusHealthIndicator;
import com.widdo.nexus.core.log.LogMessages;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.core.support.template.AbstractNexusAdvancedTemplate;
import com.widdo.nexus.hadoop.adapter.NexusHdfsAdapterFactory;
import com.widdo.nexus.hadoop.templete.NexusHdfsAdvancedTemplate;
import com.widdo.nexus.neo4j.adapter.NexusNeo4JAdapterFactory;
import com.widdo.nexus.neo4j.template.NexusNeo4jAdvancedTemplate;
import com.widdo.nexus.spring.handler.NexusExceptionHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * NexusAutoConfiguration.
 *
 * @author XYL
 * @date 2025/08/27 20:18
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NexusProperties.class)
@ConditionalOnClass({AbstractNexusAdvancedTemplate.class, NexusDatabaseAdapter.class})
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class NexusAutoConfiguration {

    private final NexusLogger logger = NexusLogger.getLogger(getClass());

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @PostConstruct
    public void init() {
        logger.info(LogMessages.STARTUP_BANNER, Optional.ofNullable(buildProperties.getVersion()).orElse(""));
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusEntityMapper nexusEntityMapper() {
        return new NexusEntityMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusQueryLoader nexusQueryLoader(NexusProperties properties) {
        NexusQueryLoader loader = new NexusQueryLoader();

        //加载图谱查询
        loadGraphYaml(loader, properties);

        //加载hadoop查询
        loadHadoopYaml(loader, properties);

        return loader;
    }

    private void loadHadoopYaml(NexusQueryLoader loader, NexusProperties properties) {
        // 加载图库默认查询
//        loader.loadFromYaml("classpath:nexus/queries/default.yml");
        loader.loadFromYaml("nexus/hadoop/queries/default.yml");

        // 加载数据库特定查询
        String databaseType = properties.getGraph().getDatabase().getType().toLowerCase();
//        loader.loadFromYaml("classpath:nexus/queries/" + databaseType + ".yml");
        loader.loadFromYaml("nexus/hadoop/queries/" + databaseType + ".yml");

        // 加载自定义查询位置
        if (properties.getQueryLocations() != null) {
            for (String location : properties.getQueryLocations()) {
                loader.loadFromYaml(location);
            }
        }

        //加载@Query和@QueryRef注解所在包下的查询
        if (!properties.getScan().getBasePackages().isEmpty()) {
            loader.scanAnnotations(properties.getScan().getBasePackages().toArray(new String[0]));
        }
    }

    private void loadGraphYaml(NexusQueryLoader loader, NexusProperties properties) {
        // 加载图库默认查询
//        loader.loadFromYaml("classpath:nexus/queries/default.yml");
        loader.loadFromYaml("nexus/graph/queries/default.yml");

        // 加载数据库特定查询
        String databaseType = properties.getGraph().getDatabase().getType().toLowerCase();
//        loader.loadFromYaml("classpath:nexus/queries/" + databaseType + ".yml");
        loader.loadFromYaml("nexus/graph/queries/" + databaseType + ".yml");

        // 加载自定义查询位置
        if (properties.getQueryLocations() != null) {
            for (String location : properties.getQueryLocations()) {
                loader.loadFromYaml(location);
            }
        }

        //加载@Query和@QueryRef注解所在包下的查询
        if (!properties.getScan().getBasePackages().isEmpty()) {
            loader.scanAnnotations(properties.getScan().getBasePackages().toArray(new String[0]));
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusNeo4jAdvancedTemplate nexusNeo4jAdvancedTemplate(
            NexusGraphAdapter adapter,
            NexusEntityMapper entityMapper,
            NexusQueryLoader queryLoader,
            NexusProperties properties) {
        return new NexusNeo4jAdvancedTemplate(adapter, entityMapper, queryLoader, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusHdfsAdvancedTemplate nexusHdfsAdvancedTemplate(
            NexusHadoopAdapter adapter,
            NexusQueryLoader queryLoader,
            NexusProperties properties) {
        return new NexusHdfsAdvancedTemplate(adapter, queryLoader, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusExceptionHandler nexusExceptionHandler(NexusProperties properties) {
        return new NexusExceptionHandler(properties);
    }

    // 健康检查
    @Bean
    @ConditionalOnEnabledHealthIndicator("nexus")
    public NexusHealthIndicator nexusHealthIndicator(NexusGraphAdapter adapter) {
        return new NexusHealthIndicator(adapter);
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusAdapterFactoryRegistry nexusAdapterFactoryRegistry() {
        NexusAdapterFactoryRegistry registry = new NexusAdapterFactoryRegistry();

        //图谱适配器工厂
        registry.registerFactory(new NexusNeo4JAdapterFactory());

        //Hadoop适配器工厂
        registry.registerFactory(new NexusHdfsAdapterFactory());
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusHadoopAdapter nexusHadoopAdapter(
            NexusAdapterFactoryRegistry registry,
            NexusProperties properties) {
        logger.info(LogMessages.DATABASE_ADAPTER_INIT, properties.getGraph().getDatabase().getType().toUpperCase());
        return registry.createHadoopAdapter(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusGraphAdapter nexusNeo4jAdapter(
            NexusAdapterFactoryRegistry registry,
            NexusProperties properties) {
        logger.info(LogMessages.DATABASE_ADAPTER_INIT, properties.getGraph().getDatabase().getType().toUpperCase());
        return registry.createGraphAdapter(properties);
    }
}
