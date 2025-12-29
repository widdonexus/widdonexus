package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.exception.NexusException;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.support.template.NexusQueryContext;

import java.util.Map;

/**
 * NexusHadoopAdapter
 * <p>
 * Hadoop底层存储公共的接口
 *
 * @author XYL
 * @date 2025/12/09 17:39
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusHadoopAdapter extends NexusDatabaseAdapter {
    NexusResult execute(String query, Map<String, Object> parameters, NexusQueryContext context) throws NexusException;

    <T> T postExecute(NexusResult resultSet, Class<T> resultType);
}
