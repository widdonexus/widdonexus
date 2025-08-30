package com.widdo.nexus.core.result;

import com.widdo.nexus.core.result.entity.Value;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * GraphResultSet
 *
 * @author XYL
 * @date 2025/08/27 15:56
 * @since 0.0.1-SNAPSHOT
 */
public interface GraphResultSet extends Iterator<Map<String, Object>>, AutoCloseable {

    List<Map<String, Value>> toList();

    com.widdo.nexus.core.result.Result<?> result();

    <T> List<T> toList(Class<T> type);
}
