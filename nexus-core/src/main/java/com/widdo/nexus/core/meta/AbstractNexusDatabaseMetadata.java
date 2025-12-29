package com.widdo.nexus.core.meta;

import java.util.Map;

/**
 * @author XYL
 * @date 2025/08/31 17:16
 * @since
 */
public abstract class AbstractNexusDatabaseMetadata implements NexusDatabaseMetadata{

    public String databaseType;

    public String version;

    public Map<String,Object> stats;

}
