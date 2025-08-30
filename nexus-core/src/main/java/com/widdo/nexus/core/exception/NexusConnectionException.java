package com.widdo.nexus.core.exception;

/**
 * NexusConnectionException.
 * <p>
 * 连接相关异常
 *
 * @author XYL
 * @date 2025/08/27 16:39
 * @since 0.0.1-SNAPSHOT
 */
public class NexusConnectionException extends NexusException {

    public NexusConnectionException(String message) {
        super("CONNECTION_ERROR", message);
    }

    public NexusConnectionException(String message, Throwable cause) {
        super("CONNECTION_ERROR", message, cause);
    }
}
