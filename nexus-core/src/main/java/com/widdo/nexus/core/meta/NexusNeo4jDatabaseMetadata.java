package com.widdo.nexus.core.meta;

import com.widdo.nexus.core.enums.NexusDatabaseFeature;

import java.util.Map;
import java.util.Set;

/**
 * NexusNeo4jDatabaseMetadata.
 *
 * @author XYL
 * @date 2025/08/31 17:11
 * @since 0.0.1-SNAPSHOT
 */
public class NexusNeo4jDatabaseMetadata extends AbstractNexusDatabaseMetadata {

    @Override
    public String databaseType() {
        return databaseType;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public Set<NexusDatabaseFeature> getSupportedFeatures() {
        return Set.of();
    }

    @Override
    public Map<String, Object> getStats() {
        return this.stats;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }

    public void setDatabaseType(String databaseType){
        this.databaseType = databaseType;
    }
}
