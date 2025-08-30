package com.widdo.nexus.core.properties;

/**
 * NexusProperties.
 * <p>
 * Nexus统一配置属性
 * <p>
 * 使用 @ConfigurationProperties 和 @NestedConfigurationProperty 简化配置
 *
 * @author XYL
 * @date 2025/08/27 17:06
 * @since 0.0.1-SNAPSHOT
 */

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "widdo.nexus")
public class NexusProperties {

    private boolean enabled = true;

    private Database database = new Database();

    @NestedConfigurationProperty
    private Neo4j neo4j = new Neo4j();
    private JanusGraph janusgraph = new JanusGraph();
    private Execution execution = new Execution();
    private Logging logging = new Logging();

    private Exception exception = new Exception();

    private Scan scan = new Scan();

    /**
     * 载自定义查询位置
     */
    private List<String> queryLocations;

    // Getter和Setter
    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Neo4j getNeo4j() {
        return neo4j;
    }

    public void setNeo4j(Neo4j neo4j) {
        this.neo4j = neo4j;
    }

    public JanusGraph getJanusgraph() {
        return janusgraph;
    }

    public void setJanusgraph(JanusGraph janusgraph) {
        this.janusgraph = janusgraph;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public List<String> getQueryLocations() {
        return queryLocations;
    }

    public void setQueryLocations(List<String> queryLocations) {
        this.queryLocations = queryLocations;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public Scan getScan() {
        return scan;
    }

    public void setScan(Scan scan) {
        this.scan = scan;
    }

    // 嵌套配置类
    @Data
    public static class Database {
        private String type = "NEO4J";
        private String name;
    }

    @Data
    public static class Neo4j {
        private String uri = "bolt://localhost:7687";
        private String username = "neo4j";
        private String password;
        private String database = "neo4j";
        private Pool pool = new Pool();

        @Data
        public static class Pool {
            private int maxConnectionPoolSize = 100;
            private int connectionAcquisitionTimeout = 60;
            private int maxConnectionLifetime = 3600;
        }
    }

    @Data
    public static class JanusGraph {
        private String configFile;
        private Map<String, Object> properties = new HashMap<>();
    }

    @Data
    public static class Execution {
        private boolean retryEnabled = true;
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        private long slowQueryThresholdMs = 1000;
    }

    @Data
    public static class Logging {
        private boolean enableQueryLogging = true;
        private boolean enableTransactionLogging = true;
        private boolean enablePerformanceLogging = true;
        private Level level = Level.INFO;

        public enum Level {
            TRACE, DEBUG, INFO, WARN, ERROR
        }
    }

    @Data
    public static class Exception {
        private boolean includeStacktrace = true;
    }

    @Data
    public static class Scan {
        private List<String> basePackages = new ArrayList<>();
    }
}
