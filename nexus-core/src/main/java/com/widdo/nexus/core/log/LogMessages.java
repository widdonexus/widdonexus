package com.widdo.nexus.core.log;

import java.text.MessageFormat;

/**
 * LogMessages
 * <p>
 * 统一日志消息模板
 * <p>
 * 确保所有日志格式一致且可搜索
 *
 * @author XYL
 * @date 2025/08/27 15:34
 * @since 0.0.1-SNAPSHOT
 */
public class LogMessages {

    // 启动相关
    public static final String STARTUP_BANNER = "\n" +
            "=========================================\n" +
            "    Widdo Nexus Framework Starting...\n" +
            "    Version: {0}\n" +
            "=========================================";
    public static final String DATABASE_ADAPTER_REGISTER = "Registered Nexus adapter factory: {0}";
    public static final String DATABASE_ADAPTER_INIT = "Initializing Nexus database adapter for: {0}";
    public static final String QUERY_LOADER_START = "Loading queries from {0}";
    // 运行时相关
    public static final String QUERY_EXECUTING = "Executing query: {0}\n";
    public static final String QUERY_PARAMETERS = "Query parameters: {0}";
    public static final String QUERY_DURATION = "Query executed in {0} ms: {1}";
    public static final String TRANSACTION_START = "Transaction started: {0}";
    public static final String TRANSACTION_COMMIT = "Transaction committed: {0}";
    public static final String TRANSACTION_ROLLBACK = "Transaction rolled back: {0}";
    public static final String TRANSACTION_CLOSE = "Transaction closed: {0}";
    public static final String HDFS_COMMAND_EXECUTING = "HDFS Command executed: {0} (type: {1}, duration: {2}ms, read: {3}, write: {4}";
    // 警告相关
    public static final String SLOW_QUERY_WARN = "Slow query detected ({0} ms): {1}";
    public static final String DEPRECATED_API_WARN = "Deprecated API used: {0}";
    // 错误相关
    public static final String DATABASE_CONNECTION_ERROR = "Database connection failed: {0}";
    public static final String QUERY_EXECUTION_ERROR = "Query execution failed: {0}";
    public static final String QUERY_EXECUTION_ERROR_DURATION = "Query failed after {0} ms: {1}";
    public static final String MAPPING_ERROR = "Result mapping failed: {0}";
    private LogMessages() {
    }

    // 格式化方法
    public static String format(String template, Object... args) {
        return MessageFormat.format(template, args);
    }
}
