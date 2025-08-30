package com.widdo.nexus.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

/**
 * NexusLogger
 * <p>
 * 结构化日志记录器
 *
 * @author XYL
 * @date 2025/08/27 15:36
 * @since 0.0.1-SNAPSHOT
 */
public class NexusLogger {

    private final Logger logger;
    private final String component;

    private NexusLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
        this.component = clazz.getSimpleName();
    }

    /**
     * NexusLogger getLogger.
     *
     * @param clazz clazz
     * @return com.widdo.nexus.core.log.NexusLogger
     * @author XYL
     * @date 2025/08/27 15:41:16
     */
    public static NexusLogger getLogger(Class<?> clazz) {
        return new NexusLogger(clazz);
    }

    /**
     * NexusLogger debug.
     *
     * @param queryId  queryId
     * @param query    query
     * @param params   params
     * @param duration duration
     * @return void
     * @author XYL
     * @date 2025/08/27 15:41:37
     */
    public void debug(String queryId, String query, Map<String, Object> params, long duration) {
        if (logger.isDebugEnabled()) {
            MDC.put("queryId", queryId);
            MDC.put("duration", String.valueOf(duration));

            logger.debug(LogMessages.format(LogMessages.QUERY_EXECUTING, query));
            logger.debug(LogMessages.format(LogMessages.QUERY_PARAMETERS, params));
            logger.debug(LogMessages.format(LogMessages.QUERY_DURATION, duration, queryId));

            MDC.remove("queryId");
            MDC.remove("duration");
        }
    }

    /**
     * NexusLogger error.
     *
     * @param queryId queryId
     * @param query   query
     * @param ex      ex
     * @author XYL
     * @date 2025/08/27 15:42:28
     */
    public void error(String queryId, String query, Throwable ex) {
        MDC.put("queryId", queryId);
        MDC.put("errorType", ex.getClass().getSimpleName());

        logger.error(LogMessages.format(LogMessages.QUERY_EXECUTION_ERROR, query), ex);

        MDC.remove("queryId");
        MDC.remove("errorType");
    }

    /**
     * NexusLogger warnSlowQuery.
     *
     * @param queryId   queryId
     * @param query     query
     * @param duration  duration
     * @param threshold threshold
     * @author XYL
     * @date 2025/08/27 15:47:51
     */
    public void warnSLowQuery(String queryId, String query, long duration, long threshold) {
        if (duration > threshold) {
            MDC.put("queryId", queryId);
            MDC.put("duration", String.valueOf(duration));

            logger.warn(LogMessages.format(LogMessages.SLOW_QUERY_WARN, duration, query));

            MDC.remove("queryId");
            MDC.remove("duration");
        }
    }

    public void info(String message, Object... args) {
        logger.info(LogMessages.format(message, args));
    }

    public void warn(String message, Object... args) {
        logger.warn(LogMessages.format(message, args));
    }

    public void error(String message, Object... args) {
        logger.error(LogMessages.format(message, args));
    }

    public void debug(String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(LogMessages.format(message, args));
        }
    }
}
