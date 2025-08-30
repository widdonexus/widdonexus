package com.widdo.nexus.core.support.template;

import com.widdo.nexus.core.adapter.NexusDatabaseAdapter;
import com.widdo.nexus.core.entity.NexusEntityMapper;
import com.widdo.nexus.core.exception.NexusExceptionHelper;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.query.NexusExecutionStrategyFactory;
import com.widdo.nexus.core.query.NexusQueryExecutionStrategy;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.core.result.NexusGraphResultSet;
import com.widdo.nexus.core.support.query.NexusQuery;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * NexusAdvancedTemplate
 * <p>
 * Nexus高级模板类
 *
 * @author XYL
 * @date 2025/08/27 16:19
 * @since 0.0.1-SNAPSHOT
 */
public class NexusAdvancedTemplate {

    private final NexusDatabaseAdapter adapter;
    private final NexusEntityMapper entityMapper;
    private final NexusQueryLoader queryLoader;
    private final NexusExecutionStrategyFactory strategyFactory;
    private final NexusQueryExecutionTemplate executionTemplate;
    private final NexusLogger logger = NexusLogger.getLogger(getClass());

    public NexusAdvancedTemplate(NexusDatabaseAdapter adapter,
                                 NexusEntityMapper entityMapper,
                                 NexusQueryLoader queryLoader,
                                 NexusProperties properties) {
        this.adapter = adapter;
        this.entityMapper = entityMapper;
        this.queryLoader = queryLoader;
        this.strategyFactory = new NexusExecutionStrategyFactory(properties);
        this.executionTemplate = createExecutionTemplate(properties);
    }

    /**
     * 执行查询ID对应的查询
     */
    public <T> T execute(String queryId, Map<String, Object> parameters, Class<T> resultType) {
        String cypher = queryLoader.getQuery(queryId);
        NexusQueryContext context = new NexusQueryContext(queryId);

        return executionTemplate.execute(cypher, parameters, resultType, context);
    }

    /**
     * 执行NexusQuery对象
     */
    public <T> T execute(NexusQuery query, Class<T> resultType) {
        NexusQueryContext context = new NexusQueryContext("custom_query");

        return executionTemplate.execute(
                query.getCypher(),
                query.getParameters(),
                resultType,
                context
        );
    }

    /**
     * 执行自定义Cypher查询
     */
    public <T> T executeCypher(String cypher, Map<String, Object> parameters, Class<T> resultType) {
        NexusQueryContext context = new NexusQueryContext("raw_cypher");

        return executionTemplate.execute(cypher, parameters, resultType, context);
    }

    /**
     * 流式查询执行
     */
    public <T> Stream<T> stream(String queryId, Map<String, Object> parameters, Class<T> resultType) {
        String cypher = queryLoader.getQuery(queryId);
        NexusQueryContext context = new NexusQueryContext(queryId);

        try {
            NexusGraphResultSet resultSet = adapter.executeQuery(cypher, parameters);
            return resultSet.toList().stream()
                    .map(record -> entityMapper.mapToEntity(record, resultType));
        } catch (Exception ex) {
            context.setExecutionTime(System.currentTimeMillis() - context.getStartTime());
            logger.error("Stream query failed after {} ms: {}",
                    context.getExecutionTime(), queryId, ex);
            throw NexusExceptionHelper.executionError("Failed to execute stream query", ex);
        }
    }

    /**
     * 批量操作
     */
    public void executeBatch(List<NexusQuery> queries) {
        NexusQueryContext context = new NexusQueryContext("batch_operation");
        context.setStartTime(System.currentTimeMillis());

        try {
            for (int i = 0; i < queries.size(); i++) {
                NexusQuery query = queries.get(i);
                context.setCurrentOperation(i + 1);
                context.setTotalOperations(queries.size());

                adapter.executeCommand(query.getCypher(), query.getParameters());
            }

            context.setExecutionTime(System.currentTimeMillis() - context.getStartTime());
            logger.info("Batch operation completed: {} queries in {} ms",
                    queries.size(), context.getExecutionTime());
        } catch (Exception ex) {
            context.setExecutionTime(System.currentTimeMillis() - context.getStartTime());
            logger.error("Batch operation failed after {} ms at operation {}/{}",
                    context.getExecutionTime(),
                    context.getCurrentOperation(),
                    context.getTotalOperations(),
                    ex);
            throw NexusExceptionHelper.executionError("Batch operation failed", ex);
        }
    }

    private NexusQueryExecutionTemplate createExecutionTemplate(NexusProperties properties) {
        return new NexusQueryExecutionTemplate() {
            private final NexusQueryExecutionStrategy strategy =
                    strategyFactory.createStrategy(adapter);

            @Override
            protected NexusGraphResultSet doExecute(String query,
                                                    Map<String, Object> parameters,
                                                    NexusQueryContext context) {
                return strategy.execute(query, parameters);
            }

            @Override
            protected <T> T convertResults(NexusGraphResultSet resultSet, Class<T> resultType) {
                return (T) adapter.postExecute(resultSet, resultType);
            }

            // 获取List的泛型类型（简化实现）
            private Class<?> getListElementType() {
                // 实际实现需要使用TypeToken等机制
                return Map.class;
            }
        };
    }
}
