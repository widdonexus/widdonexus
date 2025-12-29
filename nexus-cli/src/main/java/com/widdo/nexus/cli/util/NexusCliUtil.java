package com.widdo.nexus.cli.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.widdo.nexus.cli.properties.NexusCliProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NexusCliUtil.
 *
 * @author XYL
 * @date 2025/09/01 10:02
 * @since 0.0.1-SNAPSHOT
 */
public class NexusCliUtil {

    private static final ObjectMapper jsonMapper;
    private static final CsvMapper csvMapper;
    private static final ObjectMapper yamlMapper;

    static {
        jsonMapper = new ObjectMapper();
        csvMapper = new CsvMapper();
        yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * 解析JSON参数字符串为Map
     */
    public static Map<String, Object> parseJsonParams(String paramsJson) {
        try {
            if (paramsJson == null || paramsJson.trim().isEmpty() || "{}".equals(paramsJson)) {
                return Collections.emptyMap();
            }
            return jsonMapper.readValue(paramsJson, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON parameters: " + e.getMessage());
        }
    }


    public static String formatOutput(List<Map<String, Object>> results,
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
    private static String toJson(List<Map<String, Object>> results) {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
        } catch (Exception e) {
            return "Error converting to JSON: " + e.getMessage();
        }
    }

    /**
     * 将结果转换为CSV格式
     */
    private static String toCsv(List<Map<String, Object>> results) {
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
    private static String toYaml(List<Map<String, Object>> results) {
        try {
            return yamlMapper.writeValueAsString(results);
        } catch (Exception e) {
            return "Error converting to YAML: " + e.getMessage();
        }
    }

    private static String toTable(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "No results found";
        }

        Set<String> columns = results.get(0).keySet();
        List<List<String>> rows = results.stream()
                .map(row -> columns.stream()
                        .map(col -> String.valueOf(row.get(col)))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

//        return tableRenderer.renderTable(
//                new ArrayList<>(columns),
//                rows );
        return "";

    }
}
