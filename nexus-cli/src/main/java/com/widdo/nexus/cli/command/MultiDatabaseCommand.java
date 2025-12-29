package com.widdo.nexus.cli.command;

import com.widdo.nexus.cli.processor.DatabaseAwareQueryProcessor;
import com.widdo.nexus.cli.properties.NexusCliProperties;
import com.widdo.nexus.cli.shell.TableRenderer;
import com.widdo.nexus.cli.util.NexusCliUtil;
import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.enums.NexusDatabaseFeature;
import com.widdo.nexus.core.meta.NexusDatabaseMetadata;
import com.widdo.nexus.core.result.NexusGraphResultSet;
import com.widdo.nexus.core.result.NexusResult;
import com.widdo.nexus.core.result.NexusResultInterface;
import com.widdo.nexus.core.result.entity.Value;
import com.widdo.nexus.neo4j.util.Neo4jUtil;
import org.springframework.boot.actuate.health.Health;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
/**
 * MultiDatabaseCommand
 * <p>
 * Nexus Shell 多数据库命令实现
 *
 * @author XYL
 * @date 2025/09/01 10:21
 * @since 0.0.1-SNAPSHOT
 */
@ShellComponent
@ShellCommandGroup("Database Commands")
public class MultiDatabaseCommand {

    private final NexusGraphAdapter adapter;
    private final DatabaseAwareQueryProcessor queryProcessor;

    private final TableRenderer tableRenderer;

    public MultiDatabaseCommand(NexusGraphAdapter adapter, TableRenderer tableRenderer) {
        this.adapter = adapter;
        this.tableRenderer = tableRenderer;
        this.queryProcessor = new DatabaseAwareQueryProcessor(adapter);
    }

    @ShellMethod(key = "db-info", value = "Show database information")
    public String databaseInfo() {
        NexusDatabaseMetadata metadata = adapter.getMetadata();
        Health health = adapter.healthCheck();

        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("Type", metadata.databaseType()));
        rows.add(Arrays.asList("Version", metadata.getVersion()));
        rows.add(Arrays.asList("Status", health.getStatus().toString()));
        rows.add(Arrays.asList("Nodes", String.valueOf(metadata.getStats().get("nodeCount"))));
        rows.add(Arrays.asList("Relationships", String.valueOf(metadata.getStats().get("relationshipCount"))));

        // 添加数据库特定信息
        metadata.getStats().forEach((key, value) ->
                rows.add(Arrays.asList(key, String.valueOf(value))));

        return tableRenderer.renderTable(
                Arrays.asList("Property", "Value"),
                rows
        );
    }

    @ShellMethod(key = "db-query", value = "Execute a database-specific query")
    public String executeDatabaseQuery(
            @ShellOption(help = "Query to execute") String query,
            @ShellOption(help = "Query parameters in JSON", defaultValue = "{}") String paramsJson,
            @ShellOption(help = "Output format", defaultValue = "TABLE") NexusCliProperties.OutputFormat format) {

        try {
            Map<String, Object> parameters = NexusCliUtil.parseJsonParams(paramsJson);
            NexusGraphResultSet resultSet = queryProcessor.executeQuery(query, parameters);
            final com.widdo.nexus.core.result.Result<List<Map<String, Value>>> result = Neo4jUtil.originResult(resultSet);
            final NexusResult wrapper = NexusResultInterface.NEO4j.ALL.wrapper(result);
            final List<Map<String, Object>> orDefault = (List<Map<String, Object>>) wrapper.getOrDefault("data", List.class);

            return NexusCliUtil.formatOutput(orDefault, format);
        } catch (Exception ex) {
            return "Query execution failed: " + ex.getMessage();
        }
    }

    @ShellMethod(key = "db-features", value = "Show supported database features")
    public String showFeatures() {
        List<List<String>> rows = new ArrayList<>();

        for (NexusDatabaseFeature feature : NexusDatabaseFeature.values()) {
            boolean supported = adapter.supportsFeature(feature);
            String status = supported ? "✓" : "✗";
            rows.add(Arrays.asList(feature.name(), status));
        }

        return tableRenderer.renderTable(
                Arrays.asList("Feature", "Supported"),
                rows
        );
    }

    @ShellMethod(key = "db-switch", value = "Switch database connection")
    public String switchDatabase(
            @ShellOption(help = "Database type") String databaseType,
            @ShellOption(help = "Connection configuration in JSON") String configJson) {

        // 这里需要实现动态重新配置数据库连接
        // 在实际实现中，这可能需要重启应用或重新初始化bean

        return String.format("Switched to %s database. Note: This may require restarting the CLI.", databaseType);
    }

}