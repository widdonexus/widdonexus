package com.widdo.nexus.core.support.template;

import com.widdo.nexus.core.log.LogMessages;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.result.NexusGraphResultSet;

import java.util.Map;

/**
 * NexusQueryExecutionTemplate
 * <p>
 * 查询执行模板
 * <p>
 * 模板方法模式 - 查询执行流程
 *
 * @author XYL
 * @date 2025/08/27 15:32
 * @since 0.0.1-SNAPSHOT
 */
public abstract class NexusQueryExecutionTemplate {

    protected final NexusLogger logger = NexusLogger.getLogger(getClass());

    public final <T> T execute(String queryId, Map<String, Object> parameters,
                               Class<T> resultType, NexusQueryContext context) {
        // 1. 预处理
        preExecute(queryId, parameters, context);

        try {
            // 2. 执行查询
            NexusGraphResultSet resultSet = doExecute(queryId, parameters, context);

            // 3. 处理结果
            T result = postExecute(queryId, resultSet, resultType, context);

            // 4. 后处理
            afterExecute(queryId, parameters, result, context);

            return result;
        } catch (Exception ex) {
            // 5. 异常处理
            handleException(queryId, parameters, ex, context);
            throw ex;
        } finally {
            //TODO:记录上下文

        }
    }

    protected void preExecute(String queryId, Map<String, Object> parameters,
                              NexusQueryContext context) {
        context.setStartTime(System.currentTimeMillis());

        //处理MessageFormat格式化日志时把cypher语句中的{}识别为占位符
        queryId = queryId.replace("{", "'{'").replace("}", "'}'");

        logger.debug(LogMessages.format(LogMessages.QUERY_EXECUTING, queryId));
    }

    protected abstract NexusGraphResultSet doExecute(String queryId,
                                                     Map<String, Object> parameters,
                                                     NexusQueryContext context);

    protected <T> T postExecute(String queryId, NexusGraphResultSet resultSet, Class<T> resultType,
                                NexusQueryContext context) {
        context.setExecutionTime(System.currentTimeMillis() - context.getStartTime());

        //处理MessageFormat格式化日志时把cypher语句中的{}识别为占位符
        queryId = queryId.replace("{", "'{'").replace("}", "'}'");

        logger.debug(LogMessages.format(LogMessages.QUERY_DURATION, context.getExecutionTime(), queryId));

        return convertResults(resultSet, resultType);
    }

    protected void afterExecute(String queryId, Map<String, Object> parameters,
                                Object result, NexusQueryContext context) {
        // 可扩展点：缓存结果、记录指标等.1秒阈值
        if (context.getExecutionTime() > 1000) {
            //处理MessageFormat格式化日志时把cypher语句中的{}识别为占位符
            queryId = queryId.replace("{", "'{'").replace("}", "'}'");
            logger.warn(LogMessages.format(LogMessages.SLOW_QUERY_WARN, context.getExecutionTime(), queryId));
        }
    }

    protected void handleException(String queryId, Map<String, Object> parameters,
                                   Exception ex, NexusQueryContext context) {
        context.setExecutionTime(System.currentTimeMillis() - context.getStartTime());
        //处理MessageFormat格式化日志时把cypher语句中的{}识别为占位符
        queryId = queryId.replace("{", "'{'").replace("}", "'}'");
        logger.error(LogMessages.format(LogMessages.QUERY_EXECUTION_ERROR_DURATION, context.getExecutionTime()), queryId, ex);
    }

    protected abstract <T> T convertResults(NexusGraphResultSet resultSet, Class<T> resultType);
}
