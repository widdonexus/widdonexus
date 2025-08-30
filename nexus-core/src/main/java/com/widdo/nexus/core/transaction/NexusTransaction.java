package com.widdo.nexus.core.transaction;

/**
 * NexusTransaction
 * <p>
 * 事务接口
 *
 * @author XYL
 * @date 2025/08/27 17:34
 * @since 0.0.1-SNAPSHOT
 */
public interface NexusTransaction extends AutoCloseable {
    void commit();

    void rollback();

    boolean isOpen();

    String txId();
}