package com.widdo.nexus.core.meta;

import com.widdo.nexus.core.enums.NexusDatabaseFeature;

import java.util.Map;
import java.util.Set;

/**
 * NexusDatabaseMetadata.
 * <p>
 * 数据库元数据接口
 *
 * @author XYL
 * @date 2025/08/27 17:58
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusDatabaseMetadata {

    String getVersion();

    Set<NexusDatabaseFeature> getSupportedFeatures();

    Map<String, Object> getStats();
}
