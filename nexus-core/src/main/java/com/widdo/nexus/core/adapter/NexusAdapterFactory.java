package com.widdo.nexus.core.adapter;

import com.widdo.nexus.core.entity.NexusEntityMapper;
import com.widdo.nexus.core.properties.NexusProperties;

/**
 * NexusAdapterFactory.
 * <p>
 * 数据库适配器工厂接口
 *
 * @author XYL
 * @date 2025/08/27 17:03
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusAdapterFactory {

    NexusDatabaseAdapter createAdapter(NexusProperties properties, NexusEntityMapper entityMapper);

    boolean supports(String databaseType);
}
