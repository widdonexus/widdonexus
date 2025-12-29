package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.enums.NexusDatabaseFeature;
import com.widdo.nexus.core.meta.NexusDatabaseMetadata;
import com.widdo.nexus.core.result.NexusGraphResultSet;
import com.widdo.nexus.core.result.Result;
import com.widdo.nexus.core.transaction.NexusTransaction;
import org.springframework.boot.actuate.health.Health;

import java.util.Map;

/**
 * NexusGraphAdapter
 *
 * @author XYL
 * @date 2025/12/14 18:44
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusGraphAdapter extends NexusDatabaseAdapter {

    NexusGraphResultSet executeQuery(String query, Map<String, Object> parameters);

    void executeCommand(String command, Map<String, Object> parameters);

    NexusTransaction beginTransaction();

    void close();

    boolean supportsFeature(NexusDatabaseFeature feature);

    NexusDatabaseMetadata getMetadata();

    Health healthCheck();

    Result<?> toExecute(String model, String query, Map<String, Object> params);

    <T> T postExecute(NexusGraphResultSet resultSet, Class<T> resultType);

    //实际实现需要使用TypeToken等机制
    Class<?> getListElementType();
}
