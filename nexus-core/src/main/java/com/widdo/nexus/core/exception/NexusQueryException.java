package com.widdo.nexus.core.exception;

import java.util.Map;

/**
 * NexusQueryException.
 * <p>
 * 查询相关异常
 *
 * @author XYL
 * @date 2025/08/27 16:31
 * @since 0.0.1-SNAPSHOT
 */
public class NexusQueryException extends NexusException {

    public NexusQueryException(String message) {
        super("QUERY_ERROR", message);
    }

    public NexusQueryException(String message, Throwable cause) {
        super("QUERY_ERROR", message, cause);
    }

    public NexusQueryException withQuery(String query) {
        return (NexusQueryException) withContext("query", query);
    }

    public NexusQueryException withQueryId(long queryId) {
        return (NexusQueryException) withContext("queryId", queryId);
    }

    public NexusQueryException withParameters(Map<String, Object> parameters) {
        return (NexusQueryException) withContext("parameters", parameters);
    }
}
