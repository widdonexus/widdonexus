package com.widdo.nexus.core.persistence;

import java.util.Set;

/**
 * 增强的持久化策略接口
 * 支持更丰富的操作
 */
public interface NexusPersistenceStrategy {
    void save(String key, Object value);
    Object load(String key);
    void delete(String key);
    boolean exists(String key);
    
    // 新增方法
    Set<String> keys();
    long size();
    void clear();
    default <T> T load(String key, Class<T> type) {
        Object value = load(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
}