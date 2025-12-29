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
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "widdo.nexus")
public class NexusProperties {

    private boolean enabled = true;

    private Graph graph = new Graph();

    private Hadoop hadoop = new Hadoop();

    private Logging logging = new Logging();

    private Exception exception = new Exception();

    private Scan scan = new Scan();

    private Migration migration = new Migration();

    /**
     * 载自定义查询位置
     */
    private List<String> queryLocations;

    public Hadoop getHadoop() {
        return hadoop;
    }

    public void setHadoop(Hadoop hadoop) {
        this.hadoop = hadoop;
    }

    // Getter和Setter
    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
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

    public Migration getMigration() {
        return migration;
    }

    public void setMigration(Migration migration) {
        this.migration = migration;
    }

    // 嵌套配置类
    @Data
    public static class Graph {

        private Database database = new Database();

        private Neo4j neo4j = new Neo4j();

        private JanusGraph janusGraph = new JanusGraph();

        private Execution execution = new Execution();

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
    }

    @Data
    public static class Hadoop {

        private Hdfs hdfs = new Hdfs();

        private Database database = new Database();

        @Data
        public static class Database {
            private String type = "NEO4J";
            private String name;
        }

        @Data
        public static class Hdfs {

            private String username = "neo4j";

            private Nn nn = new Nn();

            @Data
            public static class Nn {
                private String webAddr;
                private String insideAddr;
            }

        }
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

    @Data
    public static class Migration {
        private String source;
        private String target;
    }
}
