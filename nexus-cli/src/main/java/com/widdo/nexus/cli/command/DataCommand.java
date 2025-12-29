package com.widdo.nexus.cli.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.widdo.nexus.core.support.template.AbstractNexusAdvancedTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DataCommand
 *
 * Nexus Shell 数据管理命令
 *
 * @author XYL
 * @date 2025/08/27 14:31
 * @since 0.0.1-SNAPSHOT
 */
@ShellComponent
@ShellCommandGroup("Data Commands")
public class DataCommand {

    private final AbstractNexusAdvancedTemplate template;
    private final ObjectMapper objectMapper;

    public DataCommand(AbstractNexusAdvancedTemplate template) {
        this.template = template;
        this.objectMapper = new ObjectMapper();
    }

    @ShellMethod(key = "import-json", value = "Import data from JSON file")
    public String importJson(
            @ShellOption(help = "JSON file path") String filePath,
            @ShellOption(help = "Target node label") String label) {

        try {
            List<Map<String, Object>> data = readJsonFile(filePath);
            int count = importData(data, label);

            return String.format("Imported %d records to %s", count, label);
        } catch (Exception ex) {
            return "Import failed: " + ex.getMessage();
        }
    }

    @ShellMethod(key = "export-json", value = "Export data to JSON file")
    public String exportJson(
            @ShellOption(help = "Output file path") String filePath,
            @ShellOption(help = "Cypher query to select data") String query) {

        try {
            List<Map<String, Object>> results = template.executeCypher(
                    query, Collections.emptyMap(), List.class);

            writeJsonFile(filePath, results);
            return String.format("Exported %d records to %s", results.size(), filePath);
        } catch (Exception ex) {
            return "Export failed: " + ex.getMessage();
        }
    }

    @ShellMethod(key = "clear-data", value = "Clear all data (DANGEROUS!)")
//    @ShellOptionAccessControl(roles = {"ADMIN"})
    public String clearData(
            @ShellOption(help = "Confirmation code", defaultValue = "") String confirm) {

        if (!"DELETE_ALL".equals(confirm)) {
            return "Error: This operation requires confirmation. Use --confirm DELETE_ALL";
        }

        try {
            template.executeCypher("MATCH (n) DETACH DELETE n",
                    Collections.emptyMap(), Void.class);
            return "All data cleared successfully";
        } catch (Exception ex) {
            return "Clear operation failed: " + ex.getMessage();
        }
    }

    /**
     * 读取JSON文件并将其解析为Map列表
     *
     * @param filePath JSON文件路径
     * @return 解析后的数据列表
     * @throws IOException 如果文件读取或解析失败
     */
    private List<Map<String, Object>> readJsonFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        return objectMapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * 将数据导入图数据库
     *
     * @param data 要导入的数据列表
     * @param label 目标节点标签
     * @return 成功导入的记录数
     */
    private int importData(List<Map<String, Object>> data, String label) {
        if (data == null || data.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Map<String, Object> record : data) {
            try {
                // 构建Cypher创建语句
                StringBuilder cypher = new StringBuilder("CREATE (n:");
                cypher.append(label).append(" {");

                // 添加属性
                int i = 0;
                for (Map.Entry<String, Object> entry : record.entrySet()) {
                    if (i > 0) {
                        cypher.append(", ");
                    }
                    cypher.append(entry.getKey()).append(": $").append(entry.getKey());
                    i++;
                }
                cypher.append("})");

                // 执行Cypher语句
                template.executeCypher(cypher.toString(), record, Void.class);
                count++;
            } catch (Exception e) {
                System.err.println("Failed to import record: " + record + ", error: " + e.getMessage());
            }
        }

        return count;
    }

    /**
     * 将数据写入JSON文件
     *
     * @param filePath 输出文件路径
     * @param data 要写入的数据
     * @throws IOException 如果文件写入失败
     */
    private void writeJsonFile(String filePath, List<Map<String, Object>> data) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), data);
    }

}