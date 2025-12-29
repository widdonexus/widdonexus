package com.widdo.nexus.cli.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.log.NexusLogger;
import com.widdo.nexus.core.properties.NexusProperties;
import com.widdo.nexus.core.result.NexusGraphResultSet;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.result.NexusResultInterface;
import com.widdo.nexus.core.result.entity.Value;
import com.widdo.nexus.neo4j.util.Neo4jUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * MigrationCommand
 * <p>
 * Nexus Shell 数据库迁移工具命令
 *
 * @author XYL
 * @date 2025/09/01 10:21
 * @since 0.0.1-SNAPSHOT
 */
@ShellComponent
@ShellCommandGroup("Migration Commands")
public class MigrationCommand {

    private final NexusGraphAdapter sourceAdapter;
    private final NexusGraphAdapter targetAdapter;

    private final NexusLogger logger;

    public MigrationCommand(
            @Qualifier("sourceAdapter") NexusGraphAdapter sourceAdapter,
            @Qualifier("targetAdapter") NexusGraphAdapter targetAdapter, NexusProperties properties) {

        //从配置文件中获取source和target
        final String sourceType = properties.getMigration().getSource();
        final String targetType = properties.getMigration().getTarget();

        this.sourceAdapter = sourceAdapter;
        this.targetAdapter = targetAdapter;
        this.logger = NexusLogger.getLogger(getClass());
    }
    
    @ShellMethod(key = "migrate-schema", value = "Migrate schema between databases")
    public String migrateSchema(
        @ShellOption(help = "Source database type") String sourceType,
        @ShellOption(help = "Target database type") String targetType) {
        
        try {
            // 获取源数据库的schema信息
            final NexusGraphResultSet resultSet = sourceAdapter.executeQuery(
                    "CALL db.indexes()", Collections.emptyMap());

            final com.widdo.nexus.core.result.Result<List<Map<String, Value>>> result = Neo4jUtil.originResult(resultSet);
            final NexusResult wrapper = NexusResultInterface.NEO4j.ALL.wrapper(result);
            final List<Map<String, Object>> orDefault = (List<Map<String, Object>>) wrapper.getOrDefault("data", List.class);

            // 在目标数据库创建相应的schema
            for (Map<String, Object> index : orDefault) {
                String createIndexQuery = generateCreateIndexQuery(index, targetType);
                targetAdapter.executeCommand(createIndexQuery, Collections.emptyMap());
            }
            
            return "Schema migration completed successfully";
        } catch (Exception ex) {
            return "Schema migration failed: " + ex.getMessage();
        }
    }
    
    @ShellMethod(key = "migrate-data", value = "Migrate data between databases")
    public String migrateData(
        @ShellOption(help = "Batch size", defaultValue = "1000") int batchSize,
        @ShellOption(help = "Labels to migrate (comma-separated)") String labels) {
        
        try {
            List<String> labelList = Arrays.asList(labels.split(","));
            int totalMigrated = 0;
            
            for (String label : labelList) {
                int migrated = migrateLabelData(label, batchSize);
                totalMigrated += migrated;
                logger.info("Migrated {} nodes with label: {}", migrated, label);
            }
            
            return String.format("Data migration completed. Total nodes migrated: %d", totalMigrated);
        } catch (Exception ex) {
            return "Data migration failed: " + ex.getMessage();
        }
    }
    
    private int migrateLabelData(String label, int batchSize) {
        int migrated = 0;
        int offset = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            // 从源数据库批量读取数据
            String query = String.format(
                "MATCH (n:%s) RETURN n SKIP %d LIMIT %d", label, offset, batchSize);

            final NexusGraphResultSet resultSet = sourceAdapter.executeQuery(
                    query, Collections.emptyMap());

            final com.widdo.nexus.core.result.Result<List<Map<String, Value>>> result = Neo4jUtil.originResult(resultSet);
            final NexusResult wrapper = NexusResultInterface.NEO4j.ALL.wrapper(result);
            final List<Map<String, Object>> orDefault = (List<Map<String, Object>>) wrapper.getOrDefault("data", List.class);


            // 批量写入目标数据库
            for (Map<String, Object> node : orDefault) {
                String createQuery = generateCreateQuery(node, label, targetAdapter.getDatabaseType());
                targetAdapter.executeCommand(createQuery, Collections.emptyMap());
                migrated++;
            }
            
            offset += batchSize;
        }
        
        return migrated;
    }

    private String generateCreateIndexQuery(Map<String, Object> indexInfo, String targetType) {
        String indexName = (String) indexInfo.get("name");
        List<String> labels = (List<String>) indexInfo.get("labels");
        List<String> properties = (List<String>) indexInfo.get("properties");
        String type = (String) indexInfo.get("type");
        String entityType = (String) indexInfo.get("entityType");

        // 确定是节点索引还是关系索引
        boolean isNodeIndex = "NODE".equals(entityType);

        // 根据目标数据库类型生成不同的索引创建语句
        if ("neo4j".equalsIgnoreCase(targetType)) {
            // Neo4j 索引创建语法
            if ("FULLTEXT".equalsIgnoreCase(type)) {
                // 全文索引
                // 全文索引通常只针对一个标签/类型
                String labelOrType = labels.get(0);
                return String.format("CREATE FULLTEXT INDEX %s FOR (n:%s) ON EACH [n.%s]",
                        indexName, labelOrType, String.join(", n.", properties));
            } else {
                // 标准索引
                // 假设每个索引只涉及一个标签/类型
                String labelOrType = labels.get(0);
                if (isNodeIndex) {
                    return String.format("CREATE INDEX %s FOR (n:%s) ON (n.%s)",
                            indexName, labelOrType, String.join(", n.", properties));
                } else {
                    return String.format("CREATE INDEX %s FOR ()-[r:%s]-() ON (r.%s)",
                            indexName, labelOrType, String.join(", r.", properties));
                }
            }
        } else if ("opengauss".equalsIgnoreCase(targetType)) {
            // OpenGauss 索引创建语法
            // 在OpenGauss中，我们需要将图模式转换为关系模式
            String tableName = isNodeIndex ? "vertex" : "edge";
            String labelColumn = isNodeIndex ? "label" : "edge_label";

            // 对于OpenGauss，我们需要为特定标签创建部分索引
            StringBuilder sb = new StringBuilder();
            sb.append("CREATE INDEX ").append(indexName).append(" ON ").append(tableName).append(" (");

            // 添加属性列
            for (int i = 0; i < properties.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("properties->>'").append(properties.get(i)).append("'");
            }

            sb.append(") WHERE ");

            // 添加标签条件
            if (labels.size() == 1) {
                sb.append(labelColumn).append(" = '").append(labels.get(0)).append("'");
            } else {
                sb.append(labelColumn).append(" IN (");
                for (int i = 0; i < labels.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append("'").append(labels.get(i)).append("'");
                }
                sb.append(")");
            }

            return sb.toString();
        } else {
            throw new UnsupportedOperationException("Unsupported target database type: " + targetType);
        }
    }


    private String generateCreateQuery(Map<String, Object> node, String label, String databaseType) {
        try {
            if ("neo4j".equalsIgnoreCase(databaseType)) {
                return generateNeo4jCreateQuery(node, label);
            } else if ("opengauss".equalsIgnoreCase(databaseType)) {
                return generateOpenGaussCreateQuery(node, label);
            } else {
                throw new UnsupportedOperationException("Unsupported database type: " + databaseType);
            }
        } catch (Exception e) {
            logger.error("Failed to generate create query for node: " + node, e);
            throw e;
        }
    }

    private String generateNeo4jCreateQuery(Map<String, Object> node, String label) {
        StringBuilder query = new StringBuilder("CREATE (n:");
        query.append(label);

        // 添加属性
        if (node.containsKey("properties") && node.get("properties") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) node.get("properties");
            if (!properties.isEmpty()) {
                query.append(" {");
                boolean first = true;
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (!first) {
                        query.append(", ");
                    }
                    query.append(entry.getKey()).append(": ");
                    query.append(formatValueForCypher(entry.getValue()));
                    first = false;
                }
                query.append("}");
            }
        }

        query.append(")");
        return query.toString();
    }

    private String generateOpenGaussCreateQuery(Map<String, Object> node, String label) {
        StringBuilder query = new StringBuilder("INSERT INTO vertex (id, label, properties) VALUES (");

        // 添加ID
        if (node.containsKey("id")) {
            query.append(formatValueForSQL(node.get("id")));
        } else {
            // 使用默认序列值
            query.append("DEFAULT");
        }
        query.append(", ");

        // 添加标签
        query.append(formatValueForSQL(label)).append(", ");

        // 添加属性
        if (node.containsKey("properties") && node.get("properties") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) node.get("properties");
            query.append(formatValueForSQL(convertMapToJson(properties)));
        } else {
            query.append("'{}'::jsonb");
        }

        query.append(")");
        return query.toString();
    }

    private String formatValueForCypher(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            // 转义字符串中的特殊字符
            String escaped = ((String) value)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("'", "\\'");
            return "\"" + escaped + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            // 处理嵌套属性
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append(": ").append(formatValueForCypher(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } else if (value instanceof List) {
            // 处理列表
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(formatValueForCypher(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        } else {
            // 默认处理为字符串
            return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
    }

    private String formatValueForSQL(Object value) {
        if (value == null) {
            return "NULL";
        } else if (value instanceof String) {
            // 转义字符串中的特殊字符
            String escaped = ((String) value).replace("'", "''");
            return "'" + escaped + "'";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? "TRUE" : "FALSE";
        } else {
            // 默认处理为字符串
            return "'" + value.toString().replace("'", "''") + "'";
        }
    }

    private String convertMapToJson(Map<String, Object> map) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert map to JSON: " + map, e);
            return "{}";
        }
    }
}