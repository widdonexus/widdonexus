package com.widdo.nexus.core.exception;

import java.util.Map;
import java.util.function.Supplier;

/**
 * NexusExceptionHelper.
 * <p>
 * 异常创建辅助工具
 *
 * @author XYL
 * @date 2025/08/27 16:41
 * @since 0.0.1-SNAPSHOT
 */
public class NexusExceptionHelper {

    private NexusExceptionHelper() {
    }

    // 快速创建异常的方法
    public static NexusQueryException queryError(String message) {
        return new NexusQueryException(message);
    }

    public static NexusQueryException queryError(String message, Throwable cause) {
        return new NexusQueryException(message, cause);
    }

    public static NexusExecutionException queryError(String message, String query, Map<String, Object> params) {
        return new NexusExecutionException(message)
                .withQuery(query)
                .withParameters(params);
    }

    public static NexusExecutionException executionError(String message) {
        return new NexusExecutionException(message);
    }

    public static NexusExecutionException executionError(String message, Throwable cause) {
        return new NexusExecutionException(message, cause);
    }

    public static NexusExecutionException executionError(String message, String query, Map<String, Object> params) {
        return new NexusExecutionException(message)
                .withQuery(query)
                .withParameters(params);
    }

    public static NexusMappingException mappingError(String message, Class<?> entityType) {
        return new NexusMappingException(message)
                .withEntityType(entityType);
    }

    public static NexusConfigurationException configError(String message) {
        return new NexusConfigurationException(message);
    }

    public static NexusConfigurationException configError(String message, Throwable cause) {
        return new NexusConfigurationException(message, cause);
    }

    public static NexusConnectionException connectionError(String message, Throwable cause) {
        return new NexusConnectionException(message, cause);
    }

    /**
     * NexusExceptionHelper wrap.
     * <p>
     * 异常转换方法：将底层异常转换为框架异常
     *
     * @param ex
     * @return com.widdo.nexus.core.exception.NexusException
     * @author XYL
     * @date 2025/08/27 16:43:44
     */
    public static NexusException wrap(Throwable ex) {
        if (ex instanceof NexusException) {
            return (NexusException) ex;
        }

        // 根据异常类型进行转换
        if (ex instanceof org.neo4j.driver.exceptions.ServiceUnavailableException) {
            return connectionError("Database service unavailable", ex);
        }

        if (ex instanceof java.sql.SQLTimeoutException) {
            return queryError("Query timeout", ex);
        }

        // 默认转换
        return new NexusException("UNKNOWN_ERROR", "Unexpected error occurred", ex);
    }

    /**
     * NexusExceptionHelper throwIf.
     * <p>
     * 条件性异常抛出
     *
     * @param condition
     * @param exceptionSupplier
     * @return void
     * @author XYL
     * @date 2025/08/27 16:43:31
     */
    public static void throwIf(boolean condition, Supplier<NexusException> exceptionSupplier) {
        if (condition) {
            throw exceptionSupplier.get();
        }
    }
}
