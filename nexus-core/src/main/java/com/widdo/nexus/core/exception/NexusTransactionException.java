package com.widdo.nexus.core.exception;

/**
 * NexusQueryException.
 * <p>
 * 事务相关异常
 *
 * @author XYL
 * @date 2025/08/27 16:35
 * @since 0.0.1-SNAPSHOT
 */
public class NexusTransactionException extends NexusException {
    public NexusTransactionException(String message) {
        super("TRANSACTION_ERROR", message);
    }

    public NexusTransactionException(String message, Throwable cause) {
        super("TRANSACTION_ERROR", message, cause);
    }
}