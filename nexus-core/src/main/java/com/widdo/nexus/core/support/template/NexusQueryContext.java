package com.widdo.nexus.core.support.template;

import com.widdo.nexus.core.util.HdfsCommandExtractor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NexusQueryContext
 * <p>
 * 查询执行上下文
 *
 * @author XYL
 * @date 2025/08/27 15:52
 * @since 0.0.1-SNAPSHOT
 */
public class NexusQueryContext {

    private final String queryId;
    private String processedQueryId;
    private final String executionId;
    private long startTime;
    private long executionTime;
    private int currentOperation;
    private int totalOperations;
    private Map<String, Object> attributes = new HashMap<>();
    private HdfsCommandExtractor.CommandInfo commandInfo;

    public NexusQueryContext(String queryId) {
        this.queryId = queryId;
        this.executionId = UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
    }

    public String getQueryId() {
        return queryId;
    }

    public String getProcessedQueryId() {
        return processedQueryId;
    }

    public void setProcessedQueryId(String processedQueryId) {
        this.processedQueryId = processedQueryId;
    }

    public HdfsCommandExtractor.CommandInfo getCommandInfo() {
        return commandInfo;
    }

    public void setCommandInfo(HdfsCommandExtractor.CommandInfo commandInfo) {
        this.commandInfo = commandInfo;
    }

    public String getExecutionId() {
        return executionId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public int getCurrentOperation() {
        return currentOperation;
    }

    public void setCurrentOperation(int currentOperation) {
        this.currentOperation = currentOperation;
    }

    public int getTotalOperations() {
        return totalOperations;
    }

    public void setTotalOperations(int totalOperations) {
        this.totalOperations = totalOperations;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }
}
