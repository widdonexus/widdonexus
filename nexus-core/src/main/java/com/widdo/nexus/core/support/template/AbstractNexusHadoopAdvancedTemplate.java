package com.widdo.nexus.core.support.template;

import com.widdo.nexus.core.adapter.NexusHadoopAdapter;
import com.widdo.nexus.core.exception.NexusExceptionHelper;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.query.NexusExecutionStrategyFactory;
import com.widdo.nexus.core.query.NexusQueryExecutionStrategy;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.support.query.NexusQuery;

import java.util.List;
import java.util.Map;

/**
 * AbstractNexusHadoopAdvancedTemplate
 *
 * @author XYL
 * @date 2025/12/14 19:16
 * @since 0.0.1-SNAPSHOT
 */
public class AbstractNexusHadoopAdvancedTemplate {

    private final NexusHadoopAdapter adapter;
    private final NexusQueryLoader queryLoader;
    private final NexusExecutionStrategyFactory strategyFactory;
    private final NexusHadoopExecutionTemplate executionTemplate;
    private final NexusLogger logger = NexusLogger.getLogger(getClass());

    public AbstractNexusHadoopAdvancedTemplate(NexusHadoopAdapter adapter,
                                               NexusQueryLoader queryLoader,
                                               NexusProperties properties) {
        this.adapter = adapter;
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

        parameters.put("queryId", queryId);

        return executionTemplate.execute(cypher, parameters, resultType, context);
    }

    /**
     * 执行NexusQuery对象
     */
    public <T> T execute(NexusQuery query, Class<T> resultType) {
        NexusQueryContext context = new NexusQueryContext("custom_query");

        return executionTemplate.execute(
                query.cypher(),
                query.parameters(),
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

                adapter.execute(query.cypher(), query.parameters(), context);
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

    private NexusHadoopExecutionTemplate createExecutionTemplate(NexusProperties properties) {
        return new NexusHadoopExecutionTemplate() {
            private final NexusQueryExecutionStrategy<NexusHadoopAdapter, NexusResult> strategy =
                    strategyFactory.createStrategy(adapter);

            @Override
            protected NexusResult doExecute(String query,
                                            Map<String, Object> parameters,
                                            NexusQueryContext context) {
                return strategy.execute(query, parameters, context);
            }

            @Override
            protected <T> T convertResults(NexusResult resultSet, Class<T> resultType) {
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
