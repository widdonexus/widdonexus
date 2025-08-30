package com.widdo.nexus.core.result;

import com.widdo.nexus.core.result.entity.Value;

import java.util.List;
import java.util.Map;

/**
 * NexusGraphResultSet
 *
 * @author XYL
 * @date 2025/08/27 15:51
 * @since 0.0.1-SNAPSHOT
 */
public abstract class NexusGraphResultSet implements GraphResultSet {

    @Override
    public List<Map<String, Value>> toList() {
        return List.of();
    }

    @Override
    public <T> List<T> toList(Class<T> type) {
        return List.of();
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Map<String, Object> next() {
        return Map.of();
    }
}
