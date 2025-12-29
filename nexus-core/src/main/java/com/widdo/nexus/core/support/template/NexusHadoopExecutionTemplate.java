package com.widdo.nexus.core.support.template;

import com.widdo.nexus.core.log.LogMessages;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.util.HdfsCommandExtractor;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * NexusHadoopExecutionTemplate
 *
 * @author XYL
 * @date 2025/12/14 20:25
 * @since 0.0.1-SNAPSHOT
 */
public abstract class NexusHadoopExecutionTemplate {

    protected final NexusLogger logger = NexusLogger.getLogger(getClass());

    public final <T> T execute(String queryId, Map<String, Object> parameters,
                               Class<T> resultType, NexusQueryContext context) {

        HdfsCommandExtractor.CommandInfo commandInfo = HdfsCommandExtractor.parseCommandWithParameters(queryId, parameters);
        context.setCommandInfo(commandInfo);

        // 1. 预处理
        preExecute(queryId, parameters, context);

        try {
            queryId = context.getProcessedQueryId();

            // 2. 执行查询
            NexusResult resultSet = doExecute(queryId, parameters, context);

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
            //记录上下文
            logCommandExecution(context, commandInfo);
        }
    }

    private void logCommandExecution(NexusQueryContext context,
                                     HdfsCommandExtractor.CommandInfo commandInfo) {
        long duration = System.currentTimeMillis() - context.getStartTime();

        logger.debug(LogMessages.format(LogMessages.HDFS_COMMAND_EXECUTING, commandInfo.getCommand(),
                commandInfo.getCategory(),
                duration,
                commandInfo.isReadOperation(),
                commandInfo.isWriteOperation()));
    }

    /**
     * 替换queryId中的参数占位符
     */
    private String replaceParameters(String queryId, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return queryId;
        }

        final Map<String, Object> params = (Map<String, Object>) parameters.getOrDefault("params", Map.of());

        if (params == null || params.isEmpty()) {
            return queryId;
        }

        // 使用正则表达式替换所有 $参数名
        String result = queryId;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            if (paramValue != null) {
                // 替换 $paramName 形式的占位符
                result = result.replaceAll("\\$" + Pattern.quote(paramName), paramValue.toString());
            }
        }

        return result;
    }

    protected void preExecute(String queryId, Map<String, Object> parameters,
                              NexusQueryContext context) {
        context.setStartTime(System.currentTimeMillis());

        final String processedQueryId = replaceParameters(queryId, parameters);
        context.setProcessedQueryId(processedQueryId);

        logger.debug(LogMessages.format(LogMessages.QUERY_EXECUTING, queryId));
    }


    protected abstract NexusResult doExecute(String queryId,
                                             Map<String, Object> parameters,
                                             NexusQueryContext context);

    protected <T> T postExecute(String queryId, NexusResult resultSet, Class<T> resultType,
                                NexusQueryContext context) {
        context.setExecutionTime(System.currentTimeMillis() - context.getStartTime());

        logger.debug(LogMessages.format(LogMessages.QUERY_DURATION, context.getExecutionTime(), queryId));

        return convertResults(resultSet, resultType);
    }

    protected void afterExecute(String queryId, Map<String, Object> parameters,
                                Object result, NexusQueryContext context) {
        // 可扩展点：缓存结果、记录指标等.1秒阈值
        if (context.getExecutionTime() > 1000) {
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

    protected abstract <T> T convertResults(NexusResult resultSet, Class<T> resultType);
}
