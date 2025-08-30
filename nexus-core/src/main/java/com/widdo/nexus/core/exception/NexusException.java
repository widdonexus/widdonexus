package com.widdo.nexus.core.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * NexusException.
 * <p>
 * 异常基类 - 所有框架异常的父类
 *
 * @author XYL
 * @date 2025/08/27 16:27
 * @since 0.0.1-SNAPSHOT
 */
public class NexusException extends RuntimeException {

    private final String errorCode;

    private final Map<String, Object> context;

    public NexusException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    public NexusException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>();
    }

    public NexusException withContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }

    @Override
    public String getMessage() {
        if (context.isEmpty()) {
            return super.getMessage();
        }
        return super.getMessage() + " [Context: " + context + "]";
    }

}
