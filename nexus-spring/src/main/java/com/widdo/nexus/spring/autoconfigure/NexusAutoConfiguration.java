package com.widdo.nexus.spring.autoconfigure;

import com.widdo.nexus.core.adapter.NexusAdapterFactoryRegistry;
import com.widdo.nexus.core.adapter.NexusDatabaseAdapter;
import com.widdo.nexus.core.entity.NexusEntityMapper;
import com.widdo.nexus.core.log.LogMessages;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.core.support.template.NexusAdvancedTemplate;
import com.widdo.nexus.neo4j.adapter.NexusNeo4jAdapterFactory;
import com.widdo.nexus.spring.handler.NexusExceptionHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NexusAutoConfiguration.
 *
 * @author XYL
 * @date 2025/08/27 20:18
 * @since 0.0.1-SNAPSHOT
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NexusProperties.class)
@ConditionalOnClass({NexusAdvancedTemplate.class, NexusDatabaseAdapter.class})
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class NexusAutoConfiguration {

    private final NexusLogger logger = NexusLogger.getLogger(getClass());

    @PostConstruct
    public void init() {
        logger.info(LogMessages.STARTUP_BANNER, getClass().getPackage().getImplementationVersion());
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusAdapterFactoryRegistry nexusAdapterFactoryRegistry() {
        NexusAdapterFactoryRegistry registry = new NexusAdapterFactoryRegistry();
        registry.registerFactory(new NexusNeo4jAdapterFactory());
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusEntityMapper nexusEntityMapper() {
        return new NexusEntityMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusDatabaseAdapter nexusDatabaseAdapter(
            NexusAdapterFactoryRegistry registry,
            NexusProperties properties,
            NexusEntityMapper entityMapper) {
        logger.info(LogMessages.DATABASE_ADAPTER_INIT, properties.getDatabase().getType().toUpperCase());
        return registry.createAdapter(properties, entityMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusQueryLoader nexusQueryLoader(NexusProperties properties) {
        NexusQueryLoader loader = new NexusQueryLoader();

        // 加载默认查询
//        loader.loadFromYaml("classpath:nexus/queries/default.yml");
        loader.loadFromYaml("nexus/queries/default.yml");

        // 加载数据库特定查询
        String databaseType = properties.getDatabase().getType().toLowerCase();
//        loader.loadFromYaml("classpath:nexus/queries/" + databaseType + ".yml");
        loader.loadFromYaml("nexus/queries/" + databaseType + ".yml");

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

        return loader;
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusAdvancedTemplate nexusAdvancedTemplate(
            NexusDatabaseAdapter adapter,
            NexusEntityMapper entityMapper,
            NexusQueryLoader queryLoader,
            NexusProperties properties) {
        return new NexusAdvancedTemplate(adapter, entityMapper, queryLoader, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NexusExceptionHandler nexusExceptionHandler(NexusProperties properties) {
        return new NexusExceptionHandler(properties);
    }
}
