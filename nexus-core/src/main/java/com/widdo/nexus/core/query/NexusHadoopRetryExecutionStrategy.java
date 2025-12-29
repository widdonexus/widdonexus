package com.widdo.nexus.core.query;

import com.widdo.nexus.core.adapter.NexusHadoopAdapter;
import com.widdo.nexus.core.enums.QueryExecutionType;
import com.widdo.nexus.core.exception.NexusConnectionException;
import com.widdo.nexus.core.exception.NexusException;
import com.widdo.nexus.core.exception.NexusExceptionHelper;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.support.template.NexusQueryContext;

import java.util.Map;

/**
 * NexusHadoopRetryExecutionStrategy
 *
 * @author XYL
 * @date 2025/12/14 21:26
 * @since 0.0.1-SNAPSHOT
 */
public class NexusHadoopRetryExecutionStrategy implements NexusQueryExecutionStrategy<NexusHadoopAdapter, NexusResult> {

    private final NexusHadoopAdapter adapter;
    private final int maxRetries;
    private final long retryDelayMs;
    private final NexusLogger logger = NexusLogger.getLogger(getClass());

    public NexusHadoopRetryExecutionStrategy(NexusHadoopAdapter adapter, int maxRetries, long retryDelayMs) {
        this.adapter = adapter;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public NexusResult execute(String query, Map<String, Object> parameters, NexusQueryContext context) {
        int attempt = 0;
        NexusException lastException = null;

        while (attempt <= maxRetries) {
            try {
                return adapter.execute(query, parameters, context);
            } catch (NexusConnectionException ex) {
                attempt++;
                lastException = ex;
                logger.warn("Connection failed, attempt {}/{}", attempt, maxRetries);

                if (attempt <= maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw NexusExceptionHelper.queryError("Query execution interrupted", ie)
                                .withQuery(query)
                                .withParameters(parameters);
                    }
                }
            }
        }
        throw NexusExceptionHelper.queryError("Failed to execute query after " + maxRetries + " attempts", lastException)
                .withQuery(query)
                .withParameters(parameters);
    }

    @Override
    public QueryExecutionType getExecutionType() {
        return QueryExecutionType.RETRY;
    }
}
