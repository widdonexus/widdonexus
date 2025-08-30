package com.widdo.nexus.cli.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.widdo.nexus.cli.properties.NexusCliProperties;
import com.widdo.nexus.cli.shell.TableRenderer;
import com.widdo.nexus.core.query.NexusQueryLoader;
import com.widdo.nexus.core.support.template.NexusAdvancedTemplate;
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

    private final NexusAdvancedTemplate template;
    private final NexusQueryLoader queryLoader;
    private final NexusCliProperties properties;
    private final TableRenderer tableRenderer;

    private final ObjectMapper jsonMapper;
    private final CsvMapper csvMapper;
    private final ObjectMapper yamlMapper;

    public QueryCommand(NexusAdvancedTemplate template,
                        NexusQueryLoader queryLoader,
                        NexusCliProperties properties,
                        TableRenderer tableRenderer) {
        this.template = template;
        this.queryLoader = queryLoader;
        this.properties = properties;
        this.tableRenderer = tableRenderer;

        this.jsonMapper = new ObjectMapper();
        this.csvMapper = new CsvMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }


    @ShellMethod(key = "query", value = "Execute a Cypher query")
    public String executeQuery(
            @ShellOption(help = "Cypher query to execute") String cypher,
            @ShellOption(help = "Output format", defaultValue = "TABLE") NexusCliProperties.OutputFormat format,
            @ShellOption(help = "Timeout in seconds", defaultValue = "30") int timeout) {

        try {
            List<Map<String, Object>> results = template.executeCypher(
                    cypher, Collections.emptyMap(), List.class);

            return formatOutput(results, format);
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
            Map<String, Object> params = parseJsonParams(paramsJson);
            List<Map<String, Object>> results = template.execute(id, params, List.class);

            return formatOutput(results, format);
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

    /**
     * 解析JSON参数字符串为Map
     */
    private Map<String, Object> parseJsonParams(String paramsJson) {
        try {
            if (paramsJson == null || paramsJson.trim().isEmpty() || "{}".equals(paramsJson)) {
                return Collections.emptyMap();
            }
            return jsonMapper.readValue(paramsJson, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON parameters: " + e.getMessage());
        }
    }


    private String formatOutput(List<Map<String, Object>> results,
                                NexusCliProperties.OutputFormat format) {
        switch (format) {
            case JSON:
                return toJson(results);
            case CSV:
                return toCsv(results);
            case YAML:
                return toYaml(results);
            default:
                return toTable(results);
        }
    }

    /**
     * 将结果转换为JSON格式
     */
    private String toJson(List<Map<String, Object>> results) {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
        } catch (Exception e) {
            return "Error converting to JSON: " + e.getMessage();
        }
    }

    /**
     * 将结果转换为CSV格式
     */
    private String toCsv(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "";
        }

        try {
            // 获取所有列名
            Set<String> columns = results.get(0).keySet();

            // 创建CSV schema
            CsvSchema.Builder schemaBuilder = CsvSchema.builder();
            for (String column : columns) {
                schemaBuilder.addColumn(column);
            }
            CsvSchema schema = schemaBuilder.build().withHeader();

            // 转换为CSV
            return csvMapper.writer(schema).writeValueAsString(results);
        } catch (Exception e) {
            return "Error converting to CSV: " + e.getMessage();
        }
    }

    /**
     * 将结果转换为YAML格式
     */
    private String toYaml(List<Map<String, Object>> results) {
        try {
            return yamlMapper.writeValueAsString(results);
        } catch (Exception e) {
            return "Error converting to YAML: " + e.getMessage();
        }
    }

    private String toTable(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "No results found";
        }

        Set<String> columns = results.get(0).keySet();
        List<List<String>> rows = results.stream()
                .map(row -> columns.stream()
                        .map(col -> String.valueOf(row.get(col)))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        return tableRenderer.renderTable(
                new ArrayList<>(columns),
                rows
        );
    }
}
