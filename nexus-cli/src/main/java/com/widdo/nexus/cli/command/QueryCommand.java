package com.widdo.nexus.cli.command;

import com.widdo.nexus.cli.properties.NexusCliProperties;
import com.widdo.nexus.cli.shell.TableRenderer;
import com.widdo.nexus.cli.util.NexusCliUtil;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.core.support.template.AbstractNexusAdvancedTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.*;
import java.util.stream.Collectors;

/**
 * QueryCommand
 * <p>
 * Nexus Shell 查询命令
 *
 * @author XYL
 * @date 2025/08/27 14:27
 * @since 0.0.1-SNAPSHOT
 */
@ShellComponent
@ShellCommandGroup("Query Commands")
public class QueryCommand {

    private final AbstractNexusAdvancedTemplate template;
    private final NexusQueryLoader queryLoader;
    private final NexusCliProperties properties;
    private final TableRenderer tableRenderer;


    public QueryCommand(AbstractNexusAdvancedTemplate template,
                        NexusQueryLoader queryLoader,
                        NexusCliProperties properties,
                        TableRenderer tableRenderer) {
        this.template = template;
        this.queryLoader = queryLoader;
        this.properties = properties;
        this.tableRenderer = tableRenderer;


    }


    @ShellMethod(key = "query", value = "Execute a Cypher query")
    public String executeQuery(
            @ShellOption(help = "Cypher query to execute") String cypher,
            @ShellOption(help = "Output format", defaultValue = "TABLE") NexusCliProperties.OutputFormat format,
            @ShellOption(help = "Timeout in seconds", defaultValue = "30") int timeout) {

        try {
            List<Map<String, Object>> results = template.executeCypher(
                    cypher, Collections.emptyMap(), List.class);

            return NexusCliUtil.formatOutput(results, format);
        } catch (Exception ex) {
            return "Error: " + ex.getMessage();
        }
    }


    @ShellMethod(key = "query-id", value = "Execute a predefined query by ID")
    public String executeQueryById(
            @ShellOption(help = "Query ID") String id,
            @ShellOption(help = "Query parameters in JSON format", defaultValue = "{}") String paramsJson,
            @ShellOption(help = "Output format", defaultValue = "TABLE") NexusCliProperties.OutputFormat format) {

        try {
            Map<String, Object> params = NexusCliUtil.parseJsonParams(paramsJson);
            List<Map<String, Object>> results = template.execute(id, params, List.class);

            return NexusCliUtil.formatOutput(results, format);
        } catch (Exception ex) {
            return "Error: " + ex.getMessage();
        }
    }

    @ShellMethod(key = "query-list", value = "List all available queries")
    public String listQueries() {
        Set<String> queryIds = queryLoader.getAllQueryIds();

        return tableRenderer.renderTable(
                Arrays.asList("Query ID", "Description"),
                queryIds.stream()
                        .map(id -> Arrays.asList(id, getQueryDescription(id)))
                        .collect(Collectors.toList())
        );
    }

    private String getQueryDescription(String queryId) {
        return queryLoader.getDescription(queryId);
    }


}
