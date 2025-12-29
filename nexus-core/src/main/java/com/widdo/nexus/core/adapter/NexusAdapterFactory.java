package com.widdo.nexus.core.adapter;

/**
 * NexusAdapterFactory.
 * <p>
 * 数据库适配器工厂接口。
 * <p>
 * 扩展，区分图数据库，大数据
 * widdo.nexus.graph.database: - type ,name
 * widdo.nexus.hadoop.database:
 *
 * @author XYL
 * @date 2025/08/27 17:03
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusAdapterFactory {

    boolean supports(String databaseType);
}
