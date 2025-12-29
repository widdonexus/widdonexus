package com.widdo.nexus.cli.command;

import com.widdo.nexus.cli.shell.TableRenderer;
import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.health.NexusHealthIndicator;
import com.widdo.nexus.core.meta.NexusDatabaseMetadata;
import com.widdo.nexus.core.result.entity.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminCommand
 *
 * Nexus Shell 系统管理命令
 *
 * @author XYL
 * @date 2025/08/27 14:28
 * @since 0.0.1-SNAPSHOT
 */
@ShellComponent
@ShellCommandGroup("Admin Commands")
public class AdminCommand {

    private final NexusGraphAdapter adapter;
    private final NexusHealthIndicator healthIndicator;

    private final TableRenderer tableRenderer;

    public AdminCommand(NexusGraphAdapter adapter,
                        NexusHealthIndicator healthIndicator,
                        TableRenderer tableRenderer) {
        this.adapter = adapter;
        this.healthIndicator = healthIndicator;
        this.tableRenderer = tableRenderer;
    }

    @ShellMethod(key = "health", value = "Check database health status")
    public String healthCheck() {
        Health health = healthIndicator.health();

        return tableRenderer.renderTable(
                Arrays.asList("Component", "Status", "Details"),
                Arrays.asList(Arrays.asList(
                        "Database",
                        health.getStatus().toString(),
                        health.getDetails().toString()
                ))
        );
    }

    @ShellMethod(key = "info", value = "Show database information")
    public String databaseInfo() {
        NexusDatabaseMetadata metadata = adapter.getMetadata();

        return tableRenderer.renderTable(
                Arrays.asList("Property", "Value"),
                metadata.getStats().entrySet().stream()
                        .map(entry -> Arrays.asList(entry.getKey(), String.valueOf(entry.getValue())))
                        .collect(Collectors.toList())
        );
    }

    @ShellMethod(key = "index-list", value = "List all indexes")
    public String listIndexes() {
        try {
            List<Map<String, Value>> indexes = adapter.executeQuery(
                    "SHOW INDEXES", Collections.emptyMap()).toList();

            return tableRenderer.renderTable(
                    Arrays.asList("Name", "Type", "Properties", "Status"),
                    indexes.stream()
                            .map(index -> Arrays.asList(
                                    String.valueOf(index.get("name")),
                                    String.valueOf(index.get("type")),
                                    String.valueOf(index.get("properties")),
                                    String.valueOf(index.get("status"))
                            ))
                            .collect(Collectors.toList())
            );
        } catch (Exception ex) {
            return "Failed to list indexes: " + ex.getMessage();
        }
    }
}