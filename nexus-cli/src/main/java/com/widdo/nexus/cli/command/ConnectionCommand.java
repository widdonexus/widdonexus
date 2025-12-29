package com.widdo.nexus.cli.command;

import com.widdo.nexus.cli.shell.TableRenderer;
import com.widdo.nexus.core.adapter.NexusGraphAdapter;
import com.widdo.nexus.core.enums.NexusDatabaseFeature;
import com.widdo.nexus.core.meta.NexusDatabaseMetadata;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.Arrays;

/**
 * ConnectionCommand
 * <p>
 * Nexus Shell 数据库连接测试命令
 *
 * @author XYL
 * @date 2025/09/01 10:21
 * @since 0.0.1-SNAPSHOT
 */
@ShellComponent
@ShellCommandGroup("Connection Commands")
public class ConnectionCommand {
    
    private final NexusGraphAdapter adapter;

    private final TableRenderer tableRenderer;

    public ConnectionCommand(NexusGraphAdapter adapter,TableRenderer tableRenderer) {
        this.adapter = adapter;
        this.tableRenderer = tableRenderer;
    }
    
    @ShellMethod(key = "test-connection", value = "Test database connection")
    public String testConnection() {
        try {
            long startTime = System.currentTimeMillis();
            Health health = adapter.healthCheck();
            long duration = System.currentTimeMillis() - startTime;
            
            if (health.getStatus() == Status.UP) {
                return String.format("✓ Connection successful (%d ms)\n%s", 
                    duration, health.getDetails());
            } else {
                return String.format("✗ Connection failed: %s", health.getDetails());
            }
        } catch (Exception ex) {
            return "Connection test failed: " + ex.getMessage();
        }
    }
    
    @ShellMethod(key = "connection-status", value = "Show current connection status")
    public String connectionStatus() {
        NexusDatabaseMetadata metadata = adapter.getMetadata();
        Health health = adapter.healthCheck();
        
        return tableRenderer.renderTable(
            Arrays.asList("Property", "Value"),
            Arrays.asList(
                Arrays.asList("Database Type", metadata.databaseType()),
                Arrays.asList("Version", metadata.getVersion()),
                Arrays.asList("Status", health.getStatus().toString()),
                Arrays.asList("Supports Cypher", 
                    String.valueOf(adapter.supportsFeature(NexusDatabaseFeature.NATIVE_CYPHER_SUPPORT))),
                Arrays.asList("Supports Transactions", 
                    String.valueOf(adapter.supportsFeature(NexusDatabaseFeature.ACID_TRANSACTIONS)))
            )
        );
    }
}