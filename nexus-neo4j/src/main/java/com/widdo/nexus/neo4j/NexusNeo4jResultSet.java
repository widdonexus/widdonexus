package com.widdo.nexus.neo4j;

import com.widdo.nexus.core.result.NexusGraphResultSet;
import com.widdo.nexus.core.result.Result;

import java.util.List;

/**
 * NexusNeo4jResultSet.
 *
 * @author XYL
 * @date 2025/08/27 17:23
 * @since 0.0.1-SNAPSHOT
 */
public class NexusNeo4jResultSet extends NexusGraphResultSet {

    private List<String> keys;
    private List<org.neo4j.driver.Record> records;

    public NexusNeo4jResultSet(List<String> keys, List<org.neo4j.driver.Record> records) {
        this.keys = keys;
        this.records = records;
    }

    @Override
    public Result<?> result() {
        return null;
    }

    public List<String> keys(){
        return keys;
    }

    public List<org.neo4j.driver.Record> records(){
        return records;
    }
}
