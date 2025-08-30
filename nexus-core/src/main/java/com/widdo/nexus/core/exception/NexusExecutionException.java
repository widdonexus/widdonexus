package com.widdo.nexus.core.exception;

import java.util.Map;

/**
 * NexusExecutionException.
 *
 * @author XYL
 * @date 2025/08/27 22:06
 * @since 0.0.1-SNAPSHOT
 */
public class NexusExecutionException extends NexusException {

    public NexusExecutionException(String message) {
        super("EXECUTE_ERROR", message);
    }

    public NexusExecutionException(String message, Throwable cause) {
        super("EXECUTE_ERROR", message, cause);
    }

    public NexusExecutionException withQuery(String query) {
        return (NexusExecutionException) withContext("query", query);
    }

    public NexusExecutionException withQueryId(long queryId) {
        return (NexusExecutionException) withContext("queryId", queryId);
    }

    public NexusExecutionException withParameters(Map<String, Object> parameters) {
        return (NexusExecutionException) withContext("parameters", parameters);
    }
}
