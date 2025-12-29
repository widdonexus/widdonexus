package com.widdo.nexus.core.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HdfsCommandExtractor {

    // 支持的HDFS命令前缀
    private static final List<String[]> COMMAND_PREFIXES = Arrays.asList(
            new String[]{"hdfs", "dfs"},
            new String[]{"hadoop", "fs"},
            new String[]{"hdfs"},
            new String[]{"hadoop"}
    );

    // 已知的HDFS命令集合
    private static final Set<String> HDFS_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            // 文件操作
            "ls", "cat", "text", "tail", "head", "get", "copyToLocal",
            "stat", "test", "du", "df", "count", "find",

            // 目录操作
            "mkdir", "rmdir", "lsr", "dus",

            // 上传/复制/移动
            "put", "copyFromLocal", "cp", "mv", "appendToFile",

            // 删除
            "rm", "expunge",

            // 权限管理
            "chmod", "chown", "chgrp", "getfacl", "setfacl",

            // 快照
            "snapshotDiff", "allowSnapshot", "disallowSnapshot", "createSnapshot", "deleteSnapshot",

            // 其他
            "setrep", "truncate", "concat", "usage", "help"
    )));

    // 命令类型分类
    private static final Map<String, String> COMMAND_CATEGORIES = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("ls", "READ");
        put("cat", "READ");
        put("text", "READ");
        put("tail", "READ");
        put("head", "READ");
        put("get", "READ");
        put("copyToLocal", "READ");
        put("stat", "READ");
        put("test", "READ");
        put("du", "READ");
        put("df", "READ");
        put("count", "READ");
        put("find", "READ");
        put("lsr", "READ");
        put("dus", "READ");

        put("mkdir", "WRITE");
        put("rmdir", "WRITE");
        put("put", "WRITE");
        put("copyFromLocal", "WRITE");
        put("cp", "WRITE");
        put("mv", "WRITE");
        put("appendToFile", "WRITE");
        put("rm", "WRITE");
        put("expunge", "WRITE");

        put("chmod", "PERMISSION");
        put("chown", "PERMISSION");
        put("chgrp", "PERMISSION");
        put("getfacl", "PERMISSION");
        put("setfacl", "PERMISSION");

        put("snapshotDiff", "SNAPSHOT");
        put("allowSnapshot", "SNAPSHOT");
        put("disallowSnapshot", "SNAPSHOT");
        put("createSnapshot", "SNAPSHOT");
        put("deleteSnapshot", "SNAPSHOT");

        put("setrep", "ADMIN");
        put("truncate", "ADMIN");
        put("concat", "ADMIN");
        put("usage", "HELP");
        put("help", "HELP");
    }});

    // 需要递归选项的命令
    private static final Set<String> RECURSIVE_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "ls", "rm", "chmod", "chown", "chgrp", "du", "count"
    )));

    /**
     * 解析HDFS命令
     */
    public static CommandInfo parseCommand(String rawCommand) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) {
            return CommandInfo.unknown(rawCommand);
        }

        rawCommand = rawCommand.trim();

        // 1. 提取命令前缀
        CommandPrefixInfo prefixInfo = extractCommandPrefix(rawCommand);
        if (!prefixInfo.hasPrefix) {
            return CommandInfo.unknown(rawCommand);
        }

        // 2. 提取主命令和选项
        CommandExtractionResult extraction = extractMainCommand(rawCommand, prefixInfo.commandStartIndex);

        // 3. 构建命令信息
        return buildCommandInfo(rawCommand, prefixInfo, extraction);
    }

    /**
     * 提取命令前缀信息
     */
    private static CommandPrefixInfo extractCommandPrefix(String command) {
        String[] parts = command.split("\\s+");

        for (String[] prefix : COMMAND_PREFIXES) {
            if (matchesPrefix(parts, prefix)) {
                return new CommandPrefixInfo(
                        true,
                        prefix[0] + (prefix.length > 1 ? " " + prefix[1] : ""),
                        prefix.length
                );
            }
        }

        return CommandPrefixInfo.NO_PREFIX;
    }

    private static boolean matchesPrefix(String[] parts, String[] prefix) {
        if (parts.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (!prefix[i].equals(parts[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 提取主命令和选项
     */
    private static CommandExtractionResult extractMainCommand(String command, int startIndex) {
        String[] parts = command.split("\\s+");
        List<String> options = new ArrayList<>();
        String mainCommand = null;

        for (int i = startIndex; i < parts.length; i++) {
            String part = parts[i];

            if (part.startsWith("-")) {
                String potentialCommand = part.substring(1);

                // 检查是否是主命令（不是选项）
                if (mainCommand == null && isHdfsCommand(potentialCommand)) {
                    mainCommand = potentialCommand;
                } else {
                    // 收集选项
                    options.add(part);
                }
            } else if (mainCommand != null) {
                // 已经找到主命令，后面的非-开头的部分是参数
                break;
            }
        }

        // 如果没有找到命令，检查是否有简写形式
        if (mainCommand == null) {
            for (int i = startIndex; i < parts.length; i++) {
                if (parts[i].matches("^[a-zA-Z]{2,}$") && isHdfsCommand(parts[i])) {
                    mainCommand = parts[i];
                    break;
                }
            }
        }

        return new CommandExtractionResult(mainCommand, options);
    }

    private static boolean isHdfsCommand(String command) {
        return HDFS_COMMANDS.contains(command.toLowerCase());
    }

    /**
     * 构建完整的命令信息
     */
    private static CommandInfo buildCommandInfo(String rawCommand,
                                                CommandPrefixInfo prefixInfo,
                                                CommandExtractionResult extraction) {
        CommandInfo info = new CommandInfo();
        info.rawCommand = rawCommand;
        info.prefix = prefixInfo.prefix;

        if (extraction.mainCommand != null) {
            info.command = extraction.mainCommand.toLowerCase();
            info.category = COMMAND_CATEGORIES.getOrDefault(info.command, "UNKNOWN");
            info.isReadOperation = isReadOperation(info.command);
            info.isWriteOperation = isWriteOperation(info.command);
            info.requiresRecursiveOption = RECURSIVE_COMMANDS.contains(info.command);
            info.options = extraction.options;
            info.hasRecursiveOption = hasRecursiveOption(extraction.options);
            info.hasForceOption = hasForceOption(extraction.options);
        } else {
            info.command = "unknown";
            info.category = "UNKNOWN";
        }

        return info;
    }

    private static boolean isReadOperation(String command) {
        return "READ".equals(COMMAND_CATEGORIES.get(command));
    }

    private static boolean isWriteOperation(String command) {
        String category = COMMAND_CATEGORIES.get(command);
        return "WRITE".equals(category) || "PERMISSION".equals(category) ||
                "SNAPSHOT".equals(category) || "ADMIN".equals(category);
    }

    private static boolean hasRecursiveOption(List<String> options) {
        return options.stream().anyMatch(opt ->
                opt.equals("-R") || opt.equals("-r") || opt.equals("-recursive"));
    }

    private static boolean hasForceOption(List<String> options) {
        return options.stream().anyMatch(opt -> opt.equals("-f") || opt.equals("-force"));
    }

    /**
     * 命令前缀信息
     */
    private static class CommandPrefixInfo {
        static final CommandPrefixInfo NO_PREFIX = new CommandPrefixInfo(false, "", 0);

        final boolean hasPrefix;
        final String prefix;
        final int commandStartIndex; // 命令部分开始的索引

        CommandPrefixInfo(boolean hasPrefix, String prefix, int prefixWordCount) {
            this.hasPrefix = hasPrefix;
            this.prefix = prefix;
            this.commandStartIndex = prefixWordCount;
        }
    }

    /**
     * 命令提取结果
     */
    private static class CommandExtractionResult {
        final String mainCommand;
        final List<String> options;

        CommandExtractionResult(String mainCommand, List<String> options) {
            this.mainCommand = mainCommand;
            this.options = options != null ? options : Collections.emptyList();
        }
    }

    /**
     * 命令信息类
     */
    public static class CommandInfo {
        private String rawCommand;
        private String prefix;
        private String command;
        private String processedCommand;
        private String category;
        private boolean isReadOperation;
        private boolean isWriteOperation;
        private boolean requiresRecursiveOption;
        private boolean hasRecursiveOption;
        private boolean hasForceOption;
        private List<String> options;

        // 静态工厂方法
        public static CommandInfo unknown(String rawCommand) {
            CommandInfo info = new CommandInfo();
            info.rawCommand = rawCommand;
            info.command = "unknown";
            info.category = "UNKNOWN";
            info.options = Collections.emptyList();
            return info;
        }

        // Getters
        public String getRawCommand() {
            return rawCommand;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getCommand() {
            return command;
        }

        public String getCategory() {
            return category;
        }

        public boolean isReadOperation() {
            return isReadOperation;
        }

        public boolean isWriteOperation() {
            return isWriteOperation;
        }

        public boolean requiresRecursiveOption() {
            return requiresRecursiveOption;
        }

        public boolean hasRecursiveOption() {
            return hasRecursiveOption;
        }

        public boolean hasForceOption() {
            return hasForceOption;
        }

        public List<String> getOptions() {
            return options;
        }

        public boolean isValid() {
            return !"unknown".equals(command);
        }

        public boolean isRecursive() {
            return requiresRecursiveOption && hasRecursiveOption;
        }

        public String getProcessedCommand() {
            return processedCommand;
        }

        public void setProcessedCommand(String processedCommand) {
            this.processedCommand = processedCommand;
        }

        @Override
        public String toString() {
            return String.format(
                    "CommandInfo{command='%s', category='%s', read=%s, write=%s, recursive=%s, options=%s}",
                    command, category, isReadOperation, isWriteOperation,
                    hasRecursiveOption, options
            );
        }

        public String toSimpleString() {
            if (!isValid()) {
                return "Unknown command";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("HDFS ").append(command.toUpperCase());

            if (hasRecursiveOption) sb.append(" (recursive)");
            if (hasForceOption) sb.append(" (force)");

            sb.append(" [").append(category).append("]");
            return sb.toString();
        }
    }

    /**
     * 增强的解析方法：支持参数替换
     */
    public static CommandInfo parseCommandWithParameters(String commandTemplate,
                                                         Map<String, Object> parameters) {
        // 1. 替换参数
        String processedCommand = replaceParameters(commandTemplate, parameters);

        // 2. 解析命令
        CommandInfo info = parseCommand(processedCommand);

        // 3. 添加参数信息
        info.rawCommand = commandTemplate; // 保留原始模板
        info.processedCommand = processedCommand;
//        info.parameters = extractParameters(commandTemplate);

        return info;
    }

    /**
     * 替换命令中的参数占位符
     */
    public static String replaceParameters(String commandTemplate, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return commandTemplate;
        }

        //命令参数
        final Map<String, Object> params = (Map<String, Object>) parameters.getOrDefault("params", Map.of());

        if (params == null || params.isEmpty()) {
            return commandTemplate;
        }

        String result = commandTemplate;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            if (paramValue != null) {
                // 替换 $paramName 或 ${paramName} 形式的占位符
                result = result.replaceAll("\\$" + Pattern.quote(paramName),
                        Matcher.quoteReplacement(paramValue.toString()));
                result = result.replaceAll("\\$\\{" + Pattern.quote(paramName) + "\\}",
                        Matcher.quoteReplacement(paramValue.toString()));
            }
        }

        return result;
    }

    /**
     * 从命令模板中提取参数名
     */
    private static List<String> extractParameters(String commandTemplate) {
        List<String> params = new ArrayList<>();

        // 匹配 $param 或 ${param} 形式的参数
        Pattern pattern = Pattern.compile("\\$(?:\\{([^}]+)\\}|([a-zA-Z_][a-zA-Z0-9_]*))");
        Matcher matcher = pattern.matcher(commandTemplate);

        while (matcher.find()) {
            String param = matcher.group(1);
            if (param == null) {
                param = matcher.group(2);
            }
            params.add(param);
        }

        return params;
    }

    // 测试方法
    public static void main(String[] args) {
        // 测试各种HDFS命令格式
        String[] testCommands = {
                "hdfs dfs -ls $target $source",
                "hadoop fs -mkdir -p $path",
                "hdfs dfs -put local.txt $hdfsPath",
                "hadoop fs -get $hdfsFile local.txt",
                "hdfs dfs -cp $src $dst",
                "hadoop fs -rm -r -f $path",
                "hdfs dfs -chmod -R 755 $file",
                "hdfs -ls -R /user",
                "hadoop fs -du -h $directory",
                "hdfs dfs -find /user -name \"*.txt\"",
                "hdfs dfs -setrep 3 $file",
                "invalid command",
                "",
                null
        };

        // 参数映射
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("target", "/user/hadoop");
        parameters.put("source", "/data");
        parameters.put("path", "/tmp/newdir");
        parameters.put("hdfsPath", "/user/hadoop/uploads");
        parameters.put("hdfsFile", "/user/hadoop/data.txt");
        parameters.put("src", "/source/path");
        parameters.put("dst", "/dest/path");
        parameters.put("file", "/user/hadoop/test.txt");
        parameters.put("directory", "/user");

        System.out.println("=== 测试基本命令解析 ===");
        for (String cmd : testCommands) {
            CommandInfo info = parseCommand(cmd);
            System.out.printf("%-50s -> %s%n",
                    cmd != null ? cmd.substring(0, Math.min(cmd.length(), 45)) + "..." : "null",
                    info.isValid() ? info.toSimpleString() : "Invalid command");
        }

        System.out.println("\n=== 测试带参数的命令解析 ===");
        String template = "hdfs dfs -ls $target $source";
        CommandInfo infoWithParams = parseCommandWithParameters(template, parameters);
        System.out.println("Template: " + template);
        System.out.println("Processed: " + infoWithParams.processedCommand);
        System.out.println("Info: " + infoWithParams);
//        System.out.println("Parameters: " + infoWithParams.parameters);

        System.out.println("\n=== 测试命令分类 ===");
        String[] categorizedCommands = {
                "hdfs dfs -ls -R /path",          // READ
                "hadoop fs -mkdir /newdir",       // WRITE
                "hdfs dfs -chmod 755 file.txt",   // PERMISSION
                "hadoop fs -help",                // HELP
                "hdfs dfs -setrep 3 file.txt"     // ADMIN
        };

        for (String cmd : categorizedCommands) {
            CommandInfo info = parseCommand(cmd);
            System.out.printf("%-40s -> %s (category: %s)%n",
                    cmd, info.getCommand(), info.getCategory());
        }
    }
}